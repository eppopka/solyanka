/* SPDX-License-Identifier: GPL-3.0-only; user-provided impl.zip RotatingItem adaptation. */
package dev.solyanka.client.mixin

import com.mojang.blaze3d.vertex.PoseStack
import dev.solyanka.client.feature.ArchiveVisuals
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.world.entity.HumanoidArm
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ItemInHandRenderer::class)
abstract class RotatingItemMixin {
    @Inject(method=["applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"],at=[At("RETURN")])
    private fun solyankaRotateItem(pose:PoseStack,arm:HumanoidArm,equipProgress:Float,ci:CallbackInfo) {
        ArchiveVisuals.transform(pose,arm)
    }
    @Inject(method=["submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"],at=[At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",shift=At.Shift.BEFORE)])
    private fun solyankaCaptureAura(player:net.minecraft.client.player.AbstractClientPlayer,delta:Float,pitch:Float,hand:net.minecraft.world.InteractionHand,swing:Float,stack:net.minecraft.world.item.ItemStack,equip:Float,pose:PoseStack,collector:net.minecraft.client.renderer.SubmitNodeCollector,light:Int,ci:CallbackInfo) {
        dev.solyanka.client.feature.ItemAura.captureHand(pose,hand,stack)
    }
}
