/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from WhatYouThing/NoFrills ChatTweaks; see NOTICE. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.mixin.ChatComponentAccessor
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import kotlin.math.floor

object ChatTweaks {
    fun copy(client:Minecraft,device:InputDevice,key:Int):Boolean {
        if(!SolyankaClient.isEnabled(SolyankaClient.CHAT) || client.gui.screen() !is ChatScreen || !client.isWindowActive) return false
        val s=SolyankaClient.currentGeneral().chat
        val single=s.copyLine.matches(device,key)
        if(!single && !s.copy.matches(device,key)) return false
        val chat=client.gui.hud.chat
        val access=chat as ChatComponentAccessor
        val lines=access.solyankaLines()
        val index=ChatLogic.hoveredIndex(client.mouseHandler.getScaledXPos(client.window),client.mouseHandler.getScaledYPos(client.window),
            client.window.guiScaledHeight,ChatComponent.getWidth(client.options.chatWidth().get()),client.options.chatScale().get(),
            floor(9*(client.options.chatLineSpacing().get()+1)).toInt(),access.solyankaScroll(),lines.size,chat.linesPerPage) ?: return false
        val line=lines[index]
        val raw=if(single) buildString { line.content().accept { _,_,codePoint -> appendCodePoint(codePoint);true } } else line.parent().content().string
        val text=ChatLogic.copied(raw,s.trimOnCopy)
        if(text.isEmpty()) return false
        client.keyboardHandler.clipboard=text
        if(s.messageOnCopy) chat.addClientSystemMessage(Component.literal("[solyanka] "+ChatLogic.feedback(text,single,s.feedbackLimit)))
        return true
    }
}
