/* SPDX-License-Identifier: GPL-3.0-only
 * Input-hook behavior reference: WhatYouThing/NoFrills; see NOTICE. */
package dev.solyanka.client.mixin

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.InputDevice
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.input.KeyEvent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(KeyboardHandler::class)
abstract class KeyboardHandlerMixin {
    @Inject(method = ["keyPress"], at = [At("HEAD")], cancellable = true)
    private fun solyankaInput(window: Long, action: Int, event: KeyEvent, ci: CallbackInfo) {
        if (SolyankaClient.handleInput(window, action, InputDevice.KEYBOARD, event.key(), event.modifiers())) ci.cancel()
    }
}
