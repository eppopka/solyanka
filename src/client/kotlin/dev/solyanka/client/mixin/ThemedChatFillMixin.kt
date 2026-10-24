/* SPDX-License-Identifier: GPL-3.0-only; Stray ChatComponentFillMixin CC0 adaptation. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.ThemedChat
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(targets=["net.minecraft.client.gui.components.ChatComponent\$DrawingBackgroundGraphicsAccess","net.minecraft.client.gui.components.ChatComponent\$DrawingFocusedGraphicsAccess"])
abstract class ThemedChatFillMixin {
 @Inject(method=["fill(IIIII)V"],at=[At("HEAD")],cancellable=true)
 private fun solyankaFill(x0:Int,y0:Int,x1:Int,y1:Int,color:Int,ci:CallbackInfo) {if(ThemedChat.skipFill(x0,x1)) ci.cancel()}
}
