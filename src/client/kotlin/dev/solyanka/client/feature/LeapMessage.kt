/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object LeapMessage {
    private val store by lazy { LeapCountStore(FabricLoader.getInstance().configDir.resolve("solyanka-leap-counts.json")).also { it.load() } }
    private fun message(raw:String,overlay:Boolean) {
        if(overlay || !S.isEnabled(S.LEAP_MESSAGE) || !FeatureRuntime.context.dungeon) return
        val name=LeapMessageLogic.target(raw) ?: return
        val client=Minecraft.getInstance();val player=client.player ?: return
        val settings=S.currentGeneral().features.leapMessage
        val count=if(settings.countLeaps) try { store.increment(name) } catch(e:Exception) {
            player.sendSystemMessage(Component.literal("[solyanka] Leap count not saved; announcement skipped: ${e.message}"));return
        } else null
        val text=LeapMessageLogic.format(settings.message,name,count)
        if(text.isNotBlank() && text.length<=200) player.connection.sendCommand("pc $text")
    }
    fun register() {
        ClientReceiveMessageEvents.GAME.register { m,o -> message(m.string,o) }
        ClientReceiveMessageEvents.GAME_CANCELED.register { m,o -> message(m.string,o) }
    }
}
