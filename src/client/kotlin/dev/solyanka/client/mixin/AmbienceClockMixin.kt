/* SPDX-License-Identifier: GPL-3.0-only; adapted from Stray CC0 */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Environment
import dev.solyanka.utility.EnvironmentLogic
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
@Mixin(net.minecraft.client.ClientClockManager::class)
abstract class AmbienceClockMixin {
 @Inject(method=["getTotalTicks"],at=[At("HEAD")],cancellable=true)
 private fun solyankaTime(clock:net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock>,ci:CallbackInfoReturnable<Long>) {
  if(Environment.ambience() && clock.unwrapKey().orElse(null)==net.minecraft.world.clock.WorldClocks.OVERWORLD) EnvironmentLogic.time(Environment.settings().clock)?.let {ci.returnValue=it}
 }
}
