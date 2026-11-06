/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatCommands {
    private val echoes=ChatReplyEchoes()
    private fun receive(raw:String,overlay:Boolean) {
        if(overlay || !S.isEnabled(S.CHAT_COMMANDS)) return
        val client=Minecraft.getInstance();val player=client.player ?: return
        val address=client.currentServer?.ip?.substringBefore(':')?.lowercase() ?: return
        if(address!="hypixel.net" && !address.endsWith(".hypixel.net")) return
        val chat=ChatCommandsLogic.parse(raw) ?: return
        if(chat.name.equals(player.gameProfile.name,true) && echoes.consume(chat.channel,chat.text,System.nanoTime()/1_000_000L)) return
        val config=S.currentGeneral().features.chatCommands
        if(!ChatCommandsLogic.allowed(config,chat.channel)) return
        val suggestion=chat.text.removePrefix(config.prefix).takeIf {chat.text.startsWith(config.prefix)}
            ?.let {Regex("(?i)^suggest\\s+(.+)$").matchEntire(it)?.groupValues?.get(1)?.trim()}
        val isSuggestion=suggestion!=null && S.currentGeneral().features.music.suggest
        val response=if(isSuggestion) null else ChatCommandsLogic.response(config,chat) ?: return
        fun send(text:String) {
            if(text.startsWith(S.currentGeneral().features.chatCommands.prefix)) echoes.remember(chat.channel,text,System.nanoTime()/1_000_000L)
            when(chat.channel) {
            "party"->player.connection.sendCommand("pc $text")
            "guild"->player.connection.sendCommand("gc $text")
            "officer"->player.connection.sendCommand("oc $text")
            "coop"->player.connection.sendCommand("cc $text")
            "private"->player.connection.sendCommand("msg ${chat.name} $text")
            "public"->player.connection.sendCommand("achat $text")
        }}
        if(!isSuggestion) {send(response!!);return}
        val world=client.level
        val accepted=YandexMusic.suggest(suggestion) {success,message->client.execute {
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
