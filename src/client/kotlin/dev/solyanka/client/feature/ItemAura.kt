/* SPDX-License-Identifier: GPL-3.0-only; user impl.zip ItemAura adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.PrimitiveTopology
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import kotlin.math.roundToInt
/** Captures final item/arm pose. Flushes AFTER vanilla hand features, before hand view matrix is popped. */
object ItemAura {
 private val captured=ArrayList<Matrix4f>(2)
 private var failed=false
 private fun type(depth:Boolean):RenderType {
  val p=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation("solyanka/item_aura_$depth")
   .withVertexBinding(0,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
   .withCull(false).withDepthStencilState(DepthStencilState(if(depth) CompareOp.GREATER_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS,false))
   .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT)).build()
  BindGroupLayout.ensureCompatible(p.bindGroupLayouts)
  return RenderType.create("solyanka_item_aura_$depth",RenderSetup.builder(RenderPipelines.register(p)).createRenderSetup())
 }
 private val depthType by lazy {type(true)};private val topType by lazy {type(false)}
 fun clear() {captured.clear();failed=false}
 fun begin() {captured.clear()}
 @JvmStatic fun captureHand(pose:PoseStack,hand:InteractionHand,stack:ItemStack) {
  if(!S.isEnabled(S.ITEM_AURA) || failed || captured.size>=2) return
  val s=S.currentGeneral().features.itemAura
  if(hand==InteractionHand.OFF_HAND && !s.offHand || s.onlyWithItem && stack.isEmpty) return
  val sign=if(hand==InteractionHand.MAIN_HAND)1.0 else -1.0
  val m=Matrix4f(pose.last().pose()).translate((s.offsetX*sign).toFloat(),s.offsetY.toFloat(),s.offsetZ.toFloat())
   .rotateXYZ(Math.toRadians(s.rotX).toFloat(),Math.toRadians(s.rotY*sign).toFloat(),Math.toRadians(s.rotZ*sign).toFloat())
  if(s.spin!=0.0)m.rotateY(Math.toRadians((System.nanoTime()*1e-9*s.spin)%360).toFloat())
  m.scale(1f,s.stretch.toFloat(),1f);captured.add(m)
 }
 fun flush(dispatcher:FeatureRenderDispatcher) {
  if(captured.isEmpty())return
  val matrices=captured.toList();captured.clear()
  if(!S.isEnabled(S.ITEM_AURA) || failed)return
  val storage=SubmitNodeStorage()
  try {
   val s=S.currentGeneral().features.itemAura;val mesh=ItemAuraMesh(s).build(System.nanoTime()*1e-9)
   val type=if(s.depth)depthType else topType
   for(matrix in matrices) {
    val pose=PoseStack();pose.mulPose(matrix)
    storage.submitCustomGeometry(pose,type) {p,v->
     var i=0
     while(i+6<mesh.size) {
      val color=((mesh[i+6]*255).roundToInt().coerceIn(0,255) shl 24) or ((mesh[i+3]*255).roundToInt().coerceIn(0,255) shl 16) or ((mesh[i+4]*255).roundToInt().coerceIn(0,255) shl 8) or (mesh[i+5]*255).roundToInt().coerceIn(0,255)
      v.addVertex(p,mesh[i],mesh[i+1],mesh[i+2]).setColor(color);i+=7
     }
    }
   }
   dispatcher.renderAllFeatures(storage)
  } catch(e:Exception) {failed=true;org.slf4j.LoggerFactory.getLogger("solyanka/item-aura").error("Item Aura disabled until reset; vanilla hand retained",e)}
  finally {storage.submitsPerOrder.clear()}
 }
}
