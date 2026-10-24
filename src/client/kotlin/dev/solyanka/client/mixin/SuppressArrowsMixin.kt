/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.entity.layers.StuckInBodyLayer::class)
abstract class SuppressArrowsMixin {
 @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private lateinit var model:net.minecraft.client.model.Model<*>
 @Inject(method=["submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V"],at=[At("HEAD")],cancellable=true)
 private fun solyankaArrows(ci:CallbackInfo) { if(Suppression.on { stuckArrows } && model is net.minecraft.client.model.`object`.projectile.ArrowModel) ci.cancel() }

}
