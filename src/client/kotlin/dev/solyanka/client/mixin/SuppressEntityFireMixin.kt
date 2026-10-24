/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.entity.EntityRenderer::class)
abstract class SuppressEntityFireMixin {
 @Inject(method=["extractRenderState"],at=[At("TAIL")])
 private fun solyankaFire(e:net.minecraft.world.entity.Entity,s:net.minecraft.client.renderer.entity.state.EntityRenderState,delta:Float,ci:CallbackInfo) { if(Suppression.on { entityFire }) s.displayFireAnimation=false }

 @Inject(method=["extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"],at=[At("HEAD")],cancellable=true)
 private fun solyankaDeadName(e:net.minecraft.world.entity.Entity,s:net.minecraft.client.renderer.entity.state.EntityRenderState,d:Float,ci:CallbackInfo) {
  if(Suppression.deadName(e.name.string)) {s.nameTag=null;s.nameTagAttachment=null;ci.cancel()}
 }
}
