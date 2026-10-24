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
 @Volatile private var ready=false
 private var failed=false
 private val types=linkedMapOf<Identifier,RenderType>()
 private val pipelineDelegate=lazy {
  RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET,RenderPipelines.GLOBALS_SNIPPET)
   .withLocation("solyanka/held_item").withVertexShader(shaderId).withFragmentShader(shaderId)
   .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler3").withSampler("Sampler4").withSampler("Sampler5").build())
   .withShaderDefine("ALPHA_CUTOUT",.1f).withShaderDefine("PORTAL_LAYERS",15)
   .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build())
 }
 private val pipeline get()=pipelineDelegate.value
 fun retry() {failed=false}
 fun suspend() {ready=false}
 fun tick() {
  if(!S.isEnabled(S.HELD_ITEM)) {ready=false;return}
  if(failed || !RenderSystem.isOnRenderThread()) return
  val s=S.currentGeneral().features.heldItem
  if(last==s && texture!=null) {ready=true;return}
  try {
   pipelineDelegate.value // Register pipeline metadata before publishing readiness to item preparation.
   var current=texture
   if(current==null) {
    val image=NativeImage(2,1,false)
    try {current=DynamicTexture({"solyanka held-item settings"},image)} catch(e:Exception) {image.close();throw e}
    Minecraft.getInstance().textureManager.register(configId,current);texture=current
   }
   val pixels=current.pixels
   val values=HeldItemPolicy.pixels(s);pixels.setPixel(0,0,values[0]);pixels.setPixel(1,0,values[1]);current.upload()
   last=s;ready=true
  } catch(e:Exception) {ready=false;failed=true;logger.error("Held-item settings upload failed; vanilla rendering retained. Toggle the feature to retry.",e)}
 }
 @Synchronized @JvmStatic fun wrap(original:RenderType,submit:ItemFeatureRenderer.Submit):RenderType {
  if(!S.isEnabled(S.HELD_ITEM) || !ready || !HeldItemPolicy.hand(submit.displayContext().name,S.currentGeneral().features.heldItem)) return original
  val p=original.pipeline()
  if(original.isOutline || p.vertexFormatBindings.firstOrNull()!=DefaultVertexFormat.ENTITY || p.primitiveTopology!=PrimitiveTopology.QUADS) return original
  val atlas=submit.quads().firstOrNull {it.materialInfo().itemRenderType()===original}?.materialInfo()?.sprite()?.atlasLocation() ?: return original
  types[atlas]?.let {return it}
  if(types.size>=64) return original
  val type=RenderType.create("solyanka_held_item",RenderSetup.builder(pipeline)
   .withTexture("Sampler0",atlas).withTexture("Sampler1",ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
   .withTexture("Sampler3",AbstractEndPortalRenderer.END_SKY_LOCATION).withTexture("Sampler4",AbstractEndPortalRenderer.END_PORTAL_LOCATION)
   .withTexture("Sampler5",configId).useLightmap().setOutline(RenderSetup.OutlineProperty.NONE).createRenderSetup())
  types[atlas]=type;return type
 }
 @Synchronized fun close() {ready=false;types.clear();if(texture!=null) Minecraft.getInstance().textureManager.release(configId);texture=null;last=null;failed=false}
}
