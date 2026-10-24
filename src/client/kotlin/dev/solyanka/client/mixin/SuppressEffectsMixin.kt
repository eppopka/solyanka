/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.gui.screens.inventory.EffectsInInventory::class)
abstract class SuppressEffectsMixin {
 @Inject(method=["extractRenderState"],at=[At("HEAD")],cancellable=true)
 private fun solyankaEffects(ci:CallbackInfo) { if(Suppression.on { effectDisplay }) ci.cancel() }

}
