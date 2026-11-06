/* SPDX-License-Identifier: GPL-3.0-only
 * Ynison protocol and device flow informed by https://github.com/MarshalX/yandex-music-api (LGPL-3.0). */
package dev.solyanka.client.feature

import com.google.gson.*
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.client.ui.nofrills.NoFrillsClickGui
import dev.solyanka.utility.MusicLogic
import dev.solyanka.utility.MusicServiceFailure
import dev.solyanka.utility.MusicServerFailure
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier
import java.net.URI
import java.net.URLEncoder
import java.net.http.*
import java.nio.ByteBuffer
import java.time.Duration
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/** Explicit session-only login; never write credentials to settings, logs, or chat. */
object YandexMusic {
    private const val CLIENT_ID="23cabbbdc6cd418abb4b39c32c41195d"
    private const val CLIENT_SECRET="53bc75238f0c4d08a118e51fe9203300"
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val generation=AtomicInteger()
    private val requests=ArrayBlockingQueue<Request>(8)
    private data class Request(val query:String,val callback:(Boolean,String)->Unit)
    @Volatile var status="Disconnected";private set
    data class Verification(val url:URI,val code:String)
    @Volatile var verification:Verification?=null;private set
    @Volatile var track:String?=null;private set
    @Volatile private var socket:WebSocket?=null
    @Volatile private var connected=false
    private fun enc(s:String)=URLEncoder.encode(s,Charsets.UTF_8)
    private fun httpJson(uri:String,token:String?=null,form:Map<String,String>?=null):JsonObject {
        val builder=HttpRequest.newBuilder(URI(uri)).timeout(Duration.ofSeconds(12))
        if(token!=null) builder.header("Authorization","OAuth $token")
        if(form!=null) builder.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.entries.joinToString("&") {enc(it.key)+"="+enc(it.value)})) else builder.GET()
        val response=http.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream())
        val data=response.body().use {it.readNBytes(2_000_001)};require(data.size<=2_000_000) {"Response too large"}
        val json=try {JsonParser.parseString(data.toString(Charsets.UTF_8)).asJsonObject} catch(e:Exception) {
            if(response.statusCode() !in 200..299) throw MusicServiceFailure("HTTP ${response.statusCode()}")
            throw e
        }
        val authError=json.get("error")?.takeIf {it.isJsonPrimitive}?.asString
        if(response.statusCode() !in 200..299 && authError !in setOf("authorization_pending","slow_down"))
            throw MusicServiceFailure("HTTP ${response.statusCode()}")
        return json
    }
    private class Frames:WebSocket.Listener {
        val queue=ArrayBlockingQueue<String>(32);private val text=StringBuilder()
        override fun onOpen(ws:WebSocket) {ws.request(1)}
        override fun onText(ws:WebSocket,data:CharSequence,last:Boolean):CompletionStage<*>? {
            if(text.length+data.length>2_000_000) {ws.abort();return null}
            text.append(data)
            if(last) {if(!queue.offer(text.toString())) {queue.poll();queue.offer(text.toString())};text.setLength(0)}
            ws.request(1);return null
        }
        override fun onError(ws:WebSocket,error:Throwable) {queue.offer("{\"error\":\"connection closed\"}")}
        override fun onClose(ws:WebSocket,statusCode:Int,reason:String):CompletionStage<*>? {queue.offer("{\"error\":\"connection closed\"}");return null}
    }
    private fun open(uri:String,token:String,device:String,ticket:String?=null,session:String?=null):Pair<WebSocket,Frames> {
        val info=JsonObject().apply {
            addProperty("Ynison-Device-Id",device)
            addProperty("Ynison-Device-Info", "{\"app_name\":\"Solyanka\",\"type\":\"1\"}")
            if(ticket!=null) {addProperty("Ynison-Redirect-Ticket",ticket);addProperty("Ynison-Session-Id",session)}
        }
        val listener=Frames()
        val ws=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).header("Origin","https://music.yandex.ru")
            .header("Authorization","OAuth $token").subprotocols("Bearer","v2",enc(info.toString()).replace("+","%20"))
            .buildAsync(URI(uri),listener).get(12,TimeUnit.SECONDS)
        return ws to listener
    }
    private fun frame(listener:Frames,seconds:Long=10):JsonObject {
        val raw=listener.queue.poll(seconds,TimeUnit.SECONDS) ?: throw MusicServiceFailure("timeout")
        val json=JsonParser.parseString(raw).asJsonObject
        if(json.has("error")) throw MusicLogic.serverFailure(json.get("error"))
        return json
    }
    private fun registration(device:String):String = JsonParser.parseString("""{"updateFullState":{"playerState":{"playerQueue":{"currentPlayableIndex":-1,"options":{"repeatMode":"NONE"},"version":{"deviceId":"$device"},"entityType":"VARIOUS","entityContext":"BASED_ON_ENTITY_BY_DEFAULT"},"status":{"paused":true,"playbackSpeed":1,"version":{"deviceId":"$device"}}},"device":{"capabilities":{"canBePlayer":false,"canBeRemoteController":true,"volumeGranularity":0},"info":{"deviceId":"$device","type":"WEB","title":"Solyanka","appName":"yandex-music"},"volumeInfo":{"volume":0}},"isCurrentlyActive":false},"rid":"${UUID.randomUUID()}","activityInterceptionType":"DO_NOT_INTERCEPT_BY_DEFAULT"}""").toString()
    fun disconnect() {
        generation.incrementAndGet();connected=false;socket?.abort();socket=null;track=null;verification=null;status="Disconnected"
        while(true) {val r=requests.poll() ?: break;r.callback(false,"Music disconnected")}
    }
    fun connect() {
        if(!S.isEnabled(S.MUSIC)) {status="Enable Yandex Music first";return}
        disconnect();val gen=generation.get();status="Requesting sign-in code…"
        Thread({
            var ws:WebSocket?=null
            var stage="Sign-in code"
            try {
                val device=UUID.randomUUID().toString().replace("-","").take(12)
                val code=httpJson("https://oauth.yandex.ru/device/code",form=mapOf("client_id" to CLIENT_ID,"device_id" to device,"device_name" to "Solyanka"))
                val url=URI(code.get("verification_url").asString)
                require(url.scheme=="https" && url.host in setOf("oauth.yandex.ru","ya.ru") && url.userInfo==null && url.port==-1)
                val userCode=code.get("user_code").asString;require(userCode.matches(Regex("[A-Za-z0-9-]{1,32}")))
                val privateCode=code.get("device_code").asString
                val expires=code.get("expires_in").asLong.coerceIn(10,900)
                var interval=code.get("interval").asLong.coerceIn(3,30)
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(expires)
                if(gen!=generation.get()) return@Thread
                verification=Verification(url,userCode)
                status="${url.host}${url.path}  code: $userCode"
                stage="Sign-in"
                var token:String?=null
                while(token==null && gen==generation.get() && System.nanoTime()<deadline) {
                    Thread.sleep(interval*1000)
                    if(gen!=generation.get()) return@Thread
                    val result=httpJson("https://oauth.yandex.ru/token",form=mapOf("grant_type" to "device_code","code" to privateCode,"client_id" to CLIENT_ID,"client_secret" to CLIENT_SECRET))
                    if(result.get("error")?.asString=="slow_down") interval=(interval+5).coerceAtMost(30)
                    token=result.get("access_token")?.asString
                }
                val auth=token ?: throw MusicServiceFailure("code expired")
                if(gen!=generation.get()) return@Thread
                verification=null;status="Connecting…"
                var attempt=0
                lateinit var frames:Frames
                lateinit var state:JsonObject
                while(true) {
                    try {
                        stage="Music redirect"
                        val (redirect,rf)=open("wss://ynison.music.yandex.ru/redirector.YnisonRedirectService/GetRedirectToYnison",auth,device)
                        val response=try {frame(rf)} finally {redirect.abort()}
                        if(gen!=generation.get()) return@Thread
                        val host=response.get("host").asString.lowercase()
                        if(!host.matches(Regex("[a-z0-9.-]+\\.music\\.yandex\\.(ru|net)"))) throw MusicServiceFailure("unrecognized server")
                        stage="Music connection"
                        val pair=open("wss://$host/ynison_state.YnisonStateService/PutYnisonState",auth,device,MusicLogic.redirectValue(response,"redirectTicket"),MusicLogic.redirectValue(response,"sessionId"))
                        ws=pair.first
                        if(gen!=generation.get()) return@Thread
                        socket=ws;frames=pair.second
                        stage="Music registration"
                        ws.sendText(registration(device),true).get(10,TimeUnit.SECONDS)
                        state=frame(frames)
                        if(gen!=generation.get()) return@Thread
                        break
                    } catch(e:MusicServerFailure) {
                        ws?.abort();ws=null
                        if(gen!=generation.get()) return@Thread
                        socket=null
                        val delay=MusicLogic.retryDelay(e,attempt) ?: throw e
                        attempt++
                        status="Music unavailable; retrying ($attempt/3)…"
                        val until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(delay)
                        while(gen==generation.get() && System.nanoTime()<until) Thread.sleep(250)
                        if(gen!=generation.get()) return@Thread
                    }
                }
                connected=true;status="Connected (session only)";stage="Music session"
                var lastId="";var ping=System.nanoTime()
                while(gen==generation.get()) {
                    val idAndTitle=runCatching {MusicLogic.current(state)}.getOrNull()
                    if(idAndTitle==null) {track=null;lastId=""}
                    else if(idAndTitle.first!=lastId) {
                        val id=idAndTitle.first
                        track=runCatching {
                            val metadata=httpJson("https://api.music.yandex.net/tracks/$id",auth).getAsJsonArray("result")[0].asJsonObject
                            val title=metadata.get("title")?.asString ?: idAndTitle.second
                            val artist=metadata.getAsJsonArray("artists")?.mapNotNull {it.asJsonObject.get("name")?.asString}?.joinToString(", ").orEmpty()
                            if(artist.isBlank()) title else "$artist — $title"
                        }.getOrElse {idAndTitle.second.takeIf(String::isNotBlank) ?: "Track $id"};lastId=id
                    }
                    val request=requests.poll()
                    if(request!=null) {
                        try {
                            val encoded=enc(request.query)
                            val search=httpJson("https://api.music.yandex.net/search?type=track&text=$encoded",auth)
                            val first=search.getAsJsonObject("result").getAsJsonObject("tracks").getAsJsonArray("results").firstOrNull()?.asJsonObject ?: error("No matching track")
                            val id=first.get("id").asString;val name=first.get("title")?.asString ?: id
                            // Drain already-received changes before constructing a whole-state update.
                            while(true) {val next=frames.queue.poll() ?: break;state=JsonParser.parseString(next).asJsonObject;require(!state.has("error"))}
                            val original=MusicLogic.queueSize(state)
                            val rid=UUID.randomUUID().toString()
                            val payload=MusicLogic.appendRequest(state,id,device,rid,System.currentTimeMillis())
                            ws.sendText(payload.toString(),true).get(10,TimeUnit.SECONDS)
                            val limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);var ok=false
                            while(gen==generation.get() && System.nanoTime()<limit && !ok) {
                                val next=frames.queue.poll(1,TimeUnit.SECONDS) ?: continue
                                state=JsonParser.parseString(next).asJsonObject;require(!state.has("error"))
                                ok=MusicLogic.confirmed(state,id,original,rid)
                            }
                            request.callback(ok,if(ok) name else "Queue update not confirmed; check the app")
                        } catch(e:Exception) {request.callback(false,"Queue request failed or unsupported; check the app")}
                    }
                    val next=frames.queue.poll(2,TimeUnit.SECONDS)
                    if(next!=null) {state=JsonParser.parseString(next).asJsonObject;require(!state.has("error"))}
                    if(System.nanoTime()-ping>TimeUnit.SECONDS.toNanos(20)) {ws.sendPing(ByteBuffer.wrap(byteArrayOf(1)));ping=System.nanoTime()}
                }
            } catch(e:Exception) {if(gen==generation.get()) status=MusicLogic.failure(stage,e)}
            finally {ws?.abort();if(gen==generation.get()) {connected=false;socket=null;track=null;verification=null;while(true) {val r=requests.poll() ?: break;r.callback(false,"Music disconnected")}}}
        },"solyanka-yandex-music").apply {isDaemon=true;start()}
    }
    fun suggest(query:String,callback:(Boolean,String)->Unit):Boolean {
        if(!connected || query.isBlank() || query.length>100 || query.any {it.isISOControl()}) return false
        return requests.offer(Request(query,callback))
    }
    fun register() {HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","yandex_music")) {g,_->
        val c=Minecraft.getInstance();val value=track ?: return@attachElementBefore
        if(!S.isEnabled(S.MUSIC) || c.player==null || c.level==null || c.gui.hud.isHidden) return@attachElementBefore
        val screen=c.gui.screen();if(screen is HudEditorScreen || screen!=null && screen !is ChatScreen && screen !is NoFrillsClickGui) return@attachElementBefore
        val h=S.currentGeneral().features.music.hud;if(!h.enabled) return@attachElementBefore
        val scale=h.scale.toFloat();val x=h.x.coerceIn(0,(c.window.guiScaledWidth-c.font.width(value)*scale).roundToInt().coerceAtLeast(0));val y=h.y.coerceIn(0,(c.window.guiScaledHeight-10*scale).roundToInt().coerceAtLeast(0))
        g.pose().pushMatrix();try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);g.text(c.font,value,0,0,h.color,true)} finally {g.pose().popMatrix()}
    }}
}
