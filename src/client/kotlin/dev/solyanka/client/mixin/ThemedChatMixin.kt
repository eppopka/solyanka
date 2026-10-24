/* SPDX-License-Identifier: GPL-3.0-only; Stray ChatChrome CC0 adaptation. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.ThemedChat
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Font
import org.spongepowered.asm.mixin.Mixin
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
@Mixin(ChatComponent::class)
abstract class ThemedChatMixin {
 @WrapMethod(method=["extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent\$DisplayMode;Z)V"])
 private fun solyankaPane(g:GuiGraphicsExtractor,font:Font,ticks:Int,mx:Int,my:Int,mode:ChatComponent.DisplayMode,click:Boolean,original:Operation<Void>) {
  val previous=ThemedChat.begin(g,this as Any as ChatComponent,ticks,mode)
  try {original.call(g,font,ticks,mx,my,mode,click)} finally {ThemedChat.end(previous)}
 }
}
