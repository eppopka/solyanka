/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft

object VanguardAnnounce {
    private val window=VanguardInviteWindow()
    private val queue=java.util.ArrayDeque<String>()
    private var announced=false
    private var leftAt:Long?=null
    private var nextSend=0L
    private var keyword="+"
    private fun now()=System.nanoTime()/1_000_000L
    fun reset() {window.clear();queue.clear();announced=false;leftAt=null;nextSend=0}
    private fun shaft()=S.isEnabled(ModuleIds.VANGUARD_ANNOUNCE) && RngAnnounce.hypixel() &&
        FeatureRuntime.context.skyblock && FeatureRuntime.context.area.contains("mineshaft",true)
    fun status():String=when {window.active(now())->"Accepting guild replies: ${window.count}/3 slots reserved (60-second window)";announced->"Invitation window closed or shaft left";else->"Waiting for a Vanguard Mineshaft"}
    fun tick(c:Minecraft) {
        if(!S.isEnabled(ModuleIds.VANGUARD_ANNOUNCE) || c.player==null || c.level==null || !RngAnnounce.hypixel()) {reset();return}
        val time=now()
        if(!shaft()) {
            // Stop invites immediately, but don't reannounce because TAB flickered for one tick.
            window.clear();queue.clear()
            if(leftAt==null)leftAt=time
            if(time-(leftAt ?: time)>=2000)announced=false
            return
        }
        leftAt=null
        val ctx=FeatureRuntime.context
        val tab=c.connection?.listedOnlinePlayers?.mapNotNull {it.tabListDisplayName?.string}.orEmpty()
        val vanguard=ctx.mineshaftVariant?.startsWith("FAIR_",true)==true || (MineshaftLogic.corpses(tab)[CorpseKind.VANGUARD] ?: 0)>0
        if(vanguard && !announced) {
            val s=S.currentGeneral().features.vanguardAnnounce;s.validate()
            c.player!!.connection.sendCommand("gc ${s.announcement()}")
            keyword=s.keyword;window.begin(time);announced=true;nextSend=time+1000
        }
        if(!window.active(time)) {queue.clear();return}
        if(time>=nextSend && queue.isNotEmpty()) {
            c.player!!.connection.sendCommand("p ${queue.remove()}")
            nextSend=time+1000
        }
    }
    private fun receive(raw:String,overlay:Boolean) {
        if(overlay || !shaft() || raw.length>512)return
        val c=Minecraft.getInstance();val player=c.player ?: return
        val name=window.accept(raw,player.gameProfile.name,keyword,now()) ?: return
        queue.add(name) // Bounded by the three irrevocably reserved unique slots.
    }
    fun register() {
        ClientReceiveMessageEvents.GAME.register {message,overlay->receive(message.string,overlay)}
        ClientReceiveMessageEvents.GAME_CANCELED.register {message,overlay->receive(message.string,overlay)}
    }
}
