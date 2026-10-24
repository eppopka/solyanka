/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills GuiMixin.onClearChat; disconnect moved to Hud in 26.2. */
package dev.solyanka.client.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import dev.solyanka.client.SolyankaClient
import net.minecraft.client.gui.Hud
import net.minecraft.client.gui.components.ChatComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(Hud::class)
abstract class HudMixin {
    @WrapOperation(method=["onDisconnected"],at=[At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V")])
    private fun solyankaKeepHistory(chat:ChatComponent,history:Boolean,original:Operation<Void>) {
        if(!SolyankaClient.keepChatHistory()) original.call(chat,history)
    }
}
