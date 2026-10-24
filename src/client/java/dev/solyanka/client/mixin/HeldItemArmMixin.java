/* SPDX-License-Identifier: GPL-3.0-only; Stray AvatarRendererMixin CC0 arm hook adaptation. */
package dev.solyanka.client.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.solyanka.client.feature.HeldItemCosmetic;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(AvatarRenderer.class)
public abstract class HeldItemArmMixin {
 @ModifyExpressionValue(method="renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/geom/ModelPart;Z)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
 private RenderType solyanka$handShader(RenderType original,PoseStack pose,SubmitNodeCollector collector,int light,Identifier skin,ModelPart part,boolean sleeve) {
  var model=((AvatarRenderer<?>)(Object)this).getModel();
  if(part!=model.rightArm && part!=model.leftArm)return original;
  return HeldItemCosmetic.wrapArm(original,skin,part==model.rightArm);
 }
}
