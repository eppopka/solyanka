/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.ScreenEffectRenderer::class)
abstract class SuppressFireOverlayMixin {
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method=["submit"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V")])
 private fun solyankaFire(p:com.mojang.blaze3d.vertex.PoseStack,c:net.minecraft.client.renderer.SubmitNodeCollector,s:net.minecraft.client.renderer.texture.TextureAtlasSprite,original:com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void>) { if(!Suppression.on { fireOverlay }) original.call(p,c,s) }

}
