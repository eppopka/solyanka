/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen::class)
abstract class SuppressRecipeMixin {
 @Inject(method=["initButton"],at=[At("HEAD")],cancellable=true)
 private fun solyankaBook(ci:CallbackInfo) { if(Suppression.on { recipeBook }) ci.cancel() }

}
