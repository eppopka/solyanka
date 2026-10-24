/* SPDX-License-Identifier: GPL-3.0-only
 * Odin RenderUtils geometry adaptation; (c) 2025 odtheking, BSD-3-Clause. */
package dev.solyanka.client.feature
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import com.mojang.blaze3d.pipeline.*
import com.mojang.blaze3d.platform.CompareOp
import net.minecraft.world.phys.*

object WorldOverlay {
 data class Segment(val a:Vec3,val b:Vec3,val color:Int,val depth:Boolean=true,val width:Float=3f)
 data class Fill(val box:AABB,val color:Int,val depth:Boolean=true)
 data class Label(val pos:Vec3,val text:String,val color:Int=-1,val depth:Boolean=true,val scale:Float=1f)
 private data class Snapshot(val segments:List<Segment>,val fills:List<Fill>,val labels:List<Label>)
 @Volatile private var channels:Map<String,Snapshot> = emptyMap()
 private fun fillType(depth:Boolean)=RenderType.create("solyanka_fills_$depth",RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation("solyanka/fills_$depth").withCull(false).withDepthStencilState(DepthStencilState(if(depth) CompareOp.LESS_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS,false)).withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT)).build())).createRenderSetup())
 private val fillDepth by lazy {fillType(true)}
 private val fillEsp by lazy {fillType(false)}
 private val esp by lazy { RenderType.create("solyanka_lines_esp",RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).withLocation("solyanka/lines_esp").withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS,false)).withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT)).build())).createRenderSetup()) }
 fun publish(value:List<Segment>,filled:List<Fill> = emptyList(),text:List<Label> = emptyList(),owner:String="dungeon") { channels=channels+(owner to Snapshot(value.toList(),filled.toList(),text.toList())) }
 fun clear(owner:String) { channels=channels-owner }
 fun box(out:MutableList<Segment>,b:AABB,color:Int,depth:Boolean=true) {
  val v=listOf(Vec3(b.minX,b.minY,b.minZ),Vec3(b.maxX,b.minY,b.minZ),Vec3(b.maxX,b.maxY,b.minZ),Vec3(b.minX,b.maxY,b.minZ),Vec3(b.minX,b.minY,b.maxZ),Vec3(b.maxX,b.minY,b.maxZ),Vec3(b.maxX,b.maxY,b.maxZ),Vec3(b.minX,b.maxY,b.maxZ))
  listOf(0 to 1,1 to 2,2 to 3,3 to 0,4 to 5,5 to 6,6 to 7,7 to 4,0 to 4,1 to 5,2 to 6,3 to 7).forEach { (a,z)->out.add(Segment(v[a],v[z],color,depth)) }
 }
 fun register() { LevelRenderEvents.COLLECT_SUBMITS.register { c -> render(c) } }
 private fun render(c:LevelRenderContext) {
  if(net.minecraft.client.Minecraft.getInstance().gui.hud.isHidden) return
  val values=channels.values.toList();val snapshot=values.flatMap {it.segments}.toMutableList();val camera=c.levelState().cameraRenderState.pos
  val filled=values.flatMap {it.fills}.toMutableList();val text=values.flatMap {it.labels}
  BlockSelection.collect(c,snapshot,filled)
  val pose=c.poseStack();pose.pushPose()
  try { pose.translate(-camera.x,-camera.y,-camera.z)
   filled.groupBy {it.depth}.forEach { (depth,list) ->
    c.submitNodeCollector().submitCustomGeometry(pose,if(depth) fillDepth else fillEsp) { matrix,buffer ->
     list.forEach { f -> val b=f.box
      val v=arrayOf(Vec3(b.minX,b.minY,b.minZ),Vec3(b.maxX,b.minY,b.minZ),Vec3(b.maxX,b.maxY,b.minZ),Vec3(b.minX,b.maxY,b.minZ),Vec3(b.minX,b.minY,b.maxZ),Vec3(b.maxX,b.minY,b.maxZ),Vec3(b.maxX,b.maxY,b.maxZ),Vec3(b.minX,b.maxY,b.maxZ))
      arrayOf(intArrayOf(0,1,2,3),intArrayOf(4,5,6,7),intArrayOf(0,1,5,4),intArrayOf(3,2,6,7),intArrayOf(0,3,7,4),intArrayOf(1,2,6,5)).forEach {face->face.forEach {index->val a=v[index];buffer.addVertex(matrix,a.x.toFloat(),a.y.toFloat(),a.z.toFloat()).setColor(f.color)} }
     }
    }
   }
   text.forEach { label ->
    pose.pushPose()
    try {pose.translate(label.pos.x,label.pos.y,label.pos.z);pose.mulPose(c.levelState().cameraRenderState.orientation);val scale=.025f*label.scale;pose.scale(scale,-scale,scale)
     val component=net.minecraft.network.chat.Component.literal(label.text)
     val width=net.minecraft.client.Minecraft.getInstance().font.width(component)
     c.submitNodeCollector().submitText(pose,-width/2f,0f,component.visualOrderText,true,if(label.depth) net.minecraft.client.gui.Font.DisplayMode.NORMAL else net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,0x00f000f0,label.color,0,0)
    } finally {pose.popPose()}
   }
   snapshot.groupBy { it.depth }.forEach { (depth,list) ->
    c.submitNodeCollector().submitCustomGeometry(pose,if(depth) RenderTypes.LINES else esp) { matrix,buffer ->
     list.forEach { s -> val dx=(s.b.x-s.a.x).toFloat();val dy=(s.b.y-s.a.y).toFloat();val dz=(s.b.z-s.a.z).toFloat()
      buffer.addVertex(matrix,s.a.x.toFloat(),s.a.y.toFloat(),s.a.z.toFloat()).setColor(s.color).setNormal(matrix,dx,dy,dz).setLineWidth(s.width)
      buffer.addVertex(matrix,s.b.x.toFloat(),s.b.y.toFloat(),s.b.z.toFloat()).setColor(s.color).setNormal(matrix,dx,dy,dz).setLineWidth(s.width)
     }
    }
   }
  } finally {pose.popPose()}
 }
}
