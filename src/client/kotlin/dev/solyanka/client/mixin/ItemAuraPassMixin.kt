/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.ItemAura
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(GameRenderer::class)
abstract class ItemAuraPassMixin {
 @Shadow @Final private lateinit var featureRenderDispatcher:FeatureRenderDispatcher
 @Inject(method=["renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"],at=[At("HEAD")])
 private fun solyankaAuraBegin(ci:CallbackInfo) {ItemAura.begin()}
 @Inject(method=["renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",shift=At.Shift.AFTER)])
 private fun solyankaAuraFlush(ci:CallbackInfo) {ItemAura.flush(featureRenderDispatcher)}
}
