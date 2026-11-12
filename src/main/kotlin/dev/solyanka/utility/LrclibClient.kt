/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.math.roundToInt

/** Fetches synced LRCLIB lyrics, with an exact-match request before search. */
class LrclibClient(
    private val httpClient:HttpClient=HttpClient.newHttpClient(),
    private val baseUri:URI=URI("https://lrclib.net"),
    private val requestTimeout:Duration=Duration.ofSeconds(10),
    private val maxResponseBytes:Int=MAX_RESPONSE_BYTES,
    private val userAgent:String="Solyanka/1.4.7 (https://github.com/eppopka/solyanka)",
) {
    data class FetchResult(val decision:LrclibRetryPolicy.Decision,val lines:List<LyricLine>)

    init {
        require(baseUri.scheme=="https" || baseUri.scheme=="http")
        require(!baseUri.host.isNullOrBlank() && baseUri.userInfo==null)
        require(baseUri.rawQuery==null && baseUri.rawFragment==null)
        require(!requestTimeout.isNegative && !requestTimeout.isZero)
        require(maxResponseBytes in 1 until Int.MAX_VALUE)
        require(userAgent.isNotBlank())
    }

    /** Interruption is propagated (with the interrupt flag restored) for cancellation. */
    @Throws(InterruptedException::class)
    fun fetch(title:String,artist:String,durationSeconds:Double,nowMillis:Long,attempt:Int=0):FetchResult {
        if(title.isBlank() || artist.isBlank()) return FetchResult(
            LrclibRetryPolicy.classifyHttp(400,LrclibRetryPolicy.Endpoint.SEARCH,false,nowMillis),emptyList(),
        )

        if(durationSeconds in 1.0..3600.0) {
            val exact=get(
                "get",LrclibRetryPolicy.Endpoint.GET,
                listOf("track_name" to title,"artist_name" to artist,"duration" to durationSeconds.roundToInt().toString()),
                nowMillis,attempt,
            )
            if(!exact.decision.shouldSearchFallback) return exact
        }
        return get(
            "search",LrclibRetryPolicy.Endpoint.SEARCH,
            listOf("track_name" to title,"artist_name" to artist),nowMillis,attempt,
        )
    }

    private fun get(
        endpoint:String,
        kind:LrclibRetryPolicy.Endpoint,
        params:List<Pair<String,String>>,
        nowMillis:Long,
        attempt:Int,
    ):FetchResult {
        val query=params.joinToString("&") {(key,value)->"${encode(key)}=${encode(value)}"}
        val uri=URI.create("${baseUri.toString().trimEnd('/')}/api/$endpoint?$query")
        val request=HttpRequest.newBuilder(uri).timeout(requestTimeout).header("User-Agent",userAgent).GET().build()
        val response=try {
            httpClient.send(request,HttpResponse.BodyHandlers.ofInputStream())
        } catch(error:InterruptedException) {
            Thread.currentThread().interrupt()
            throw error
        } catch(_:IOException) {
            return transient(nowMillis,attempt)
        }

        val status=response.statusCode()
        val retryAfter=response.headers().firstValue("Retry-After").orElse(null)
        if(status !in 200..299) {
            runCatching {response.body().close()}
            return FetchResult(
                LrclibRetryPolicy.classifyHttp(status,kind,false,nowMillis,retryAfter,attempt),emptyList(),
            )
        }

        val bytes=try {response.body().use {it.readNBytes(maxResponseBytes+1)}}
        catch(_:IOException) {return transient(nowMillis,attempt)}
        if(bytes.size>maxResponseBytes) return transient(nowMillis,attempt)
        val parsed=runCatching {
            val root=JsonParser.parseString(bytes.toString(StandardCharsets.UTF_8))
            when(kind) {
                LrclibRetryPolicy.Endpoint.GET -> {
                    require(root.isJsonObject)
                    listOfNotNull(root.asJsonObject.get("syncedLyrics").syncedLyrics())
                }
                LrclibRetryPolicy.Endpoint.SEARCH -> {
                    require(root.isJsonArray)
                    root.asJsonArray.mapNotNull {it.takeIf(JsonElement::isJsonObject)?.asJsonObject?.get("syncedLyrics")?.syncedLyrics()}
                }
            }
        }.getOrElse {return transient(nowMillis,attempt)}
        val lines=parsed.asSequence().map(MusicLogic::lyrics).firstOrNull {it.isNotEmpty()}.orEmpty()
        val decision=LrclibRetryPolicy.classifyHttp(status,kind,lines.isNotEmpty(),nowMillis,retryAfter,attempt)
        return FetchResult(decision,lines)
    }

    private fun transient(nowMillis:Long,attempt:Int)=FetchResult(
        LrclibRetryPolicy.networkFailure(nowMillis,attempt),emptyList(),
    )

    private fun encode(value:String)=URLEncoder.encode(value,StandardCharsets.UTF_8)

    private fun com.google.gson.JsonElement?.syncedLyrics():String? =
        this?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}
            ?.asString?.takeIf(String::isNotBlank)

    companion object {const val MAX_RESPONSE_BYTES=512*1024}
}
