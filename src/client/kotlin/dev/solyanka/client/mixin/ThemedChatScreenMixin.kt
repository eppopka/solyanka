/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.ThemedChat
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
@Mixin(ChatScreen::class)
abstract class ThemedChatScreenMixin {
 @WrapOperation(method=["extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"],at=[At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V")])
 private fun solyankaInput(g:GuiGraphicsExtractor,x0:Int,y0:Int,x1:Int,y1:Int,color:Int,original:Operation<Void>) {
  if(ThemedChat.enabled()) ThemedChat.input(g,x0,y0,x1,y1,color) else original.call(g,x0,y0,x1,y1,color)
 }
}
