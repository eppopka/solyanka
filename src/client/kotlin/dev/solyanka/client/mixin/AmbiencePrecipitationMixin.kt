/* SPDX-License-Identifier: GPL-3.0-only; adapted from Stray CC0 */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Environment
import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
@Mixin(net.minecraft.client.renderer.WeatherEffectRenderer::class)
abstract class AmbiencePrecipitationMixin {
 @ModifyExpressionValue(method=["extractRenderState"],at=[At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome\$Precipitation;")])
 private fun solyankaSnow(original:net.minecraft.world.level.biome.Biome.Precipitation)=if(Environment.ambience() && Environment.settings().weather==4) net.minecraft.world.level.biome.Biome.Precipitation.SNOW else original
 @ModifyExpressionValue(method=["render"],at=[At(value="FIELD",target="Lnet/minecraft/client/renderer/state/level/WeatherRenderState;intensity:F")],require=2)
 private fun solyankaGradient(original:Float)=if(Environment.ambience() && Environment.settings().precipitation && original!=0f) Environment.settings().gradient.toFloat() else original
}
