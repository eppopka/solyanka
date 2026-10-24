/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.GameRenderer::class)
abstract class SuppressNauseaMixin {
 @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method=["renderLevel"],at=[At(value="FIELD",target="Lnet/minecraft/client/player/LocalPlayer;oPortalEffectIntensity:F"),At(value="FIELD",target="Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"),At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F")],require=3)
 private fun solyankaNausea(original:Float)=if(Suppression.on { nausea }) 0f else original

}
