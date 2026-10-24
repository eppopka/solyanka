/* SPDX-License-Identifier: GPL-3.0-only
 * Stray HeldItemShader CC0 adaptation to Minecraft 26.2 bind groups/item preparation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.PrimitiveTopology
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.feature.ItemFeatureRenderer
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
object HeldItemCosmetic {
 private val logger=LoggerFactory.getLogger("solyanka/held-item")
 private val configId=Identifier.fromNamespaceAndPath("solyanka","held_item_settings")
 private val shaderId=Identifier.fromNamespaceAndPath("solyanka","core/held_item")
 private var texture:DynamicTexture?=null
 private var last:HeldItemSettings?=null
 private val animationClock=HeldItemClock() // Deliberately survives texture resize/reload/close.
 private var lastPhase:Int?=null
 @Volatile private var ready=false
 private var failed=false
 private var compiled=false
 @Volatile private var cleanup=false
 @Volatile private var status="Not prepared"
 @Volatile private var eligible=0L
 @Volatile private var replacements=0L
 @Volatile private var notReady=0L
 @Volatile private var incompatible=0L
 private var announced=false
 private val types=linkedMapOf<Identifier,RenderType>()
 // ITEM_SNIPPET already inherits Globals through MATRICES_FOG_LIGHT_DIR_SNIPPET.
 // Adding GLOBALS_SNIPPET again makes the required startup shader reload fail.
 private val pipelineDelegate=lazy {
  RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET)
   .withLocation("solyanka/held_item").withVertexShader(shaderId).withFragmentShader(shaderId)
   .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler3").withSampler("Sampler4").withSampler("Sampler5").build())
   .withShaderDefine("ALPHA_CUTOUT",.1f).withShaderDefine("PORTAL_LAYERS",15)
   .withColorTargetState(ColorTargetState.DEFAULT).withCull(false).build())
 }
 private val pipeline get()=pipelineDelegate.value
 fun register() {pipelineDelegate.value}
 fun retry() {ready=false;failed=false;compiled=false;eligible=0;replacements=0;notReady=0;incompatible=0;announced=false;status="Waiting for world render"}
 fun suspend() {ready=false;cleanup=true;status="Disabled"}
 fun requestCleanup() {ready=false;cleanup=true}
 fun diagnostics():String {
  val s=S.currentGeneral().features.heldItem
  return "Held Item: enabled=${S.isEnabled(S.HELD_ITEM)}, effect=${s.style}, right=${s.rightHand}, left=${s.leftHand}, ready=$ready, status=$status, eligible=$eligible, replaced=$replacements, notReady=$notReady, incompatible=$incompatible"
 }
 /** Before GameRenderer.renderLevel creates/prepares first-person draws. No dependency on client ticks. */
 fun prepareFrame() {
  if(!RenderSystem.isOnRenderThread()) return
  if(cleanup) close()
  val s=S.currentGeneral().features.heldItem
  val phase=HeldItemClock.texel(animationClock.sample(System.nanoTime(),s.speed,S.isEnabled(S.HELD_ITEM)))
  if(!S.isEnabled(S.HELD_ITEM)) {ready=false;return}
  if(failed) return
  try {
   val c=Minecraft.getInstance()
   if(!compiled) {
    // Catch native compilation failure explicitly instead of publishing a ready-but-invalid effect.
    val result=RenderSystem.getDevice().precompilePipeline(pipeline) {id,type->c.shaderManager.getShader(id,type)}
    if(!result.isValid) {ready=false;failed=true;status="Native shader compile failed; see latest.log";logger.error("Held-item pipeline is invalid; vanilla rendering retained");return}
    compiled=true
   }
   if(texture?.pixels?.isClosed==true) {texture=null;last=null;types.clear();ready=false}
  } catch(e:Exception) {ready=false;failed=true;status="Pipeline preparation failed; see latest.log";logger.error("Held-item pipeline preparation failed",e);return}
  if(last==s && lastPhase==phase && texture!=null) {ready=true;status="Native pipeline ready";return}
  try {
   pipelineDelegate.value // Register pipeline metadata before publishing readiness to item preparation.
   var current=texture
   if(current==null) {
    val image=NativeImage(3,1,false)
    val values=HeldItemPolicy.pixels(s);image.setPixel(0,0,values[0]);image.setPixel(1,0,values[1]);image.setPixel(2,0,phase)
    try {current=DynamicTexture({"solyanka held-item settings"},image)} catch(e:Exception) {image.close();throw e}
    Minecraft.getInstance().textureManager.register(configId,current);texture=current
   }
   val pixels=current.pixels
   val values=HeldItemPolicy.pixels(s);pixels.setPixel(0,0,values[0]);pixels.setPixel(1,0,values[1]);pixels.setPixel(2,0,phase);current.upload()
   last=s;lastPhase=phase;ready=true;status="Native pipeline ready"
  } catch(e:Exception) {ready=false;failed=true;status="Settings upload failed; see latest.log";logger.error("Held-item settings upload failed; vanilla rendering retained. Toggle the feature to retry.",e)}
 }
 @Synchronized @JvmStatic fun wrap(original:RenderType,submit:ItemFeatureRenderer.Submit):RenderType {
  if(!S.isEnabled(S.HELD_ITEM) || !HeldItemPolicy.hand(submit.displayContext().name,S.currentGeneral().features.heldItem)) return original
  eligible++
  if(!ready) {notReady++;return original}
  val p=original.pipeline()
  if(p===pipeline) return original
  if(original.isOutline || p.vertexFormatBindings.firstOrNull()!=DefaultVertexFormat.ENTITY || p.primitiveTopology!=PrimitiveTopology.QUADS) {incompatible++;return original}
  val atlas=submit.quads().firstOrNull {it.materialInfo().itemRenderType()===original}?.materialInfo()?.sprite()?.atlasLocation() ?: net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_ITEMS
  types[atlas]?.let {replaced();return it}
  if(types.size>=64) return original
  val type=RenderType.create("solyanka_held_item",RenderSetup.builder(pipeline)
   .withTexture("Sampler0",atlas).withTexture("Sampler1",ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
   .withTexture("Sampler3",AbstractEndPortalRenderer.END_SKY_LOCATION).withTexture("Sampler4",AbstractEndPortalRenderer.END_PORTAL_LOCATION)
   .withTexture("Sampler5",configId).useLightmap().affectsCrumbling().setOutline(RenderSetup.OutlineProperty.NONE).createRenderSetup())
  types[atlas]=type;replaced();return type
 }
 private fun replaced() {
  replacements++
  if(!announced) {announced=true;logger.info("Held-item replacement reached the native main item path: {}",S.currentGeneral().features.heldItem.style)}
 }
 @Synchronized fun close() {
  if(!RenderSystem.isOnRenderThread()) {ready=false;cleanup=true;return}
  ready=false;types.clear();if(texture!=null) Minecraft.getInstance().textureManager.release(configId)
  texture=null;last=null;lastPhase=null;failed=false;compiled=false;cleanup=false;status="Resources released"
 }
}
