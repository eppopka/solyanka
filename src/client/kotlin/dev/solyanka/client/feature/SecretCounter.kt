/* SPDX-License-Identifier: GPL-3.0-only; adapted from user-provided SecretCounter.kt. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object SecretCounter {
    private val state=SecretCounterState()
    private val announcement=SecretAnnouncement()
    private var scope:Pair<Int?,Boolean>?=null
    private fun now()=System.nanoTime()/1_000_000L
    fun reset() {state.reset();announcement.reset();scope=null}
    private fun active():Boolean {
        val c=Minecraft.getInstance();val ctx=FeatureRuntime.context
        if(!S.isEnabled(S.SECRET_COUNTER) || c.player==null || c.level==null || !ctx.dungeon) {reset();return false}
        val next=ctx.floor to ctx.master
        if(scope!=next) {reset();scope=next}
        return true
    }
    private fun local(text:String) {Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[solyanka] $text"))}
    private fun message(text:String,overlay:Boolean) {
        if(overlay || !active()) return
        val now=now()
        if(DungeonRunMessages.started(text)) {state.reset();announcement.reset();return}
        if(DungeonRunMessages.ended(text)) state.finish(now)
        state.readSecrets(text,now)?.let {count->
            local("Secrets Found: $count")
            announcement.schedule(now,count,S.currentGeneral().features.secretCounter.autoAnnounce)
        }
    }
    fun tick() {
        if(!active()) return
        val now=now();val connection=Minecraft.getInstance().player?.connection ?: return
        when(state.poll(now)) {
            SecretCounterState.Action.REQUEST_STATS->connection.sendCommand("showextrastats")
            SecretCounterState.Action.TIMEOUT->local("Secret Counter: no Secrets Found result received; announcement skipped.")
            null->Unit
        }
        val s=S.currentGeneral().features.secretCounter
        announcement.poll(now,s.autoAnnounce)?.let {count->
            val text="[solyanka] I did $count secrets!"
            if(s.channel=="party") connection.sendCommand("pc $text") else connection.sendChat(text)
        }
    }
    fun register() {
        // Mutually exclusive Fabric paths: still observe hidden server stats, but never player CHAT events.
        ClientReceiveMessageEvents.GAME.register {m,overlay->message(m.string,overlay)}
        ClientReceiveMessageEvents.GAME_CANCELED.register {m,overlay->message(m.string,overlay)}
    }
}
