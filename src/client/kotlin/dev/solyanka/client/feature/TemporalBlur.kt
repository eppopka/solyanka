/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 FrameBlendingManager temporal modes, bounded native 26.2 integration. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import com.mojang.blaze3d.*
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryStack
import org.slf4j.LoggerFactory
import java.util.Optional
object TemporalBlur {
 private val log=LoggerFactory.getLogger("solyanka/motion-blur")
 private var read:TextureTarget?=null;private var write:TextureTarget?=null
 private var params:GpuBuffer?=null;private var last:MotionBlurSettings?=null
 private var seeded=false;private var failed=false;private var lastFrame=0L
 private var frameOutput:TextureTarget?=null
 private var frames=arrayOfNulls<TextureTarget>(0)
 private var history:FrameBlendHistory?=null
 private var refreshRate=60;private var monitorCheck=0L
 private val pipelines=linkedMapOf<Pair<GpuFormat,String>,RenderPipeline>()
 @Volatile private var cleanup=false
 fun retry() {failed=false;cleanup=true}
 fun requestCleanup() {cleanup=true}
 fun tick() {if(cleanup && RenderSystem.isOnRenderThread()) close()}
 private fun pipeline(format:GpuFormat,mode:String)=pipelines.getOrPut(format to mode) {
  val layout=BindGroupLayout.builder()
  if(mode=="frame_blend") {
   for(i in 0 until 12) layout.withSampler("Sample${i}Sampler")
   layout.withUniform("FrameBlendParamsUniforms",UniformType.UNIFORM_BUFFER)
  } else layout.withSampler("MainSampler").withSampler("PrevSampler").withUniform("AccumulationUniforms",UniformType.UNIFORM_BUFFER)
  RenderPipelines.register(RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("solyanka","motion_${mode}_${format.name.lowercase()}"))
   .withVertexShader("core/screenquad").withFragmentShader(Identifier.fromNamespaceAndPath("solyanka","post/$mode"))
   .withBindGroupLayout(layout.build()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
   .withColorTargetState(ColorTargetState(Optional.empty(),format,ColorTargetState.WRITE_ALL)).build())
 }
 fun apply(target:RenderTarget) {
  if(!RenderSystem.isOnRenderThread()) return
  if(cleanup) close()
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.motionBlur
  if(!S.isEnabled(S.MOTION_BLUR) || s.strength==0.0 || c.level==null || s.pauseInMenus && c.gui.screen()!=null) {if(read!=null || frameOutput!=null) close();return}
  if(failed) return
  try {draw(target,s)} catch(e:Exception) {close();failed=true;log.error("Motion Blur disabled for this session; native frame retained. Toggle to retry.",e)}
 }
 private fun draw(main:RenderTarget,s:MotionBlurSettings) {
  val texture=main.colorTexture ?: return;val currentView=main.colorTextureView ?: return
  val format=texture.format
  if(!BlurPolicy.sizeAllowed(main.width,main.height) || format !in setOf(GpuFormat.RGBA8_UNORM,GpuFormat.RGBA16_FLOAT)) {close();return}
  if(last?.mode!=null && last?.mode!=s.mode) close()
  val now=System.nanoTime()
  if(s.mode=="frame_blend") {drawFrames(main,s,format,now);return}
  if(read?.width!=main.width || read?.height!=main.height || read?.colorTexture?.format!=format || last?.mode!=s.mode) {
   close();read=TextureTarget("solyanka blur read",main.width,main.height,false,format);write=TextureTarget("solyanka blur write",main.width,main.height,false,format)
  }
  val previous=read ?: return;val next=write ?: return
  val encoder=RenderSystem.getDevice().createCommandEncoder()
  fun copy(src:RenderTarget,dst:RenderTarget) {encoder.copyTextureToTexture(src.colorTexture!!,dst.colorTexture!!,0,0,0,0,0,main.width,main.height)}
  if(!seeded || now-lastFrame>500_000_000L) {copy(main,previous);seeded=true;last=s;lastFrame=now;return}
  val p=pipeline(format,s.mode)
  if(params==null) params=RenderSystem.getDevice().createBuffer({"solyanka accumulation params"},GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,16L)
  // Writes are recorded before the render pass. A fresh value is cheap and avoids stale params after seeding.
  MemoryStack.stackPush().use {stack->val bytes=stack.malloc(16);bytes.putFloat(BlurPolicy.factor(s)).putInt(0).putInt(0).putInt(0).flip();encoder.writeToBuffer(params!!.slice(),bytes)}
  val sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
  encoder.createRenderPass({"solyanka accumulation blur"},next.colorTextureView!!,Optional.empty()).use {pass->
   pass.setPipeline(p);pass.bindTexture("MainSampler",currentView,sampler);pass.bindTexture("PrevSampler",previous.colorTextureView!!,sampler);pass.setUniform("AccumulationUniforms",params!!);pass.draw(3,1,0,0)
  }
  copy(next,main);read=next;write=previous;last=s;lastFrame=now
 }
 private fun drawFrames(main:RenderTarget,s:MotionBlurSettings,format:GpuFormat,now:Long) {
  val capacity=FrameBlendHistory.capacity(main.width,main.height,if(format==GpuFormat.RGBA16_FLOAT) 8 else 4)
  if(capacity<2) {close();return}
  if(frameOutput?.width!=main.width || frameOutput?.height!=main.height || frameOutput?.colorTexture?.format!=format) {
   close();frameOutput=TextureTarget("solyanka frame blend output",main.width,main.height,false,format)
   frames=arrayOfNulls(capacity);history=FrameBlendHistory(capacity)
  }
  val clock=history ?: return
  val stamp=now*1.0e-9;val index=clock.push(stamp)
  val raw=frames[index] ?: TextureTarget("solyanka raw frame $index",main.width,main.height,false,format).also {frames[index]=it}
  val encoder=RenderSystem.getDevice().createCommandEncoder()
  // Capture the untouched world, never our accumulated/composited previous output.
  encoder.copyTextureToTexture(main.colorTexture!!,raw.colorTexture!!,0,0,0,0,0,main.width,main.height)
  val samples=clock.samples(stamp,monitorRefresh(now),s.strength)
  last=s;lastFrame=now
  if(samples.size<=1) return
  if(params==null) params=RenderSystem.getDevice().createBuffer({"solyanka frame blend params"},GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,64L)
  val total=samples.sumOf {it.weight.toDouble()}.toFloat()
  MemoryStack.stackPush().use {stack->
   val bytes=stack.malloc(64);bytes.putFloat(1f/total).putInt(samples.size)
   for(i in 0 until 12) bytes.putFloat(samples.getOrNull(i)?.weight ?: 0f)
   bytes.putFloat(0f).putFloat(0f).flip();encoder.writeToBuffer(params!!.slice(),bytes)
  }
  val out=frameOutput ?: return
  val sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
  encoder.createRenderPass({"solyanka Stray weighted frame blend"},out.colorTextureView!!,Optional.empty()).use {pass->
   pass.setPipeline(pipeline(format,"frame_blend"))
   val fallback=frames[samples.last().index]!!
   for(i in 0 until 12) {
    val sample=samples.getOrNull(i)?.let {frames[it.index]} ?: fallback
    pass.bindTexture("Sample${i}Sampler",sample.colorTextureView!!,sampler)
   }
   pass.setUniform("FrameBlendParamsUniforms",params!!);pass.draw(3,1,0,0)
  }
  encoder.copyTextureToTexture(out.colorTexture!!,main.colorTexture!!,0,0,0,0,0,main.width,main.height)
 }
 /** Stray refresh-aware exposure; query GLFW at most once per second, not per sample. */
 private fun monitorRefresh(now:Long):Int {
  if(now-monitorCheck<1_000_000_000L) return refreshRate
  monitorCheck=now
  val window=Minecraft.getInstance().window.handle()
  var monitor=org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(window)
  if(monitor==0L) MemoryStack.stackPush().use {stack->
   val x=stack.mallocInt(1);val y=stack.mallocInt(1);val w=stack.mallocInt(1);val h=stack.mallocInt(1)
   org.lwjgl.glfw.GLFW.glfwGetWindowPos(window,x,y);org.lwjgl.glfw.GLFW.glfwGetWindowSize(window,w,h)
   val monitors=org.lwjgl.glfw.GLFW.glfwGetMonitors()
   var best=0L
   if(monitors!=null) for(i in 0 until monitors.limit()) {
    val candidate=monitors[i];val mode=org.lwjgl.glfw.GLFW.glfwGetVideoMode(candidate) ?: continue
    val mx=stack.mallocInt(1);val my=stack.mallocInt(1);org.lwjgl.glfw.GLFW.glfwGetMonitorPos(candidate,mx,my)
    val overlapW=(minOf(x[0]+w[0],mx[0]+mode.width())-maxOf(x[0],mx[0])).coerceAtLeast(0)
    val overlapH=(minOf(y[0]+h[0],my[0]+mode.height())-maxOf(y[0],my[0])).coerceAtLeast(0)
    val area=overlapW.toLong()*overlapH
    if(area>best) {best=area;monitor=candidate}
   }
  }
  if(monitor==0L) monitor=org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor()
  refreshRate=(org.lwjgl.glfw.GLFW.glfwGetVideoMode(monitor)?.refreshRate() ?: 60).coerceAtLeast(1)
  return refreshRate
 }
 fun close() {
  if(!RenderSystem.isOnRenderThread()) {cleanup=true;return}
  frames.forEach {it?.destroyBuffers()};frames=arrayOfNulls(0);frameOutput?.destroyBuffers();frameOutput=null;history=null
  read?.destroyBuffers();read=null;write?.destroyBuffers();write=null;params?.close();params=null;last=null;seeded=false;lastFrame=0;cleanup=false
 }
}
