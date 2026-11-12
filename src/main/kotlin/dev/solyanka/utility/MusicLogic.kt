/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*
import java.net.http.HttpTimeoutException
import java.net.http.WebSocketHandshakeException
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.math.pow

data class MusicSettings(
  val hud: HudSpec = HudSpec(x = 12, y = 245),
  val suggest: Boolean = true,
  val announce: Boolean = false,
  val serviceLabel: String = "YANDEX MUSIC",
  val playingLabel: String = "PLAYING",
  val serviceColor: Int = 0xffff6973.toInt(),
  val playingColor: Int = 0xff97a6b9.toInt(),
  val progressColor: Int = 0xffff6675.toInt(),
  val progressEffect: Int = 0,
  val effectSpeed: Double = 1.0,
  val showLyrics: Boolean = false,
) {
  fun validate() {
    hud.validate()
    require(serviceLabel.length <= 40 && playingLabel.length <= 40)
    require(progressEffect in 0..2)
    require(effectSpeed.isFinite() && effectSpeed in 0.2..3.0)
  }
}

data class LocalPlayback(
  val title: String,
  val artist: String,
  val position: Double,
  val duration: Double,
  val playing: Boolean,
  val art: String,
  val receivedNanos: Long,
)

data class LyricLine(val seconds: Double, val text: String)

data class CurrentTrack(val id: String, val title: String, val artist: String, val duration: Double)

open class MusicServiceFailure(val reason: String) : RuntimeException()

class MusicServerFailure(
  val code: Int?,
  val retryDelays: List<Long>,
  val goAway: Boolean,
  val httpCode: Int? = null,
  val serviceCode: String? = null,
  val goAwaySeconds: Long? = null,
) :
  MusicServiceFailure(
    (if (code != null && code in 0..999) "server error $code" else "server error") +
      (httpCode?.let { " / HTTP $it" } ?: "") +
      (serviceCode?.let { " / $it" } ?: ""),
  )

object MusicLogic {
  fun authorizationCodeValid(value: String): Boolean = value.matches(Regex("[0-9]{7}"))

