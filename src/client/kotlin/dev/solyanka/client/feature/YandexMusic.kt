/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import com.google.gson.JsonParser
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.client.ui.nofrills.NoFrillsClickGui
import dev.solyanka.utility.LocalPlayback
import dev.solyanka.utility.LyricLine
import dev.solyanka.utility.MusicLogic
import dev.solyanka.utility.MusicSettings
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/** Windows local media session; LRCLIB is contacted only when lyrics are explicitly enabled. */
object YandexMusic {
    private val generation=AtomicInteger()
    private val localLock=Any()
    private var localProcess:Process?=null
    @Volatile var status="Disconnected";private set
    @Volatile private var local:LocalPlayback?=null
    private data class Lyrics(val key:String,val lines:List<LyricLine>)
    @Volatile private var lyrics:Lyrics?=null
    @Volatile private var lyricsEnabled=false
    @Volatile private var requestedKey=""
    @Volatile private var retryAfterMillis=0L
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build()
    // shortcut: one running and one pending lyrics request, upgrade to cancellation if long requests become common.
    private val lyricWorker=ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,ArrayBlockingQueue(1),{r->Thread(r,"solyanka-lyrics").apply {isDaemon=true}},ThreadPoolExecutor.DiscardOldestPolicy())
    private val coverId=Identifier.fromNamespaceAndPath("solyanka","dynamic/music_cover")
    private var coverKey=""
    private var coverLoaded=false
    private const val WIDTH=208
    private fun height(s:MusicSettings)=if(s.showLyrics) 80 else 66
    private fun key(info:LocalPlayback) = info.title+"\u0000"+info.artist

    fun disconnect() {
        generation.incrementAndGet()
        synchronized(localLock) {localProcess?.destroyForcibly();localProcess=null}
        local=null;lyrics=null;requestedKey="";lyricsEnabled=false;status="Disconnected"
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
                        if(line.length>355_000) continue
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
    // Windows media controls do not expose a way to search and append tracks to the desktop queue.
    fun suggest(query:String,callback:(Boolean,String)->Unit):Boolean = false

    private fun refreshLyrics(info:LocalPlayback?,enabled:Boolean) {
        lyricsEnabled=enabled
        if(!enabled || info==null) {lyrics=null;requestedKey="";return}
        val k=key(info)
        if(lyrics?.key!=k) lyrics=null
        if(info.artist.isBlank() || info.duration !in 1.0..3600.0 || requestedKey==k || System.currentTimeMillis()<retryAfterMillis) return
        requestedKey=k
        val gen=generation.get()
        lyricWorker.execute {
            if(!lyricsEnabled || gen!=generation.get() || local?.let(::key)!=k) return@execute
            try {
                fun enc(s:String)=URLEncoder.encode(s,Charsets.UTF_8)
                val uri=URI("https://lrclib.net/api/get?track_name=${enc(info.title)}&artist_name=${enc(info.artist)}&duration=${info.duration.roundToInt().coerceIn(1,3600)}")
                val req=HttpRequest.newBuilder(uri).header("User-Agent","Solyanka/1.4 (https://github.com/eppopka/solyanka)").timeout(Duration.ofSeconds(10)).GET().build()
                val response=http.send(req,HttpResponse.BodyHandlers.ofInputStream())
                val body=response.body().use {it.readNBytes(524289)}
                if(response.statusCode()==429) {
                    val raw=response.headers().firstValue("Retry-After").orElse("60")
                    val wait=raw.toLongOrNull()?.coerceAtLeast(1) ?: if(raw.all {it.isDigit()} && raw.isNotEmpty()) Long.MAX_VALUE else 60L
                    val now=System.currentTimeMillis()
                    retryAfterMillis=if(wait>(Long.MAX_VALUE-now)/1000) Long.MAX_VALUE else now+wait*1000
                    if(gen==generation.get() && local?.let(::key)==k) requestedKey=""
                    return@execute
                }
                val lines=if(response.statusCode()==200 && body.size<=524288) runCatching {
                    val result=JsonParser.parseString(body.toString(Charsets.UTF_8)).asJsonObject
                    val synced=result.get("syncedLyrics")?.takeIf {it.isJsonPrimitive && it.asJsonPrimitive.isString}?.asString.orEmpty()
                    MusicLogic.lyrics(synced)
                }.getOrDefault(emptyList()) else emptyList()
                if(lyricsEnabled && gen==generation.get() && local?.let(::key)==k) lyrics=Lyrics(k,lines)
            } catch(e:Exception) {
                if(lyricsEnabled && gen==generation.get() && local?.let(::key)==k) lyrics=Lyrics(k,emptyList())
            }
        }
    }
    private fun updateCover(value:String) {
        if(value==coverKey) return
        if(coverLoaded) Minecraft.getInstance().textureManager.release(coverId)
        coverLoaded=false;coverKey=value
        if(value.isEmpty()) return
        try {
            val bytes=java.util.Base64.getDecoder().decode(value);require(bytes.size<=262144)
            val input=javax.imageio.ImageIO.createImageInputStream(java.io.ByteArrayInputStream(bytes)) ?: return
            input.use {
                val readers=javax.imageio.ImageIO.getImageReaders(it);if(!readers.hasNext()) return
                val reader=readers.next()
                try {reader.input=it;require(reader.getWidth(0) in 1..2048 && reader.getHeight(0) in 1..2048)} finally {reader.dispose()}
            }
            val image=com.mojang.blaze3d.platform.NativeImage.read(java.io.ByteArrayInputStream(bytes))
            val texture=try {net.minecraft.client.renderer.texture.DynamicTexture({"solyanka music cover"},image)} catch(e:Exception) {image.close();throw e}
            Minecraft.getInstance().textureManager.register(coverId,texture);texture.upload();coverLoaded=true
        } catch(e:Exception) {coverLoaded=false}
    }
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
        if(s.showLyrics) {g.fill(7,65,201,66,0xff333e50.toInt());g.text(font,fit(line ?: "♪",194),8,70,0xffd5deea.toInt(),false)}
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
            info.artist.isBlank() || info.duration !in 1.0..3600.0 -> "Lyrics need artist and track time"
            System.currentTimeMillis()<retryAfterMillis && data==null -> "Lyrics temporarily rate limited"
            data==null -> "Loading lyrics…"
            data.lines.isEmpty() -> "Synced lyrics unavailable"
            else -> MusicLogic.lyricAt(data.lines,MusicLogic.elapsed(info,System.nanoTime()))
        }
        g.pose().pushMatrix();try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);draw(g,info,s,coverLoaded,line)} finally {g.pose().popMatrix()}
    }}
}
