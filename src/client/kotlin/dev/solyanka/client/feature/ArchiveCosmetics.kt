/* SPDX-License-Identifier: GPL-3.0-only; impl.zip ChinaHat/Halo geometry adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext

object ArchiveCosmetics {
 fun collect(ctx:LevelRenderContext,lines:MutableList<WorldOverlay.Segment>,triangles:MutableList<WorldOverlay.Triangle>) {
  val c=Minecraft.getInstance();val p=c.player ?: return
  if(c.level==null || p.isSpectator || p.isInvisible) return
  val f=S.currentGeneral().features;val first=c.options.cameraType.isFirstPerson
  val time=System.nanoTime()/1_000_000_000.0
  val pos=p.getPosition(c.deltaTracker.getGameTimeDeltaPartialTick(true)).add(0.0,p.bbHeight+.03,0.0)
  fun rainbow(color:Int)=(java.awt.Color.HSBtoRGB(((time/4)%1).toFloat(),.75f,1f) and 0xffffff) or (color and -0x1000000)
  fun faces(values:List<CosmeticMesh.Face>,origin:Vec3,depth:Boolean) {values.forEach {t->fun point(v:Point3)=origin.add(v.x,v.y,v.z)
   triangles.add(WorldOverlay.Triangle(point(t.a),point(t.b),point(t.c),t.color,depth))}}
  if(S.isEnabled(S.CHINA_HAT) && !(first && f.chinaHat.hideFirstPerson)) {
   val s=f.chinaHat;val color=if(s.rainbow)rainbow(s.color) else s.color;val (mesh,edges)=CosmeticMesh.cone(s.copy(color=color))
   faces(mesh,pos,!s.throughWalls);edges.forEach {e->lines.add(WorldOverlay.Segment(pos.add(e.a.x,e.a.y,e.a.z),pos.add(e.b.x,e.b.y,e.b.z),color or -0x1000000,!s.throughWalls,s.width.toFloat()))}
  }
  if(S.isEnabled(S.HALO) && !(first && f.halo.hideFirstPerson)) {
   val s=f.halo;faces(CosmeticMesh.halo(s.copy(color=if(s.rainbow)rainbow(s.color) else s.color),time),pos.add(0.0,s.height+(if(s.bob)kotlin.math.sin(time/ .6)*.06 else 0.0),0.0),!s.throughWalls)
  }
 }
}
