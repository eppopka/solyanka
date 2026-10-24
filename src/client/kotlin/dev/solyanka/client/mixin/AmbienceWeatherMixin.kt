/* SPDX-License-Identifier: GPL-3.0-only; adapted from Stray CC0 */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Environment
import dev.solyanka.utility.EnvironmentLogic
import dev.solyanka.utility.AlwaysWaterPolicy
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
@Mixin(net.minecraft.world.level.Level::class)
abstract class AmbienceWeatherMixin {
 @Inject(method=["getRainLevel"],at=[At("RETURN")],cancellable=true)
 private fun solyankaRain(delta:Float,ci:CallbackInfoReturnable<Float>) {if((this as Any as net.minecraft.world.level.Level).isClientSide) {if(Environment.alwaysWater()) ci.returnValue=AlwaysWaterPolicy.rain(true,ci.returnValue) else if(Environment.ambience()) ci.returnValue=EnvironmentLogic.rain(Environment.settings().weather,ci.returnValue)}}
 @Inject(method=["getThunderLevel"],at=[At("RETURN")],cancellable=true)
 private fun solyankaThunder(delta:Float,ci:CallbackInfoReturnable<Float>) {if((this as Any as net.minecraft.world.level.Level).isClientSide && Environment.ambience()) ci.returnValue=EnvironmentLogic.thunder(Environment.settings().weather,ci.returnValue)}
}
