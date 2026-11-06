/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatCommands {
    private var lastReply=0L
    private val users=linkedMapOf<String,Long>()
    private fun receive(raw:String,overlay:Boolean) {
        if(overlay || !S.isEnabled(S.CHAT_COMMANDS)) return
        val client=Minecraft.getInstance();val player=client.player ?: return
        val address=client.currentServer?.ip?.substringBefore(':')?.lowercase() ?: return
        if(address!="hypixel.net" && !address.endsWith(".hypixel.net")) return
        val chat=ChatCommandsLogic.parse(raw) ?: return
        if(chat.name.equals(player.gameProfile.name,true)) return
        val config=S.currentGeneral().features.chatCommands
        if(!ChatCommandsLogic.allowed(config,chat.channel)) return
        val suggestion=chat.text.removePrefix(config.prefix).takeIf {chat.text.startsWith(config.prefix)}
            ?.let {Regex("(?i)^suggest\\s+(.+)$").matchEntire(it)?.groupValues?.get(1)?.trim()}
        val isSuggestion=suggestion!=null && S.currentGeneral().features.music.suggest
        val response=if(isSuggestion) null else ChatCommandsLogic.response(config,chat) ?: return
        val now=System.nanoTime()/1_000_000L;val key=chat.name.lowercase()
        if(now-lastReply<5000 || now-(users[key] ?: 0L)<10000) return
        lastReply=now;users[key]=now
        if(users.size>512) users.remove(users.keys.first())
        fun send(text:String) {when(chat.channel) {
            "party"->player.connection.sendCommand("pc $text")
            "guild"->player.connection.sendCommand("gc $text")
            "officer"->player.connection.sendCommand("oc $text")
            "coop"->player.connection.sendCommand("cc $text")
            "private"->player.connection.sendCommand("msg ${chat.name} $text")
            "public"->player.connection.sendCommand("achat $text")
        }}
        if(!isSuggestion) {send(response!!);return}
        val world=client.level
        val accepted=YandexMusic.suggest(suggestion!!) {success,message->client.execute {
            if(client.level!==world || client.player!==player || !S.isEnabled(S.CHAT_COMMANDS)) return@execute
            if(success && S.currentGeneral().features.music.announce) {
                val safe=message.filterNot {it.isISOControl() || it=='§'}.take(120)
                send("Added to queue: $safe")
            } else if(!success) player.sendSystemMessage(Component.literal("[solyanka] $message"))
        }}
        if(!accepted) player.sendSystemMessage(Component.literal("[solyanka] Yandex Music is disconnected or the request queue is full."))
    }
    fun register() {
        ClientReceiveMessageEvents.GAME.register {m,o->receive(m.string,o)}
        ClientReceiveMessageEvents.GAME_CANCELED.register {m,o->receive(m.string,o)}
    }
}
