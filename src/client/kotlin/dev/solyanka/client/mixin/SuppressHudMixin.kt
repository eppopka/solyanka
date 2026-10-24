/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.gui.Hud::class)
abstract class SuppressHudMixin {
 @Inject(method=["extractBossOverlay"],at=[At("HEAD")],cancellable=true)
 private fun solyankaBoss(ci:CallbackInfo) { if(Suppression.on { bossBar }) ci.cancel() }
 @Inject(method=["extractEffects"],at=[At("HEAD")],cancellable=true)
 private fun solyankaEffects(ci:CallbackInfo) { if(Suppression.on { effectDisplay }) ci.cancel() }
 @Inject(method=["extractSelectedItemName"],at=[At("HEAD")],cancellable=true)
 private fun solyankaItem(ci:CallbackInfo) { if(Suppression.on { selectedItemName }) ci.cancel() }
 @Inject(method=["extractConfusionOverlay"],at=[At("HEAD")],cancellable=true)
 private fun solyankaConfusion(ci:CallbackInfo) { if(Suppression.on { nausea }) ci.cancel() }
 @Inject(method=["extractVignette"],at=[At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V")],cancellable=true)
 private fun solyankaVignette(ci:CallbackInfo,@com.llamalad7.mixinextras.sugar.Local(ordinal=0) border:Float) { if(Suppression.on { vignette==3 || vignette==1 && border<=0f || vignette==2 && border>0f }) ci.cancel() }

}