  fun pkceChallenge(verifier: String): String {
    require(verifier.matches(Regex("[A-Za-z0-9._~-]{43,128}")))
    val digest =
      MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  fun failure(stage: String, error: Throwable): String {
    var cause = error
    while (
      (cause is ExecutionException || cause is CompletionException) && cause.cause != null
    ) cause = cause.cause!!
    val reason =
      when (cause) {
        is MusicServiceFailure -> cause.reason
        is WebSocketHandshakeException -> "HTTP ${cause.response.statusCode()}"
        is HttpTimeoutException,
        is TimeoutException -> "timeout"
        is java.net.UnknownHostException -> "DNS error"
        is javax.net.ssl.SSLException -> "TLS error"
        is java.net.ConnectException -> "network error"
        else -> "unexpected response"
      }
    return "$stage: $reason"
  }

  fun serverFailure(error: JsonElement?): MusicServerFailure {
    val obj = error?.takeIf { it.isJsonObject }?.asJsonObject
    val code = runCatching { obj?.get("grpc_code")?.asInt }.getOrNull()
    val headers =
      listOfNotNull(obj?.get("details"), obj?.get("extra_headers"))
        .filter { it.isJsonObject }
        .map { it.asJsonObject }
    val delayField = headers.firstNotNullOfOrNull { it.get("ynison-backoff-millis") }
    val raw = runCatching { delayField?.asString }.getOrNull()
    val parts = raw?.takeIf { it.length <= 256 }?.split(':')
    val delays = parts?.mapNotNull { it.toLongOrNull()?.takeIf { n -> n >= 0 } }.orEmpty()
    val invalidDelay =
      delayField != null && (parts == null || delays.size != parts.size || delays.isEmpty())
    val httpCode = runCatching {
      obj?.get("http_code")?.asInt
    }
      .getOrNull()
      ?.takeIf { it in 100..599 }
    val serviceCode =
      headers
        .firstNotNullOfOrNull { runCatching { it.get("ynison-error-code")?.asString }.getOrNull() }
        ?.takeIf { it.matches(Regex("[0-9]{1,12}")) }
    val waitField = headers.firstNotNullOfOrNull { it.get("ynison-go-away-for-seconds") }
    // shortcut: waits over seven days remain manual, upgrade if the service documents longer
    // intervals.
    val wait = runCatching {
      waitField?.asString?.toLongOrNull()
    }
      .getOrNull()
      ?.takeIf { it in 1..604800 }
    return MusicServerFailure(
      code,
      delays,
      invalidDelay || waitField != null,
      httpCode,
      serviceCode,
      wait,
    )
  }

  fun retryDelay(error: MusicServerFailure, attempt: Int): Long? {
    if (error.code != 14 || error.goAway || attempt !in 0..3) return null
    val delays = error.retryDelays.ifEmpty { listOf(1000L, 5000L, 30000L) }
    val delay = delays[attempt.coerceAtMost(delays.lastIndex)].coerceAtLeast(1000)
    return delay.takeIf { it <= 60000 }
  }

  fun localPlayback(json: JsonObject, now: Long): LocalPlayback? {
    fun text(key: String) = runCatching {
      json.get(key)?.asString
    }
      .getOrNull()
      .orEmpty()
      .filterNot { it.isISOControl() || it == '§' }
      .trim()
      .take(300)
    fun number(key: String) =
      runCatching { json.get(key)?.asDouble }
        .getOrNull()
        ?.takeIf { it.isFinite() && it >= 0 && it <= 86400 } ?: 0.0
    val title = text("title")
    if (title.isEmpty()) return null
    val duration = number("duration")
    val position = number("position").coerceAtMost(duration)
    val art =
      runCatching { json.get("art")?.asString }
        .getOrNull()
        .orEmpty()
        .takeIf { it.length <= 1_400_000 } ?: ""
    val playing = runCatching { json.get("playing")?.asBoolean }.getOrDefault(false) ?: false
    return LocalPlayback(title, text("artist"), position, duration, playing, art, now)
  }

  fun elapsed(s: LocalPlayback, now: Long): Double =
    (s.position +
        if (s.playing)
          ((now - s.receivedNanos).coerceAtLeast(0) / 1_000_000_000.0).coerceAtMost(5.0)
        else 0.0)
      .coerceIn(0.0, s.duration)

  fun timestamp(seconds: Double): String {
    val n = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(n / 60, n % 60)
  }

  // Supports regular LRC timestamps as well as the optional hour component used
  // by some LRCLIB entries: [mm:ss.xx] and [hh:mm:ss.xxx].
  private val lyricTime = Regex("\\[(?:(\\d{1,2}):)?(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

  fun lyrics(value: String): List<LyricLine> {
    if (value.length > 200_000) return emptyList()
    return value
      .lineSequence()
      .take(3000)
      .flatMap { rawLine ->
        val line = rawLine.removePrefix("\uFEFF")
        val times = lyricTime.findAll(line).toList()
        val words =
          lyricTime.replace(line, "").trim().filterNot { it.isISOControl() || it == '§' }.take(300)
        times.asSequence().mapNotNull { m ->
          val hours = m.groupValues[1].takeIf(String::isNotEmpty)?.toInt() ?: 0
          val minutes = m.groupValues[2].toInt()
          val sec = m.groupValues[3].toInt()
          if (sec >= 60 || (m.groupValues[1].isNotEmpty() && minutes >= 60)) return@mapNotNull null
          val fraction =
            m.groupValues[4].let { digits ->
              if (digits.isEmpty()) 0.0 else digits.toDouble() / 10.0.pow(digits.length)
            }
          LyricLine(hours * 3600.0 + minutes * 60.0 + sec + fraction, words)
        }
      }
      .sortedBy { it.seconds }
      .toList()
  }

  fun lyricAt(lines: List<LyricLine>, seconds: Double): String? =
    lines.lastOrNull { it.seconds <= seconds }?.text?.takeIf { it.isNotBlank() }

  fun progressColor(color: Int, effect: Int, speed: Double, time: Double): Int {
    val alpha = (color ushr 24) and 255
    if (effect == 1) {
      val factor = .55 + .45 * (.5 + .5 * kotlin.math.sin(time * speed * 2.0))
      return ((alpha * factor).toInt().coerceIn(0, 255) shl 24) or (color and 0xffffff)
    }
    if (effect == 2) {
      val hue = ((time * speed * .13) % 1.0).toFloat()
      return (alpha shl 24) or (java.awt.Color.HSBtoRGB(hue, .8f, 1f) and 0xffffff)
    }
    return color
  }

  fun localTrack(title: String?, artist: String?): String? {
    fun safe(value: String?) =
      value.orEmpty().filterNot { it.isISOControl() || it == '§' }.trim().take(300)
    val name = safe(title).takeIf { it.isNotBlank() } ?: return null
    val by = safe(artist)
    return if (by.isEmpty()) name else "$by — $name"
  }

  private fun snake(key: String) = key.replace(Regex("[A-Z]")) { "_" + it.value.lowercase() }

  private fun JsonObject.field(key: String): JsonElement? = get(key) ?: get(snake(key))

  private fun JsonObject.objectAt(key: String): JsonObject? =
    field(key)?.takeIf { it.isJsonObject }?.asJsonObject

  private fun JsonObject.text(key: String): String? =
    field(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

  private fun JsonObject.arrayAt(key: String): JsonArray? =
    field(key)?.takeIf { it.isJsonArray }?.asJsonArray

  private fun JsonObject.numberAt(key: String): Double? = runCatching {
    field(key)?.asDouble
  }
    .getOrNull()
    ?.takeIf { it.isFinite() && it >= 0.0 }

  private fun JsonObject.artistNames(): String {
    val names =
      arrayAt("artists")
        ?.asSequence()
        ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject?.text("name") }
        ?.filter(String::isNotBlank)
        ?.toList()
        .orEmpty()
    return names.joinToString(", ").takeIf(String::isNotBlank) ?: text("artist").orEmpty().trim()
  }

  fun redirectValue(response: JsonObject, key: String): String {
    val value = response.field(key)?.takeIf { it.isJsonPrimitive }?.asString
    if (value.isNullOrBlank() || value.length > 4096 || value.any { it.isISOControl() })
      throw MusicServiceFailure("missing or invalid $key")
    return value
  }

  fun queueSize(state: JsonObject): Int =
    state.objectAt("playerState")?.objectAt("playerQueue")?.arrayAt("playableList")?.size()
      ?: error("Missing queue tracks")

  fun currentTrack(state: JsonObject): CurrentTrack? {
    if (state.text("activeDeviceIdOptional").isNullOrBlank()) return null
    val ps = state.objectAt("playerState") ?: return null
    val queue = ps.objectAt("playerQueue") ?: return null
    val list = queue.arrayAt("playableList") ?: return null
    val index = queue.field("currentPlayableIndex")?.asInt ?: 0
    if (index !in 0 until list.size()) return null
    val track = list[index].takeIf { it.isJsonObject }?.asJsonObject ?: return null
    if (track.text("playableType")?.let { it != "TRACK" } == true) return null
    val id = track.text("playableId")?.takeIf { it.matches(Regex("[0-9]{1,20}")) } ?: return null
    val duration = track.numberAt("durationMs")?.div(1000.0) ?: track.numberAt("duration") ?: 0.0
    return CurrentTrack(
      id,
      track.text("title").orEmpty(),
      track.artistNames(),
      duration.coerceAtMost(86400.0),
    )
  }

  fun current(state: JsonObject): Pair<String, String>? =
    currentTrack(state)?.let { it.id to it.title }

  /** Finds the public avatar URI in both track-level and album-level Yandex payloads. */
  fun coverUri(metadata: JsonObject): String? {
    fun value(element: JsonElement?): String? =
      element
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
        ?.trim()
        ?.takeIf(String::isNotBlank)
    val keys = listOf("coverUri", "cover_uri", "coverWithoutText", "cover_without_text")
    for (key in keys) value(metadata.field(key))?.let {
      return it
    }
    metadata.arrayAt("albums")?.asSequence()?.forEach { album ->
      if (album.isJsonObject)
        for (key in keys) value(album.asJsonObject.field(key))?.let {
          return it
        }
    }
    return null
  }

  /**
   * Experimental: retain the complete observed state and append only to an ordinary, non-shuffled
   * queue.
   */
  fun appendRequest(
    state: JsonObject,
    id: String,
    deviceId: String,
    rid: String,
    now: Long,
  ): JsonObject {
    require(id.matches(Regex("[0-9]{1,20}")))
    require(!state.text("activeDeviceIdOptional").isNullOrBlank()) { "No active player" }
    val player = state.objectAt("playerState")?.deepCopy() ?: error("Missing player state")
    val queue = player.objectAt("playerQueue") ?: error("Missing player queue")
    require(queue.field("shuffleOptional")?.isJsonNull != false) {
      "Shuffle is not supported for experimental queue requests"
    }
    val structure = queue.objectAt("queue")
    require(
      structure == null ||
        (structure.field("waveQueue") == null &&
          structure.field("fmRadioQueue") == null &&
          structure.field("generativeQueue") == null),
    ) {
      "Radio and My Wave are not supported"
    }
    require(queue.text("entityType") !in setOf("RADIO", "WAVE")) {
      "Radio and My Wave are not supported"
    }
    val list = queue.arrayAt("playableList") ?: error("Missing queue tracks")
    require(list.size() in 1..2000) { "Unsupported queue size" }
    val snake = queue.has("playable_list")
    fun key(camel: String) = if (snake) snake(camel) else camel
    val track =
      JsonObject().apply {
        addProperty(key("playableId"), id)
        addProperty(key("playableType"), "TRACK")
      }
    list.add(track)
    queue.add(
      "version",
      JsonObject().apply {
        addProperty(key("deviceId"), deviceId)
        addProperty("version", now)
        addProperty(key("timestampMs"), now)
      },
    )
    return JsonObject().apply {
      add(key("updatePlayerState"), JsonObject().apply { add(key("playerState"), player) })
      addProperty("rid", rid)
      addProperty(key("playerActionTimestampMs"), now)
      addProperty(key("activityInterceptionType"), "DO_NOT_INTERCEPT_BY_DEFAULT")
    }
  }

  fun confirmed(state: JsonObject, id: String, previousSize: Int, rid: String): Boolean {
    if (state.text("rid") != rid) return false
    val list =
      state.objectAt("playerState")?.objectAt("playerQueue")?.arrayAt("playableList")
        ?: return false
    return list.size() > previousSize &&
      (previousSize until list.size()).any {
        list[it].takeIf { e -> e.isJsonObject }?.asJsonObject?.text("playableId") == id
      }
  }
}
