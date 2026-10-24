/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 selection contour adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import net.fabricmc.fabric.api.client.rendering.v1.level.*
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
object BlockSelection {
 private fun enabled()=S.isEnabled(S.BLOCK_OUTLINE) && !Minecraft.getInstance().gui.hud.isHidden
 fun register() {LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register {_:LevelRenderContext,outline:net.minecraft.client.renderer.state.level.BlockOutlineRenderState?-> !enabled() || outline==null || outline.shape().isEmpty}}
 fun collect(c:LevelRenderContext,lines:MutableList<WorldOverlay.Segment>,fills:MutableList<WorldOverlay.Fill>) {
  if(!enabled()) return
  val outline=c.levelState().blockOutlineRenderState ?: return
  val shape=outline.shape();if(shape.isEmpty) return
  val pos=outline.pos();val s=S.currentGeneral().features.worldVisuals.blockOutline
  var edges=0
  shape.forAllEdges {x,y,z,a,b,d->if(edges++<1024) {
   val from=Vec3(x+pos.x,y+pos.y,z+pos.z);val to=Vec3(a+pos.x,b+pos.y,d+pos.z)
   if(s.glow) dev.solyanka.utility.VisualChrome.halo(s.color,s.width,s.glowRadius,s.glowStrength).forEach {(width,color)->lines.add(WorldOverlay.Segment(from,to,color,!s.throughWalls,width))}
   lines.add(WorldOverlay.Segment(from,to,s.color,!s.throughWalls,s.width.toFloat()))
  }}
  if(s.filled) shape.toAabbs().take(128).forEach {fills.add(WorldOverlay.Fill(it.move(pos),s.fillColor,!s.throughWalls))}
 }
}
