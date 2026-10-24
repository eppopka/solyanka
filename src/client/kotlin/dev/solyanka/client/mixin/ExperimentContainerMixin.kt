/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Experiments
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(AbstractContainerScreen::class)
abstract class ExperimentContainerMixin {
 @Inject(method=["extractSlot"],at=[At("TAIL")])
 private fun solyankaExperimentOverlay(g:GuiGraphicsExtractor,slot:Slot,mouseX:Int,mouseY:Int,ci:CallbackInfo) { Experiments.overlay(g,slot,slot.x,slot.y) }
 @Inject(method=["slotClicked"],at=[At("HEAD")],cancellable=true)
 private fun solyankaProtect(slot:Slot?,id:Int,button:Int,action:ContainerInput,ci:CallbackInfo) { if(Experiments.blocked(slot,action)) ci.cancel() else Experiments.click(slot,action) }
 @Inject(method=["extractTooltip"],at=[At("HEAD")],cancellable=true)
 private fun solyankaEmptyTooltip(g:GuiGraphicsExtractor,x:Int,y:Int,ci:CallbackInfo) {
  val self=this as Any as AbstractContainerScreen<*>
  if(Experiments.hideTooltip((self as ContainerAccess).solyankaHovered(),self.title.string)) ci.cancel()
 }
}
