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
}
