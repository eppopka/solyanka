/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.fog.FogRenderer::class)
abstract class SuppressFogMixin {
 private var solyankaAtmospheric=false
 @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method=["setupFog"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V")])
 private fun solyankaEnvironment(e:net.minecraft.client.renderer.fog.environment.FogEnvironment,f:net.minecraft.client.renderer.fog.FogData,c:net.minecraft.client.Camera,l:net.minecraft.client.multiplayer.ClientLevel,d:Float,t:net.minecraft.client.DeltaTracker,o:com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void>) { solyankaAtmospheric=e is net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;o.call(e,f,c,l,d,t) }
 @Inject(method=["setupFog"],at=[At("RETURN")])
 private fun solyankaFog(camera:net.minecraft.client.Camera,distance:Int,tracker:net.minecraft.client.DeltaTracker,darken:Float,level:net.minecraft.client.multiplayer.ClientLevel,ci:CallbackInfoReturnable<net.minecraft.client.renderer.fog.FogData>) { if(solyankaAtmospheric && Suppression.on { fog }) ci.returnValue.let { it.renderDistanceStart=Float.MAX_VALUE;it.renderDistanceEnd=Float.MAX_VALUE;it.environmentalStart=Float.MAX_VALUE;it.environmentalEnd=Float.MAX_VALUE };dev.solyanka.client.feature.Environment.fog(ci.returnValue,distance,solyankaAtmospheric) }

}
