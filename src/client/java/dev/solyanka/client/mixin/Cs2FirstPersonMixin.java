/* Stray CC0 adaptation to MC 26.2; see NOTICE. */
package dev.solyanka.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.solyanka.client.cs2.Cs2Knives;
import dev.solyanka.client.cs2.Cs2Viewmodel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CS2 knives: replaces the first-person main hand (arm + item) with the CS2 viewmodel. */
@Mixin(ItemInHandRenderer.class)
public class Cs2FirstPersonMixin {
	@Inject(method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
	private void solyanka$cs2Viewmodel(
		AbstractClientPlayer player,
		float tickDelta,
		float pitch,
		InteractionHand hand,
		float swingProgress,
		ItemStack itemStack,
		float equippedProgress,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int light,
		CallbackInfo ci
	) {
		if (hand != InteractionHand.MAIN_HAND || !Cs2Knives.applies(itemStack) || player.isScoping()) {
			return;
		}
		// Replay the draw only when the slot or item type changes, not when the server resends the stack.
		Object equipKey = player.getInventory().getSelectedSlot() + ":" + itemStack.getItem();
		if (Cs2Viewmodel.render(player, player.getMainArm(), swingProgress, equipKey, poseStack, collector, light)) {
			ci.cancel();
		}
	}
}
