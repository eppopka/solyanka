/* SPDX-License-Identifier: GPL-3.0-only; Stray ItemFeatureRendererMixin CC0 adaptation. */
package dev.solyanka.client.mixin
import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import dev.solyanka.client.feature.HeldItemCosmetic
import net.minecraft.client.renderer.feature.ItemFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
@Mixin(ItemFeatureRenderer::class)
abstract class HeldItemFeatureMixin {
 @ModifyExpressionValue(method=["prepareMainSubmit(Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer\$Submit;)V"],at=[At(value="INVOKE",target="Lnet/minecraft/client/resources/model/geometry/BakedQuad\$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;")])
 private fun solyankaType(original:RenderType,submit:ItemFeatureRenderer.Submit):RenderType=HeldItemCosmetic.wrap(original,submit)
}
