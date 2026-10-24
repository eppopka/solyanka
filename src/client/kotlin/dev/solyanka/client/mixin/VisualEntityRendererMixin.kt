/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.*
import net.minecraft.world.entity.Entity
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import com.mojang.blaze3d.vertex.PoseStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*
@Mixin(EntityRenderer::class)
abstract class VisualEntityRendererMixin {
 @Inject(method=["extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"],at=[At("TAIL")])
 private fun solyankaState(e:Entity,state:EntityRenderState,partial:Float,ci:CallbackInfo) {
  (state as VisualEntityState).solyankaHideName=WorldVisuals.hideName(e)
  if(!e.isCurrentlyGlowing) {
   if(HuntingFeatures.glow(e)) state.outlineColor=HuntingFeatures.glowColor()
   else if(WorldVisuals.glow(e)) state.outlineColor=WorldVisuals.glowColor()
  }
 }
 @Inject(method=["affectedByCulling"],at=[At("HEAD")],cancellable=true)
 private fun solyankaCull(e:Entity,ci:CallbackInfoReturnable<Boolean>) {if(WorldVisuals.throughGlow(e) || HuntingFeatures.glow(e)) ci.returnValue=false}
 @Inject(method=["submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V"],at=[At("HEAD")],cancellable=true)
 private fun solyankaName(state:EntityRenderState,pose:PoseStack,collector:SubmitNodeCollector,camera:CameraRenderState,light:Int,ci:CallbackInfo) {if((state as VisualEntityState).solyankaHideName) ci.cancel()}
}
