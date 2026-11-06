/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*
import java.net.http.HttpTimeoutException
import java.net.http.WebSocketHandshakeException
import java.util.concurrent.ExecutionException
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

data class MusicSettings(val hud:HudSpec=HudSpec(x=12,y=245),val suggest:Boolean=true,val announce:Boolean=false) {
    fun validate() { hud.validate() }
}
class MusicServiceFailure(val reason:String):RuntimeException()

object MusicLogic {
    fun failure(stage:String,error:Throwable):String {
        var cause=error
        while((cause is ExecutionException || cause is CompletionException) && cause.cause!=null) cause=cause.cause!!
        val reason=when(cause) {
            is MusicServiceFailure -> cause.reason
            is WebSocketHandshakeException -> "HTTP ${cause.response.statusCode()}"
            is HttpTimeoutException, is TimeoutException -> "timeout"
            is java.net.UnknownHostException -> "DNS error"
            is javax.net.ssl.SSLException -> "TLS error"
            is java.net.ConnectException -> "network error"
            else -> "unexpected response"
        }
        return "$stage: $reason"
    }

    private fun snake(key:String)=key.replace(Regex("[A-Z]")) {"_"+it.value.lowercase()}
    private fun JsonObject.field(key:String):JsonElement? = get(key) ?: get(snake(key))
    private fun JsonObject.objectAt(key:String):JsonObject? = field(key)?.takeIf {it.isJsonObject}?.asJsonObject
    private fun JsonObject.text(key:String):String? = field(key)?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}?.asString
    private fun JsonObject.arrayAt(key:String):JsonArray? = field(key)?.takeIf {it.isJsonArray}?.asJsonArray
    fun redirectValue(response:JsonObject,key:String):String {
        val value=response.field(key)?.takeIf {it.isJsonPrimitive}?.asString
        if(value.isNullOrBlank() || value.length>4096 || value.any {it.isISOControl()}) throw MusicServiceFailure("missing or invalid $key")
        return value
    }
    fun queueSize(state:JsonObject):Int = state.objectAt("playerState")?.objectAt("playerQueue")?.arrayAt("playableList")?.size() ?: error("Missing queue tracks")
    fun current(state:JsonObject):Pair<String,String>? {
        if(state.text("activeDeviceIdOptional").isNullOrBlank()) return null
        val ps=state.objectAt("playerState") ?: return null
        val queue=ps.objectAt("playerQueue") ?: return null
        val list=queue.arrayAt("playableList") ?: return null
        val index=queue.field("currentPlayableIndex")?.asInt ?: 0
        if(index !in 0 until list.size()) return null
        val track=list[index].takeIf {it.isJsonObject}?.asJsonObject ?: return null
        if(track.text("playableType")?.let {it!="TRACK"} == true) return null
        val id=track.text("playableId")?.takeIf {it.matches(Regex("[0-9]{1,20}"))} ?: return null
        return id to (track.text("title") ?: "")
    }
    /** Experimental: retain the complete observed state and append only to an ordinary, non-shuffled queue. */
    fun appendRequest(state:JsonObject,id:String,deviceId:String,rid:String,now:Long):JsonObject {
        require(id.matches(Regex("[0-9]{1,20}")))
        require(!state.text("activeDeviceIdOptional").isNullOrBlank()) {"No active player"}
        val player=state.objectAt("playerState")?.deepCopy() ?: error("Missing player state")
        val queue=player.objectAt("playerQueue") ?: error("Missing player queue")
        require(queue.field("shuffleOptional")?.isJsonNull != false) {"Shuffle is not supported for experimental queue requests"}
        val structure=queue.objectAt("queue")
        require(structure==null || (structure.field("waveQueue")==null && structure.field("fmRadioQueue")==null && structure.field("generativeQueue")==null)) {"Radio and My Wave are not supported"}
        require(queue.text("entityType") !in setOf("RADIO","WAVE")) {"Radio and My Wave are not supported"}
        val list=queue.arrayAt("playableList") ?: error("Missing queue tracks")
        require(list.size() in 1..2000) {"Unsupported queue size"}
        val snake=queue.has("playable_list")
        fun key(camel:String)=if(snake) snake(camel) else camel
        val track=JsonObject().apply {addProperty(key("playableId"),id);addProperty(key("playableType"),"TRACK")}
        list.add(track)
        queue.add("version",JsonObject().apply {addProperty(key("deviceId"),deviceId);addProperty("version",now);addProperty(key("timestampMs"),now)})
        return JsonObject().apply {
            add(key("updatePlayerState"),JsonObject().apply {add(key("playerState"),player)})
            addProperty("rid",rid);addProperty(key("playerActionTimestampMs"),now);addProperty(key("activityInterceptionType"),"DO_NOT_INTERCEPT_BY_DEFAULT")
        }
    }
    fun confirmed(state:JsonObject,id:String,previousSize:Int,rid:String):Boolean {
        if(state.text("rid")!=rid) return false
        val list=state.objectAt("playerState")?.objectAt("playerQueue")?.arrayAt("playableList") ?: return false
        return list.size()>previousSize && (previousSize until list.size()).any { list[it].takeIf {e->e.isJsonObject}?.asJsonObject?.text("playableId")==id }
    }
}
