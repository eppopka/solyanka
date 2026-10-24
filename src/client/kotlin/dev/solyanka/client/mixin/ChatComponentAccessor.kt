/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin

import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ChatComponent::class)
interface ChatComponentAccessor {
    @Accessor("trimmedMessages") fun solyankaLines():List<GuiMessage.Line>
    @Accessor("chatScrollbarPos") fun solyankaScroll():Int
}
