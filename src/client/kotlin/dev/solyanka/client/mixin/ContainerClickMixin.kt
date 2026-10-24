/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills AbstractContainerScreenMixin; see NOTICE. */
package dev.solyanka.client.mixin

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.wrapoperation.*
import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.*
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.At

@Mixin(AbstractContainerScreen::class)
abstract class ContainerClickMixin {
    @Shadow private var doubleclick:Boolean=false
    @ModifyExpressionValue(method=["mouseClicked"],at=[At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z")])
    private fun solyankaMiddleFix(original:Boolean)=original || SolyankaClient.isEnabled(SolyankaClient.MIDDLE_FIX)
    @WrapOperation(method=["mouseClicked"],at=[At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",ordinal=1)])
    private fun solyankaMiddleOverride(screen:AbstractContainerScreen<*>,slot:Slot?,slotId:Int,button:Int,action:ContainerInput,original:Operation<Void>) {
        if(FeatureRuntime.middleOverride(slot,button,action)) { doubleclick=false;original.call(screen,slot,slotId,2,ContainerInput.CLONE) }
        else original.call(screen,slot,slotId,button,action)
    }
}
