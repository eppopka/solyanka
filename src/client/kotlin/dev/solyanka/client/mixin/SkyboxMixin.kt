/* SPDX-License-Identifier: GPL-3.0-only; Stray SkyRendererMixin CC0 adaptation. */
package dev.solyanka.client.mixin
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderTarget
import dev.solyanka.client.feature.SkyboxVisual
import dev.solyanka.utility.SkyboxLogic
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.DynamicUniforms
import net.minecraft.client.renderer.SkyRenderer
import net.minecraft.client.renderer.state.level.SkyRenderState
import net.minecraft.world.level.dimension.DimensionType
import org.joml.Matrix4f
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(SkyRenderer::class)
abstract class SkyboxMixin {
 @Shadow @Final private lateinit var renderTarget:RenderTarget
 @Unique private var solyankaSkyTime=0.0
 @Inject(method=["extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V"],at=[At("RETURN")])
 private fun solyankaExtract(level:ClientLevel,partial:Float,camera:Camera,state:SkyRenderState,ci:CallbackInfo) {
  solyankaSkyTime=level.gameTime.toDouble()+partial
  if(!SkyboxVisual.enabled()) return
  val s=SkyboxVisual.settings()
  if(SkyboxLogic.forceEnd(true,s,state.skybox==DimensionType.Skybox.NONE)) state.skybox=DimensionType.Skybox.END
  state.skyColor=SkyboxLogic.tint(state.skyColor,s);state.sunriseAndSunsetColor=SkyboxLogic.tint(state.sunriseAndSunsetColor,s)
 }
 @WrapOperation(method=["renderEndSky()V"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")])
 private fun solyankaEndTint(uniforms:DynamicUniforms,matrix:Matrix4f,original:Operation<GpuBufferSlice>):GpuBufferSlice =
  if(SkyboxVisual.enabled() && SkyboxVisual.settings().tint) uniforms.writeTransform(matrix,SkyboxVisual.tintVector()) else original.call(uniforms,matrix)
 @Inject(method=["renderEndSky()V"],at=[At("RETURN")])
 private fun solyankaDecor(ci:CallbackInfo) {SkyboxVisual.render(renderTarget,solyankaSkyTime)}
 @Inject(method=["close()V"],at=[At("RETURN")])
 private fun solyankaClose(ci:CallbackInfo) {SkyboxVisual.close()}
}
