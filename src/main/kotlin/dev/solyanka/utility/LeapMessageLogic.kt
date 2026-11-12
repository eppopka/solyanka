/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

data class LeapMessageSettings(
  val message: String = "Leaped to {name}!",
  val countLeaps: Boolean = false,
) {
  fun validate() {
    require(message.length <= 180 && message.none { it.isISOControl() || it == '§' }) {
      "Leap message must be at most 180 characters and contain no control codes"
    }
  }
}

object LeapMessageLogic {
  private val confirmation = Regex("^You have teleported to ([A-Za-z0-9_]{1,16})!$")

  fun target(message: String) =
    confirmation.matchEntire(ChatLogic.plain(message))?.groupValues?.get(1)

  fun format(template: String, name: String, count: Long?) =
    template.replace("{name}", name).replace("{leapcount}", count?.toString() ?: "?")
}

/**
 * A corrupt counter file is never replaced with an empty one. Counts are keyed by case-insensitive
 * name.
 */
class LeapCountStore(private val path: Path) {
  private var counts = emptyMap<String, Long>()
  var error: String? = null
    private set

  fun load() {
    try {
      if (Files.exists(path)) {
        require(Files.size(path) <= 1024 * 1024)
        val root = JsonParser.parseString(Files.readString(path)).asJsonObject
        require(root.size() <= 5000)
        counts =
          root.entrySet().associate { (name, value) ->
            require(
              name.matches(Regex("[a-z0-9_]{1,16}")) &&
                value.isJsonPrimitive &&
                value.asJsonPrimitive.isNumber,
            )
            val count = value.asBigDecimal.longValueExact()
            require(count >= 0)
            name to count
          }
      }
      error = null
    } catch (e: Exception) {
      error = "Leap counts unreadable; writes disabled: ${e.message}"
    }
  }

  fun increment(name: String): Long {
    check(error == null) { error!! }
    require(BlocklistCodec.name(name))
    val key = name.lowercase(Locale.ROOT)
    require(key in counts || counts.size < 5000)
    val next = Math.addExact(counts[key] ?: 0L, 1L)
    val updated = counts + (key to next)
    Files.createDirectories(path.parent)
    val temp = Files.createTempFile(path.parent, "solyanka-leaps-", ".tmp")
    try {
      Files.writeString(temp, GsonBuilder().setPrettyPrinting().create().toJson(updated))
      try {
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
      }
      counts = updated
    } finally {
      Files.deleteIfExists(temp)
    }
    return next
  }
}
