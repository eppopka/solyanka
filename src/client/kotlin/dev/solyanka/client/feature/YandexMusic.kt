/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import com.google.gson.*
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.client.ui.nofrills.NoFrillsClickGui
import dev.solyanka.utility.LocalPlayback
import dev.solyanka.utility.LyricLine
import dev.solyanka.utility.MusicLogic
import dev.solyanka.utility.MusicServerFailure
import dev.solyanka.utility.MusicServiceFailure
import dev.solyanka.utility.MusicSettings
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.resources.Identifier
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import kotlin.math.roundToInt

/** Windows local media session; LRCLIB is contacted only when lyrics are explicitly enabled. */
object YandexMusic {
    private const val CLIENT_ID="23cabbbdc6cd418abb4b39c32c41195d"
    private const val CLIENT_SECRET="53bc75238f0c4d08a118e51fe9203300"
    private val generation=AtomicInteger()
    private val localLock=Any()
    private var localProcess:Process?=null
    private val requests=ArrayBlockingQueue<Request>(8)
    private data class Request(val query:String,val callback:(Boolean,String)->Unit)
    @Volatile var status="Disconnected";private set
    @Volatile private var local:LocalPlayback?=null
    data class Verification(val url:URI,val code:String)
    @Volatile var verification:Verification?=null;private set
    @Volatile private var socket:WebSocket?=null
    @Volatile private var authenticated=false
    @Volatile private var retryAfterNanos:Long?=null
    private data class Lyrics(val key:String,val lines:List<LyricLine>)
    @Volatile private var lyrics:Lyrics?=null
    @Volatile private var lyricsEnabled=false
    @Volatile private var requestedKey=""
    @Volatile private var retryAfterMillis=0L
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val coverHttp=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build()
    // shortcut: one running and one pending lyrics request, upgrade to cancellation if long requests become common.
    private val lyricWorker=ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,ArrayBlockingQueue(1),{r->Thread(r,"solyanka-lyrics").apply {isDaemon=true}},ThreadPoolExecutor.DiscardOldestPolicy())
    private val coverId=Identifier.fromNamespaceAndPath("solyanka","dynamic/music_cover")
    private var coverKey=""
    private var coverLoaded=false
    private const val WIDTH=208
    private var lyricShown=""
    private var lyricPrevious=""
    private var lyricChangedAt=0L
    private const val LYRIC_TRANSITION_NANOS=260_000_000L
    private fun height(s:MusicSettings)=if(s.showLyrics) 92 else 66
    private fun key(info:LocalPlayback) = info.title+"\u0000"+info.artist

    private fun waitRemainingSeconds():Long {
        val remaining=(retryAfterNanos ?: return 0L)-System.nanoTime()
        return if(remaining>0) (remaining+999_999_999L)/1_000_000_000L else 0L
    }
    private fun enc(s:String)=URLEncoder.encode(s,Charsets.UTF_8)
    private fun deviceId(token:String):String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        .take(6).joinToString("") {"%02x".format(it.toInt() and 0xff)}
    private fun httpJson(uri:String,token:String?=null,form:Map<String,String>?=null):JsonObject {
        val builder=HttpRequest.newBuilder(URI(uri)).timeout(Duration.ofSeconds(12))
        if(token!=null) builder.header("Authorization","OAuth $token")
        if(form!=null) {
            builder.header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.entries.joinToString("&") {enc(it.key)+"="+enc(it.value)}))
        } else builder.GET()
        val response=http.send(builder.build(),HttpResponse.BodyHandlers.ofInputStream())
        val data=response.body().use {it.readNBytes(2_000_001)}
        require(data.size<=2_000_000) {"Response too large"}
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
        val queue=ArrayBlockingQueue<String>(32)
        private val text=StringBuilder()
        override fun onOpen(ws:WebSocket) {ws.request(1)}
        override fun onText(ws:WebSocket,data:CharSequence,last:Boolean):java.util.concurrent.CompletionStage<*>? {
            if(text.length+data.length>2_000_000) {ws.abort();return null}
            text.append(data)
            if(last) {if(!queue.offer(text.toString())) {queue.poll();queue.offer(text.toString())};text.setLength(0)}
            ws.request(1);return null
        }
        override fun onError(ws:WebSocket,error:Throwable) {queue.offer("{\"error\":\"connection closed\"}")}
        override fun onClose(ws:WebSocket,statusCode:Int,reason:String):java.util.concurrent.CompletionStage<*>? {
            queue.offer("{\"error\":\"connection closed\"}");return null
        }
    }
    private fun open(uri:String,token:String,device:String,ticket:String?=null,session:String?=null):Pair<WebSocket,Frames> {
        val info=JsonObject().apply {
            addProperty("Ynison-Device-Id",device)
            addProperty("Ynison-Device-Info","{\"app_name\":\"Solyanka\",\"type\":\"1\"}")
            if(ticket!=null) {addProperty("Ynison-Redirect-Ticket",ticket);addProperty("Ynison-Session-Id",session)}
        }
        val listener=Frames()
        val ws=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
            .header("Origin","https://music.yandex.ru").header("Authorization","OAuth $token")
            .subprotocols("Bearer","v2",enc(info.toString()).replace("+","%20"))
            .buildAsync(URI(uri),listener).get(12,TimeUnit.SECONDS)
        return ws to listener
    }
    private fun frame(listener:Frames,seconds:Long=10):JsonObject {
        val raw=listener.queue.poll(seconds,TimeUnit.SECONDS) ?: throw MusicServiceFailure("timeout")
        val json=JsonParser.parseString(raw).asJsonObject
        if(json.has("error")) throw MusicLogic.serverFailure(json.get("error"))
        return json
    }
    private fun registration(device:String):String = JsonParser.parseString(
        """{"updateFullState":{"playerState":{"playerQueue":{"currentPlayableIndex":-1,"options":{"repeatMode":"NONE"},"version":{"deviceId":"$device"},"entityId":"","entityType":"VARIOUS","entityContext":"BASED_ON_ENTITY_BY_DEFAULT","fromOptional":""},"status":{"paused":true,"playbackSpeed":1,"version":{"deviceId":"$device","timestampMs":0}}},"device":{"capabilities":{"canBePlayer":false,"canBeRemoteController":true,"volumeGranularity":0},"info":{"deviceId":"$device","type":"WEB","title":"Solyanka","appName":"yandex-music"},"volumeInfo":{"volume":0}},"isCurrentlyActive":false},"rid":"${UUID.randomUUID()}","playerActionTimestampMs":0,"activityInterceptionType":"DO_NOT_INTERCEPT_BY_DEFAULT"}"""
    ).toString()

    private fun objectAt(value:JsonObject,key:String):JsonObject? =
        (value.get(key) ?: value.get(key.replace(Regex("[A-Z]")) {"_"+it.value.lowercase()}))
            ?.takeIf {it.isJsonObject}?.asJsonObject
    private fun textAt(value:JsonObject,key:String):String =
        (value.get(key) ?: value.get(key.replace(Regex("[A-Z]")) {"_"+it.value.lowercase()}))
            ?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}?.asString.orEmpty()
    private fun numberAt(value:JsonObject,key:String):Double =
        runCatching {(value.get(key) ?: value.get(key.replace(Regex("[A-Z]")) {"_"+it.value.lowercase()}))?.asDouble ?: 0.0}.getOrDefault(0.0)
    private fun playbackState(state:JsonObject):Pair<Double,Boolean> {
        val player=objectAt(state,"playerState") ?: return 0.0 to false
        val info=objectAt(player,"status") ?: return 0.0 to false
        return numberAt(info,"progressMs")/1000.0 to !runCatching {info.get("paused")?.asBoolean ?: true}.getOrDefault(true)
    }
    private fun cover(auth:String,metadata:JsonObject):String {
        val albums=metadata.get("albums")?.takeIf {it.isJsonArray}?.asJsonArray ?: return ""
        val uri=albums.firstOrNull()?.takeIf {it.isJsonObject}?.asJsonObject?.get("coverUri")
            ?.takeIf {it.isJsonPrimitive}?.asString.orEmpty()
        if(uri.isBlank()) return ""
        val raw=if(uri.startsWith("http")) uri else "https://$uri"
        val url=if(raw.contains("%%")) raw.replace("%%","300x300") else raw
        return runCatching {
            val request=HttpRequest.newBuilder(URI(url)).header("Authorization","OAuth $auth")
                .timeout(Duration.ofSeconds(10)).GET().build()
            val response=coverHttp.send(request,HttpResponse.BodyHandlers.ofInputStream())
            val bytes=response.body().use {it.readNBytes(1_048_577)}
            if(response.statusCode() !in 200..299 || bytes.size>1_048_576) "" else java.util.Base64.getEncoder().encodeToString(bytes)
        }.getOrDefault("")
    }
    private fun authPlayback(state:JsonObject,metadata:JsonObject,auth:String):LocalPlayback {
        val title=metadata.get("title")?.takeIf {it.isJsonPrimitive}?.asString
            ?: MusicLogic.current(state)?.second.orEmpty().ifBlank {"Unknown title"}
        val artist=metadata.get("artists")?.takeIf {it.isJsonArray}?.asJsonArray
            ?.mapNotNull {it.takeIf(JsonElement::isJsonObject)?.asJsonObject?.get("name")?.asString}
            ?.joinToString(", ").orEmpty()
        val duration=numberAt(metadata,"durationMs")/1000.0
        val (position,playing)=playbackState(state)
        return LocalPlayback(title,artist,position,duration,playing,cover(auth,metadata),System.nanoTime())
    }

    fun disconnect() {
        generation.incrementAndGet()
        synchronized(localLock) {localProcess?.destroyForcibly();localProcess=null}
        authenticated=false;socket?.abort();socket=null;verification=null
        local=null;lyrics=null;requestedKey="";lyricsEnabled=false;status="Disconnected"
        while(true) {val request=requests.poll() ?: break;request.callback(false,"Music disconnected")}
    }
    fun connectLocalWindows() {
        if(!S.isEnabled(S.MUSIC)) {status="Enable Yandex Music first";return}
        if(!System.getProperty("os.name","").startsWith("Windows",true)) {status="Local mode requires Windows";return}
        disconnect();val gen=generation.get();status="Local: starting Windows media…"
        Thread({
            var process:Process?=null
            try {
                val script=javaClass.getResourceAsStream("/solyanka-yandex-local.ps1")?.bufferedReader(Charsets.UTF_8)?.use {it.readText()} ?: error("Missing local media reader")
                val encoded=java.util.Base64.getEncoder().encodeToString(script.replace("__PARENT_PID__",ProcessHandle.current().pid().toString()).toByteArray(Charsets.UTF_16LE))
                val powershell=java.nio.file.Paths.get(System.getenv("SystemRoot") ?: error("Missing Windows system path"),"System32","WindowsPowerShell","v1.0","powershell.exe").toString()
                synchronized(localLock) {
                    if(gen!=generation.get()) return@Thread
                    process=ProcessBuilder(powershell,"-NoLogo","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-EncodedCommand",encoded).redirectError(ProcessBuilder.Redirect.DISCARD).start()
                    localProcess=process
                }
                process!!.inputStream.bufferedReader(Charsets.UTF_8).use {input ->
                    while(gen==generation.get()) {
                        val line=input.readLine() ?: break
                        if(line.length>1_450_000) continue
                        val json=runCatching {JsonParser.parseString(line).asJsonObject}.getOrNull() ?: continue
                        if(gen!=generation.get()) break
                        when(json.get("state")?.asString) {
                            "connected" -> {local=MusicLogic.localPlayback(json,System.nanoTime());status="Local: Yandex Music connected"}
                            "waiting" -> {local=null;status="Local: waiting for Yandex Music"}
                            else -> {local=null;status="Local: Windows media read error"}
                        }
                    }
                }
                if(gen==generation.get()) status="Local: Windows media reader stopped"
            } catch(e:Exception) {if(gen==generation.get()) status="Local: unable to read Windows media"}
            finally {
                synchronized(localLock) {if(localProcess===process) localProcess=null}
                process?.destroyForcibly()
                if(gen==generation.get()) local=null
            }
        },"solyanka-local-music").apply {isDaemon=true;start()}
    }

    fun connect() {
        if(!S.isEnabled(S.MUSIC)) {status="Enable Yandex Music first";return}
        val remaining=waitRemainingSeconds()
        if(remaining>0) {status="Music: server requested wait ${remaining}s";return}
        disconnect();val gen=generation.get();status="Requesting sign-in code…"
        Thread({
            var ws:WebSocket?=null
            var stage="Sign-in code"
            try {
                val deviceCodeId=UUID.randomUUID().toString().replace("-","").take(12)
                val code=httpJson("https://oauth.yandex.ru/device/code",form=mapOf("client_id" to CLIENT_ID,"device_id" to deviceCodeId,"device_name" to "Solyanka"))
                val url=URI(code.get("verification_url").asString)
                require(url.scheme=="https" && url.host in setOf("oauth.yandex.ru","ya.ru") && url.userInfo==null && url.port==-1)
                val userCode=code.get("user_code").asString
                require(userCode.matches(Regex("[A-Za-z0-9-]{1,32}")))
                val privateCode=code.get("device_code").asString
                val expires=code.get("expires_in").asLong.coerceIn(10,900)
                var interval=code.get("interval").asLong.coerceIn(3,30)
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(expires)
                verification=Verification(url,userCode);status="${url.host}${url.path}  code: $userCode"
                stage="Sign-in"
                var token:String?=null
                while(token==null && gen==generation.get() && System.nanoTime()<deadline) {
                    Thread.sleep(interval*1000)
                    if(gen!=generation.get()) return@Thread
                    val result=httpJson("https://oauth.yandex.ru/token",form=mapOf(
                        "grant_type" to "device_code","code" to privateCode,
                        "client_id" to CLIENT_ID,"client_secret" to CLIENT_SECRET
                    ))
                    if(result.get("error")?.asString=="slow_down") interval=(interval+5).coerceAtMost(30)
                    token=result.get("access_token")?.asString
                }
                val auth=token ?: throw MusicServiceFailure("code expired")
                if(gen!=generation.get()) return@Thread
                val device=deviceId(auth)
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
                        require(host.matches(Regex("[a-z0-9.-]+\\.music\\.yandex\\.(ru|net)"))) {"unrecognized server"}
                        stage="Music connection"
                        val pair=open("wss://$host/ynison_state.YnisonStateService/PutYnisonState",auth,device,
                            MusicLogic.redirectValue(response,"redirectTicket"),MusicLogic.redirectValue(response,"sessionId"))
                        ws=pair.first
                        if(gen!=generation.get()) return@Thread
                        socket=ws;frames=pair.second;stage="Music registration"
                        ws.sendText(registration(device),true).get(10,TimeUnit.SECONDS)
                        state=frame(frames)
                        if(gen!=generation.get()) return@Thread
                        break
                    } catch(e:MusicServerFailure) {
                        ws?.abort();ws=null;socket=null
                        if(gen!=generation.get()) return@Thread
                        val serverWait=e.goAwaySeconds
                        if(serverWait!=null) retryAfterNanos=System.nanoTime()+TimeUnit.SECONDS.toNanos(serverWait)
                        if(serverWait!=null && (e.code!=14 || attempt>=4))
                            throw MusicServiceFailure("server requested wait ${serverWait}s")
                        val delay=if(serverWait!=null) TimeUnit.SECONDS.toMillis(serverWait)
                            else MusicLogic.retryDelay(e,attempt) ?: throw e
                        attempt++;status=if(serverWait!=null) "Music: server requested wait ${serverWait}s; retrying"
                            else "Music unavailable; retrying ($attempt/4)…"
                        val until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(delay)
                        while(gen==generation.get() && System.nanoTime()<until) Thread.sleep(250)
                        if(gen!=generation.get()) return@Thread
                    }
                }
                authenticated=true;status="Connected (account session)"
                var lastId=""
                var metadata:JsonObject?=null
                var ping=System.nanoTime()
                while(gen==generation.get()) {
                    val current=runCatching {MusicLogic.current(state)}.getOrNull()
                    if(current==null) {local=null;lastId="";metadata=null}
                    else if(current.first!=lastId) {
                        lastId=current.first
                        metadata=runCatching {
                            httpJson("https://api.music.yandex.net/tracks/${enc(current.first)}",auth)
                                .getAsJsonArray("result")[0].asJsonObject
                        }.getOrNull()
                        local=metadata?.let {authPlayback(state,it,auth)}
                            ?: LocalPlayback(current.second,"",playbackState(state).first,0.0,playbackState(state).second,"",System.nanoTime())
                    } else {
                        local=local?.let {old->
                            val (position,playing)=playbackState(state)
                            old.copy(position=position,playing=playing,receivedNanos=System.nanoTime())
                        }
                    }
                    val request=requests.poll()
                    if(request!=null) {
                        try {
                            val search=httpJson("https://api.music.yandex.net/search?type=track&text=${enc(request.query)}",auth)
                            val first=search.getAsJsonObject("result").getAsJsonObject("tracks")
                                .getAsJsonArray("results").firstOrNull()?.asJsonObject ?: error("No matching track")
                            val id=first.get("id").asString
                            val name=first.get("title")?.asString ?: id
                            while(true) {val next=frames.queue.poll() ?: break;state=JsonParser.parseString(next).asJsonObject;require(!state.has("error"))}
                            val original=MusicLogic.queueSize(state);val rid=UUID.randomUUID().toString()
                            val payload=MusicLogic.appendRequest(state,id,device,rid,System.currentTimeMillis())
                            ws?.sendText(payload.toString(),true)?.get(10,TimeUnit.SECONDS)
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
                    if(System.nanoTime()-ping>TimeUnit.SECONDS.toNanos(20)) {
                        ws?.sendPing(ByteBuffer.wrap(byteArrayOf(1)));ping=System.nanoTime()
                    }
                }
            } catch(e:Exception) {if(gen==generation.get()) status=MusicLogic.failure(stage,e)}
            finally {
                ws?.abort()
                if(gen==generation.get()) {authenticated=false;socket=null;verification=null;while(true) {val request=requests.poll() ?: break;request.callback(false,"Music disconnected")}}
            }
        },"solyanka-yandex-music").apply {isDaemon=true;start()}
    }

    fun suggest(query:String,callback:(Boolean,String)->Unit):Boolean {
        if(!authenticated || query.isBlank() || query.length>100 || query.any {it.isISOControl()}) return false
        return requests.offer(Request(query,callback))
    }

    private fun refreshLyrics(info:LocalPlayback?,enabled:Boolean) {
        lyricsEnabled=enabled
        if(!enabled || info==null) {lyrics=null;requestedKey="";return}
        val k=key(info)
        if(lyrics?.key!=k) lyrics=null
        if(info.title.isBlank() || info.artist.isBlank() || requestedKey==k || System.currentTimeMillis()<retryAfterMillis) return
        requestedKey=k
        val gen=generation.get()
        lyricWorker.execute {
            if(!lyricsEnabled || gen!=generation.get() || local?.let(::key)!=k) return@execute
            try {
                fun enc(s:String)=URLEncoder.encode(s,Charsets.UTF_8)
                fun request(uri:URI):Pair<Int,String> {
                    val req=HttpRequest.newBuilder(uri).header("User-Agent","Solyanka/1.4.1 (https://github.com/eppopka/solyanka)")
                        .timeout(Duration.ofSeconds(10)).GET().build()
                    val response=http.send(req,HttpResponse.BodyHandlers.ofInputStream())
                    return response.statusCode() to response.body().use {it.readNBytes(524289).let {bytes->
                        if(bytes.size>524288) "" else bytes.toString(Charsets.UTF_8)
                    }}
                }
                fun setRateLimit(status:Int) {
                    if(status!=429) return
                    val wait=60L
                    val now=System.currentTimeMillis()
                    retryAfterMillis=if(wait>(Long.MAX_VALUE-now)/1000) Long.MAX_VALUE else now+wait*1000
                }
                fun syncedFromObject(body:String):List<LyricLine> = runCatching {
                    val result=JsonParser.parseString(body).asJsonObject
                    val synced=result.get("syncedLyrics")?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}?.asString.orEmpty()
                    MusicLogic.lyrics(synced)
                }.getOrDefault(emptyList())
                var lines=emptyList<LyricLine>()
                if(info.duration in 1.0..3600.0) {
                    val result=request(URI("https://lrclib.net/api/get?track_name=${enc(info.title)}&artist_name=${enc(info.artist)}&duration=${info.duration.roundToInt()}"))
                    setRateLimit(result.first)
                    if(result.first==200) lines=syncedFromObject(result.second)
                }
                if(lines.isEmpty() && System.currentTimeMillis()>=retryAfterMillis) {
                    val result=request(URI("https://lrclib.net/api/search?track_name=${enc(info.title)}&artist_name=${enc(info.artist)}"))
                    setRateLimit(result.first)
                    if(result.first==200) lines=runCatching {
                        JsonParser.parseString(result.second).asJsonArray.asSequence()
                            .mapNotNull {it.takeIf(JsonElement::isJsonObject)?.asJsonObject}
                            .map {it.get("syncedLyrics")?.takeIf {v->v.isJsonPrimitive && v.asJsonPrimitive.isString}?.asString.orEmpty()}
                            .firstOrNull(String::isNotBlank).orEmpty().let(MusicLogic::lyrics)
                    }.getOrDefault(emptyList())
                }
                if(lyricsEnabled && gen==generation.get() && local?.let(::key)==k) {
                    lyrics=Lyrics(k,lines)
                    if(lines.isEmpty()) requestedKey=""
                }
            } catch(e:Exception) {
                if(lyricsEnabled && gen==generation.get() && local?.let(::key)==k) {
                    lyrics=Lyrics(k,emptyList());requestedKey=""
                }
            }
        }
    }
    private fun updateCover(value:String) {
        if(value==coverKey) return
        if(coverLoaded) Minecraft.getInstance().textureManager.release(coverId)
        coverLoaded=false;coverKey=value
        if(value.isEmpty()) return
        try {
            val bytes=java.util.Base64.getDecoder().decode(value);require(bytes.size<=1_048_576)
            val image=com.mojang.blaze3d.platform.NativeImage.read(java.io.ByteArrayInputStream(bytes))
            val texture=try {net.minecraft.client.renderer.texture.DynamicTexture({"solyanka music cover"},image)} catch(e:Exception) {image.close();throw e}
            Minecraft.getInstance().textureManager.register(coverId,texture);texture.upload();coverLoaded=true
        } catch(e:Exception) {coverLoaded=false}
    }
    private fun lyricTransition(value:String,now:Long):Triple<String,String,Float> {
        if(value!=lyricShown) {lyricPrevious=lyricShown;lyricShown=value;lyricChangedAt=now}
        val progress=((now-lyricChangedAt).coerceAtLeast(0L).toDouble()/LYRIC_TRANSITION_NANOS).coerceIn(0.0,1.0).toFloat()
        return Triple(lyricShown,lyricPrevious,progress)
    }
    private fun wrap(font:net.minecraft.client.gui.Font,text:String,width:Int):List<String> {
        var rest=text.trim()
        if(rest.isEmpty()) return listOf("♪")
        val lines=ArrayList<String>(2)
        repeat(2) {
            if(rest.isEmpty()) return@repeat
            val fit=font.plainSubstrByWidth(rest,width)
            if(fit.isEmpty()) return@repeat
            if(fit.length<rest.length) {
                val trimmed=font.plainSubstrByWidth(rest,(width-font.width("…")).coerceAtLeast(1))
                lines.add(trimmed+"…");rest="";return@repeat
            }
            lines.add(fit);rest=""
        }
        return lines.ifEmpty {listOf("♪")}
    }
    private fun alpha(color:Int,amount:Float):Int = ((amount.coerceIn(0f,1f)*255f).toInt() shl 24) or (color and 0xffffff)
    private fun draw(g:GuiGraphicsExtractor,info:LocalPlayback,s:MusicSettings,cover:Boolean,line:String?) {
        val font=Minecraft.getInstance().font;val tall=height(s)
        ChromeDraw.panel(g,0,0,WIDTH,tall,0xe9161b27.toInt(),0xff333e50.toInt(),7)
        ChromeDraw.panel(g,7,7,52,52,0xff242d3b.toInt(),0xff3e4b5c.toInt(),4)
        if(cover) g.blit(coverId,9,9,48,48,0f,1f,0f,1f) else g.text(font,"♪",26,27,0xfffd5d65.toInt(),false)
        fun fit(value:String,max:Int):String = if(font.width(value)<=max) value else font.plainSubstrByWidth(value,(max-font.width("...")).coerceAtLeast(0))+"..."
        g.text(font,fit(s.serviceLabel,75),66,8,s.serviceColor,false)
        val state=fit(if(info.playing) s.playingLabel else "PAUSED",55)
        g.text(font,state,WIDTH-7-font.width(state),8,s.playingColor,false)
        g.text(font,fit(info.title,135),66,22,s.hud.color,false)
        g.text(font,fit(info.artist.ifBlank {"Unknown artist"},135),66,34,0xffaab6c7.toInt(),false)
        val position=MusicLogic.elapsed(info,System.nanoTime());val progress=if(info.duration>0) (position/info.duration).coerceIn(0.0,1.0) else 0.0
        val barColor=MusicLogic.progressColor(s.progressColor,s.progressEffect,s.effectSpeed,System.nanoTime()/1_000_000_000.0)
        g.fill(66,48,201,51,0xff354052.toInt());g.fill(66,48,66+(135*progress).toInt(),51,barColor)
        val time=if(info.duration>0) MusicLogic.timestamp(position)+" / "+MusicLogic.timestamp(info.duration) else "--:-- / --:--"
        g.pose().pushMatrix();try {g.pose().translate(66f,54f);g.pose().scale(.72f,.72f);g.text(font,time,0,0,0xffaab6c7.toInt(),false)} finally {g.pose().popMatrix()}
        if(s.showLyrics) {
            g.fill(7,65,201,89,0x4c1c2635)
            g.fill(7,65,201,66,0xff435870.toInt())
            val (shown,previous,progress)=lyricTransition(line ?: "♪",System.nanoTime())
            g.scissorStack.push(ScreenRectangle(7,66,194,22))
            try {
                if(previous.isNotBlank() && progress<1f) {
                    val oldLines=wrap(font,previous,194)
                    val oldColor=alpha(0xffd5deea.toInt(),1f-progress)
                    oldLines.forEachIndexed {i,value->g.text(font,value,8,68+i*9-(progress*5f).toInt(),oldColor,false)}
                }
                val newLines=wrap(font,shown,194)
                val newColor=alpha(0xfff2f7ff.toInt(),progress)
                newLines.forEachIndexed {i,value->g.text(font,value,8,68+i*9+((1f-progress)*5f).toInt(),newColor,false)}
            } finally {g.scissorStack.pop()}
        }
    }
    fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
        if(key!="extra.music") return false
        val s=S.currentGeneral().features.music
        val sample=LocalPlayback("Song title","Artist",74.0,210.0,false,"",System.nanoTime())
        g.pose().pushMatrix();try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(s.hud.scale.toFloat(),s.hud.scale.toFloat());draw(g,sample.copy(playing=true),s,false,"Lyrics preview")} finally {g.pose().popMatrix()}
        return true
    }
    fun register() {HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","yandex_music")) {g,_->
        val c=Minecraft.getInstance();val info=local;val s=S.currentGeneral().features.music
        refreshLyrics(info,S.isEnabled(S.MUSIC) && s.showLyrics)
        updateCover(info?.art.orEmpty())
        if(info==null || !S.isEnabled(S.MUSIC) || c.player==null || c.level==null || c.gui.hud.isHidden) return@attachElementBefore
        val screen=c.gui.screen();if(screen is HudEditorScreen || screen!=null && screen !is ChatScreen && screen !is NoFrillsClickGui) return@attachElementBefore
        val h=s.hud;if(!h.enabled) return@attachElementBefore
        val scale=h.scale.toFloat();val x=h.x.coerceIn(0,(c.window.guiScaledWidth-WIDTH*scale).roundToInt().coerceAtLeast(0));val y=h.y.coerceIn(0,(c.window.guiScaledHeight-height(s)*scale).roundToInt().coerceAtLeast(0))
        val data=lyrics?.takeIf {it.key==key(info)}
        val line=when {
            info.artist.isBlank() -> "Lyrics need an artist"
            System.currentTimeMillis()<retryAfterMillis && data==null -> "Lyrics temporarily rate limited"
            data==null -> "Loading lyrics…"
            data.lines.isEmpty() -> "Synced lyrics unavailable"
            else -> MusicLogic.lyricAt(data.lines,MusicLogic.elapsed(info,System.nanoTime()))
        }
        g.pose().pushMatrix();try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);draw(g,info,s,coverLoaded,line)} finally {g.pose().popMatrix()}
    }}
}
