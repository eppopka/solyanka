/* SPDX-License-Identifier: GPL-3.0-only
 * Stray EndSkyDecor CC0 port: native 26.2 bind groups, target ownership and GPU cleanup. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.platform.*
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Vector4f
import org.slf4j.LoggerFactory
import java.util.Optional
import java.util.OptionalDouble
object SkyboxVisual {
 private val log=LoggerFactory.getLogger("solyanka/skybox")
 private val faces=List(6) {id("face_$it")};private val holes=List(8) {id("hole_$it")}
 private fun id(name:String)=Identifier.fromNamespaceAndPath("solyanka","textures/sky/end/$name.png")
 private val owned=linkedSetOf<Identifier>()
 private var quads:GpuBuffer?=null
 private var failed=false
 @Volatile private var cleanupRequested=false
 private val pipelines by lazy {listOf(pipeline("skybox",BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA),pipeline("skybox_add",BlendFunction(BlendFactor.ONE,BlendFactor.ONE)))}
 private fun pipeline(name:String,blend:BlendFunction):RenderPipeline = RenderPipelines.register(RenderPipeline.builder()
  .withLocation(Identifier.fromNamespaceAndPath("solyanka",name)).withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
  .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER).withUniform("Projection",UniformType.UNIFORM_BUFFER).withSampler("Sampler0").build())
  .withColorTargetState(ColorTargetState(blend)).withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS,false)).withCull(false)
  .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build())
 fun settings()=S.currentGeneral().features.skybox
 fun enabled()=S.isEnabled(S.SKYBOX)
 fun retry() {failed=false}
 fun requestCleanup() {cleanupRequested=true}
 fun tick() {if(cleanupRequested && RenderSystem.isOnRenderThread()) close()}
 fun tintVector():Vector4f {
  val c=SkyboxLogic.tint(-1,settings());return Vector4f(((c ushr 16) and 255)/255f,((c ushr 8) and 255)/255f,(c and 255)/255f,1f)
 }
 fun render(target:RenderTarget,time:Double) {
  if(!SkyboxLogic.decorated(enabled(),settings()) || failed || !RenderSystem.isOnRenderThread()) return
  if(cleanupRequested) close()
  try {draw(target,time)} catch(e:Exception) {failed=true;log.error("Custom sky draw failed; native End sky remains. Toggle Skybox to retry.",e)}
 }
 private fun draw(target:RenderTarget,time:Double) {
  val color=target.colorTextureView ?: return
  val p=pipelines
  if(quads==null) quads=bake()
  val vertices=quads ?: return
  val s=settings();val frames=SkyboxLogic.frames(time,s.animationSpeed)
  val mc=Minecraft.getInstance()
  // Resolve texture uploads, geometry, uniforms AND index growth before opening a render pass.
  fun view(id:Identifier)=mc.textureManager.getTexture(id).also {owned.add(id)}.textureView
  val faceViews=faces.map {view(it)};val a=view(holes[frames.a]);val b=if(frames.b!=frames.a && frames.blend>.002f) view(holes[frames.b]) else null
  val matrix=RenderSystem.getModelViewMatrixCopy().rotateY(SkyboxLogic.rotation(time,s.rotationSpeed)).rotateX(SkyboxLogic.TILT)
  val tint=tintVector();val uniforms=RenderSystem.getDynamicUniforms()
  val opaque=uniforms.writeTransform(matrix,tint)
  val fadeA=if(b!=null) uniforms.writeTransform(matrix,Vector4f(tint.x*(1-frames.blend),tint.y*(1-frames.blend),tint.z*(1-frames.blend),1f)) else opaque
  val fadeB=if(b!=null) uniforms.writeTransform(matrix,Vector4f(tint.x*frames.blend,tint.y*frames.blend,tint.z*frames.blend,0f)) else opaque
  val indices=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);val indexBuffer=indices.getBuffer(42)
  val sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
  RenderSystem.getDevice().createCommandEncoder().createRenderPass({"solyanka skybox"},color,Optional.empty(),target.depthTextureView,OptionalDouble.empty()).use {pass->
   pass.setPipeline(p[0]);RenderSystem.bindDefaultUniforms(pass);pass.setVertexBuffer(0,vertices.slice());pass.setIndexBuffer(indexBuffer,indices.type());pass.setUniform("DynamicTransforms",opaque)
   faceViews.forEachIndexed {i,texture->pass.bindTexture("Sampler0",texture,sampler);pass.drawIndexed(6,1,i*6,0,0)}
   pass.setUniform("DynamicTransforms",fadeA);pass.bindTexture("Sampler0",a,sampler);pass.drawIndexed(6,1,36,0,0)
   if(b!=null) {pass.setPipeline(p[1]);RenderSystem.bindDefaultUniforms(pass);pass.setVertexBuffer(0,vertices.slice());pass.setIndexBuffer(indexBuffer,indices.type());pass.setUniform("DynamicTransforms",fadeB);pass.bindTexture("Sampler0",b,sampler);pass.drawIndexed(6,1,36,0,0)}
  }
 }
 private fun bake():GpuBuffer = ByteBufferBuilder.exactlySized(28*DefaultVertexFormat.POSITION_TEX_COLOR.vertexSize).use {bytes->
  val builder=BufferBuilder(bytes,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR)
  SkyboxLogic.vertices().forEach {v->builder.addVertex(v.position.x.toFloat(),v.position.y.toFloat(),v.position.z.toFloat()).setUv(v.u,v.v).setColor(-1)}
  builder.buildOrThrow().use {mesh->RenderSystem.getDevice().createBuffer({"solyanka skybox quads"},GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer())}
 }
 fun close() {
  if(!RenderSystem.isOnRenderThread()) {cleanupRequested=true;return}
  quads?.close();quads=null
  val manager=Minecraft.getInstance().textureManager;owned.forEach {manager.release(it)};owned.clear();failed=false;cleanupRequested=false
 }
}
