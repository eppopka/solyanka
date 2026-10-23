/* SPDX-License-Identifier: GPL-3.0-only
 * Input-hook behavior reference: WhatYouThing/NoFrills; see NOTICE. */
package dev.solyanka.client.mixin

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.InputDevice
import net.minecraft.client.MouseHandler
import net.minecraft.client.input.MouseButtonInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MouseHandler::class)
abstract class MouseHandlerMixin {
    @Inject(method = ["onButton"], at = [At("HEAD")], cancellable = true)
    private fun solyankaInput(window: Long, event: MouseButtonInfo, action: Int, ci: CallbackInfo) {
        if (SolyankaClient.handleInput(window, action, InputDevice.MOUSE, event.button(), event.modifiers())) ci.cancel()
    }
}
