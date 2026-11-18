/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.Locale

/** The preview command never queues chat. Live sends remain Hypixel/SkyBlock-only. */
object RngAnnounce {
    private data class Pending(val channel:String,val text:String,val expires:Long)
    private val queue=java.util.ArrayDeque<Pending>()
    private val dedup=DropDeduplicator()
    private var lastSent=0L
    private var chatPrimed=false
    private var lastResult="Waiting for a new drop message"
    private val displayed=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<net.minecraft.client.multiplayer.chat.GuiMessage,Boolean>())
    fun hypixel():Boolean {
        val host=Minecraft.getInstance().currentServer?.ip?.substringBefore(':')?.lowercase(Locale.ROOT) ?: return false
        return host=="hypixel.net" || host.endsWith(".hypixel.net")
    }
    fun reset() {queue.clear();dedup.clear();lastSent=0L;displayed.clear();chatPrimed=false;lastResult="Waiting for a new drop message"}
    private fun clearCapture() {queue.clear();displayed.clear();chatPrimed=false}
    private fun liveBlock():String? = when {
        !S.isEnabled(ModuleIds.RNG_ANNOUNCE)->"module is OFF"
        Minecraft.getInstance().player==null->"no local player"
        !hypixel()->"not connected to Hypixel"
        !FeatureRuntime.context.skyblock->"SkyBlock context not detected"
        S.currentGeneral().features.rngAnnounce.channels().isEmpty()->"no target chats selected"
        else->null
    }
    fun diagnostics():String {
        val c=Minecraft.getInstance();val targets=S.currentGeneral().features.rngAnnounce.channels().joinToString(",").ifBlank {"none"}
        return "RNG ${if(S.isEnabled(ModuleIds.RNG_ANNOUNCE)) "ON" else "OFF"}; player=${c.player?.gameProfile?.name ?: "none"}; "+
            "Hypixel=${hypixel()}; SkyBlock=${FeatureRuntime.context.skyblock}; chats=$targets; "+
            "${liveBlock()?.let {"Sending blocked: $it"} ?: "Sending permitted"}; last: $lastResult"
    }
    /** Extract effective rendered colors, including legacy section-sign formatting, before flattening text. */
    private fun styled(message:Component):RngStyledMessage {
        val text=StringBuilder();val colors=mutableListOf<String?>()
        message.visualOrderText.accept {_,style,point->
            text.appendCodePoint(point)
            repeat(Character.charCount(point)) {colors.add(style.color?.toString()?.lowercase(Locale.ROOT))}
            text.length<2049
        }
        return RngStyledMessage(text.toString(),colors)
    }
    private fun evaluate(message:RngStyledMessage,localDisplay:Boolean) {
        if(!RngAnnounceLogic.candidate(message.text))return
        liveBlock()?.let {lastResult="Ignored: $it";return}
        val player=Minecraft.getInstance().player ?: return
        val drop=RngAnnounceLogic.parse(message.text,player.gameProfile.name)
            ?: run {lastResult="Rejected format/price or player name (expected ${player.gameProfile.name})";return}
        // Bare found-a/an system messages are accepted from GAME events. The display fallback
        // is reserved for SkyHanni and explicitly marked RNG DROP messages, not arbitrary chat.
        if(localDisplay && drop.kind==RngDropKind.FOUND && !drop.message.contains("RNG DROP!",true))return
        val cfg=S.currentGeneral().features.rngAnnounce
        RngAnnounceLogic.colorFailure(message,drop,cfg.requireColors)?.let {lastResult="Rejected: $it";return}
        if(!RngAnnounceLogic.allowed(drop,cfg)) {lastResult="Filtered by minimum value or unknown-value setting";return}
        val now=System.nanoTime()/1000000L
        if(!dedup.accept(drop.item,now)) {lastResult="Suppressed duplicate ${drop.item}";return}
        var queued=0
        cfg.channels().forEach {channel->if(queue.size<12) {queue.add(Pending(channel,RngAnnounceLogic.safeMessage(drop.message),now+15000));queued++}}
        lastResult=if(queued>0) "Queued ${drop.kind} to $queued chat(s)" else "Queue full"
    }
    private fun receive(message:Component,overlay:Boolean) {
        if(overlay || !S.isEnabled(ModuleIds.RNG_ANNOUNCE) || message.string.length>2048)return
        val c=Minecraft.getInstance()
        c.execute {evaluate(styled(message),false)}
    }
    /** Ampersand color codes are supported here only. This never dispatches commands. */
    fun preview(raw:String):String {
        if(raw.length>4096)return "Preview only: message too long; nothing sent"
        val message=RngStyledMessage.preview(raw)
        val ownName=Minecraft.getInstance().player?.gameProfile?.name ?: return "Preview only: join a world first; nothing sent"
        val drop=RngAnnounceLogic.parse(message.text,ownName)
            ?: return "Preview only: no matching format/valid price for $ownName; nothing sent"
        val cfg=S.currentGeneral().features.rngAnnounce
        val color=RngAnnounceLogic.colorFailure(message,drop,cfg.requireColors)
        val result=when {color!=null->"rejected: $color";!RngAnnounceLogic.allowed(drop,cfg)->"filtered by value settings";else->"matches ${drop.kind}, value=${drop.value ?: "unknown"}"}
        return "Preview only: $result; ${liveBlock()?.let {"live blocked: $it"} ?: "live environment ready"}; nothing sent"
    }
    fun tick() {
        val c=Minecraft.getInstance()
        if(!S.isEnabled(ModuleIds.RNG_ANNOUNCE) || c.player==null || c.level==null) {clearCapture();return}
        // Inspect new locally inserted messages even outside Hypixel so the status can explain
        // a blocked test, but evaluate() and the send step both enforce liveBlock().
        val access=c.gui.hud.chat as dev.solyanka.client.mixin.ChatComponentAccessor
        val parents=access.solyankaLines().take(128).map {it.parent()}.distinct()
        if(chatPrimed) parents.asReversed().filterNot {it in displayed}.forEach {message->
            val component=message.content()
            if(component.string.length<=2048 && RngAnnounceLogic.candidate(component.string))evaluate(styled(component),true)
        }
        displayed.clear();displayed.addAll(parents);chatPrimed=true
        if(liveBlock()!=null) {queue.clear();return}
        val now=System.nanoTime()/1000000L
        while(queue.isNotEmpty() && queue.peek().expires<now)queue.remove()
        if(now-lastSent<1200 || queue.isEmpty())return
        val next=queue.remove()
        if(next.channel !in S.currentGeneral().features.rngAnnounce.channels())return
        c.player?.connection?.sendCommand("${next.channel} ${next.text}")
        lastSent=now;lastResult="Sent to /${next.channel}"
    }
    fun register() {
        // Deliberately no player CHAT callback: ordinary player messages aren't drop events.
        ClientReceiveMessageEvents.GAME.register {message,overlay->receive(message,overlay)}
        ClientReceiveMessageEvents.GAME_CANCELED.register {message,overlay->receive(message,overlay)}
    }
}
