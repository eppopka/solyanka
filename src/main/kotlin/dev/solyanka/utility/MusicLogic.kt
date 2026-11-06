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

    private fun JsonObject.objectAt(key:String):JsonObject? = get(key)?.takeIf {it.isJsonObject}?.asJsonObject
    private fun JsonObject.text(key:String):String? = get(key)?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}?.asString
    fun current(state:JsonObject):Pair<String,String>? {
        if(state.text("activeDeviceIdOptional").isNullOrBlank()) return null
        val ps=state.objectAt("playerState") ?: return null
        val queue=ps.objectAt("playerQueue") ?: return null
        val list=queue.get("playableList")?.takeIf {it.isJsonArray}?.asJsonArray ?: return null
        val index=queue.get("currentPlayableIndex")?.asInt ?: 0
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
        require(!queue.has("shuffleOptional") || queue.get("shuffleOptional").isJsonNull) {"Shuffle is not supported for experimental queue requests"}
        val structure=queue.objectAt("queue")
        require(structure==null || (!structure.has("waveQueue") && !structure.has("fmRadioQueue") && !structure.has("generativeQueue"))) {"Radio and My Wave are not supported"}
        require(queue.text("entityType") !in setOf("RADIO","WAVE")) {"Radio and My Wave are not supported"}
        val list=queue.get("playableList")?.takeIf {it.isJsonArray}?.asJsonArray ?: error("Missing queue tracks")
        require(list.size() in 1..2000) {"Unsupported queue size"}
        val track=JsonObject().apply {addProperty("playableId",id);addProperty("playableType","TRACK")}
        list.add(track)
        queue.add("version",JsonObject().apply {addProperty("deviceId",deviceId);addProperty("version",now);addProperty("timestampMs",now)})
        return JsonObject().apply {
            add("updatePlayerState",JsonObject().apply {add("playerState",player)})
            addProperty("rid",rid);addProperty("playerActionTimestampMs",now);addProperty("activityInterceptionType","DO_NOT_INTERCEPT_BY_DEFAULT")
        }
    }
    fun confirmed(state:JsonObject,id:String,previousSize:Int,rid:String):Boolean {
        if(state.text("rid")!=rid) return false
        val list=state.objectAt("playerState")?.objectAt("playerQueue")?.get("playableList")?.takeIf {it.isJsonArray}?.asJsonArray ?: return false
        return list.size()>previousSize && (previousSize until list.size()).any { list[it].takeIf {e->e.isJsonObject}?.asJsonObject?.text("playableId")==id }
    }
}
