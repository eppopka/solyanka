/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills ChatComponentMixin; WhatYouThing and contributors. */
package dev.solyanka.client.mixin

import dev.solyanka.client.SolyankaClient
import net.minecraft.client.gui.components.ChatComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.Constant
import org.spongepowered.asm.mixin.injection.ModifyConstant

@Mixin(ChatComponent::class)
abstract class ChatComponentMixin {
    @ModifyConstant(method=["addMessageToQueue","addMessageToDisplayQueue"],constant=[Constant(intValue=100)],require=2)
    private fun solyankaHistoryLimit(original:Int):Int = SolyankaClient.chatLimit(original)
}
