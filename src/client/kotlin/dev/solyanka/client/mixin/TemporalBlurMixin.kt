/* SPDX-License-Identifier: GPL-3.0-only; Stray GameRendererMixin CC0 deferred temporal hook. */
package dev.solyanka.client.mixin
import com.mojang.blaze3d.pipeline.RenderTarget
import dev.solyanka.client.feature.TemporalBlur
import dev.solyanka.client.feature.HeldItemCosmetic
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.GameRenderer
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(GameRenderer::class)
abstract class TemporalBlurMixin {
 @Inject(method=["renderLevel(Lnet/minecraft/client/DeltaTracker;)V"],at=[At("HEAD")])
 private fun solyankaHeldFrame(delta:DeltaTracker,ci:CallbackInfo) {HeldItemCosmetic.prepareFrame()}
 @Shadow @Final private lateinit var mainRenderTarget:RenderTarget
 @Inject(method=["renderLevel(Lnet/minecraft/client/DeltaTracker;)V"],at=[At("RETURN")])
 private fun solyankaBlur(delta:DeltaTracker,ci:CallbackInfo) {TemporalBlur.apply(mainRenderTarget)}
 @Inject(method=["close()V","resize(II)V","resetData()V"],at=[At("RETURN")])
 private fun solyankaReset(ci:CallbackInfo) {TemporalBlur.close();HeldItemCosmetic.close()}
}
