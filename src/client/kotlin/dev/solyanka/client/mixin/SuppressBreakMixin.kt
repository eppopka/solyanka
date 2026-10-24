/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.multiplayer.ClientLevel::class)
abstract class SuppressBreakMixin {
 @Inject(method=["addBreakingBlockEffect","addDestroyBlockEffect"],at=[At("HEAD")],cancellable=true,require=2)
 private fun solyankaBreak(ci:CallbackInfo) { if(Suppression.on { breakParticles }) ci.cancel() }

}
