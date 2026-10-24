/* SPDX-License-Identifier: GPL-3.0-only; impl.zip BestiaryESP/InvisibugESP/TikiESP adaptation. */
package dev.solyanka.client.feature

import com.google.gson.JsonParser
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.*
import java.util.Base64
import kotlin.math.*

object HuntingFeatures {
 private val bugs=BugEvidence()
 private var matched=emptySet<Entity>()
 private var matchPolicy:Pair<String,Boolean>?=null
 private var tick=0
 private var scan=0
 private var center:BlockPos?=null
 private val towers=linkedMapOf<BlockPos,Int>()
 private val textures=listOf(
  listOf("247da91d9d4dd57bad2678baf648e4d9dd4b9f454a606962ceaac11ed00dea9f","dfe6925f85cf02d2f8b501291d26aa5f4f6a136de0e974c2e9e3c20633ae9202","bb19e5b72167db517ff8b9e2dd0735996641bdb6b6267a758ad2864022023cb7"),
  listOf("3eac8820104cc421e0392142dc7c79eb84bd2be8f8155c29d72f5fc58d4ee953","b89f1cb02d4acd71940d9e6c29fe75ce3b15d291d9e383030b59e068b5f5a135","28bdbd87b64e049562c986bb4c5b1336d92e68431260a9e0e8f3338d302b9761"),
  listOf("419773998a0b85ffebde5ff925ea4c9b9e007436b0eb9a43afd2d3b367d241f6","7a267c2699e38062ea4baedbfa4bb80a631048f7c8d2ee4c460f40036b084b2b","f62bef8ca58d83766c7e26f9fb4fe06c11b93542a9c10f36f460a3b427748223")
 )
 fun reset() {bugs.clear();matched=emptySet();matchPolicy=null;towers.clear();scan=0;center=null;tick=0;listOf("bestiary","invisibug","tiki").forEach(WorldOverlay::clear)}
 private fun inGalatea()=FeatureRuntime.context.skyblock && FeatureRuntime.context.area.equals("Galatea",true)
 fun glow(e:Entity)=matchPolicy==S.currentGeneral().features.bestiary.let {it.names to it.exact} && S.isEnabled(S.BESTIARY) && Minecraft.getInstance().level!=null && e.isAlive && e in matched && !Minecraft.getInstance().gui.hud.isHidden
 fun glowColor()=S.currentGeneral().features.bestiary.color or -0x1000000
 fun particle(p:ClientboundLevelParticlesPacket) {
  if(!S.isEnabled(S.INVISIBUG) || !inGalatea())return
  if(p.particle.type!=ParticleTypes.CRIT || p.count!=1 || p.maxSpeed!=0f || p.xDist!=0f || p.yDist!=0f || p.zDist!=0f || !p.alwaysShow() || !p.isOverrideLimiter)return
  val c=Minecraft.getInstance();val level=c.level ?: return;val player=c.player ?: return;val pos=Vec3(p.x,p.y,p.z)
  val marker=level.getEntities(player,AABB.ofSize(pos,1.0,2.0,1.0)){true}.singleOrNull() as? ArmorStand ?: return
  if(marker.isMarker && marker.customName==null && marker.mainHandItem.isEmpty)bugs.observe(Point3(pos.x,pos.y,pos.z))
 }
 fun tick(c:Minecraft) {
  val level=c.level ?: run {reset();return};val player=c.player ?: run {reset();return};tick++
  val f=S.currentGeneral().features
  val origin=c.gameRenderer.mainCamera().position().add(player.lookAngle.scale(.5))
  if(S.isEnabled(S.BESTIARY)) {
   // Revalidate names AND exact mode on every scan; resolve again after metadata arrives late.
   if(tick%5==0) {
    val matches=linkedSetOf<Entity>()
    level.entitiesForRendering().asSequence().filterIsInstance<ArmorStand>().take(4096).forEach {stand->
     if(stand.customName?.string?.let(f.bestiary::matches)==true) {
      val prev=level.getEntity(stand.id-1)
      val mob=if(prev!=null && prev !is ArmorStand && prev!==player && prev.isAlive && prev.distanceToSqr(stand)<=16) prev else
       level.getEntities(stand,stand.boundingBox.move(0.0,-1.0,0.0)){it !is ArmorStand && it!==player && it.isAlive}.firstOrNull()
      if(mob!=null)matches.add(mob)
     }
    };matched=matches;matchPolicy=f.bestiary.names to f.bestiary.exact
   }
   val nearest=matched.filter {it.isAlive}.minByOrNull {it.distanceToSqr(player)}
   val s=f.bestiary
   val lines=if(s.tracer && nearest!=null && (s.range==0 || nearest.distanceToSqr(player)<=s.range.toDouble()*s.range)) listOf(WorldOverlay.Segment(origin,nearest.position().add(0.0,1.0,0.0),s.color,false,2f)) else emptyList()
   WorldOverlay.publish(lines,owner="bestiary")
  } else {matched=emptySet();WorldOverlay.clear("bestiary")}
  bugs.tick()
  if(S.isEnabled(S.INVISIBUG) && inGalatea()) {
   val s=f.invisibug;val lines=mutableListOf<WorldOverlay.Segment>();val fills=mutableListOf<WorldOverlay.Fill>();val labels=mutableListOf<WorldOverlay.Label>()
   val points=bugs.confirmed().map {Vec3(it.x,it.y,it.z)}
   points.forEach {pos->val box=AABB.ofSize(pos,1.0,1.0,1.0);WorldOverlay.box(lines,box,s.color,false);fills.add(WorldOverlay.Fill(box,s.color,false));labels.add(WorldOverlay.Label(pos.add(0.0,1.3,0.0),"Invisibug"+(if(s.distance) " (${"%.1f".format(java.util.Locale.ROOT,pos.distanceTo(player.position()))}m)" else ""),s.color,false,1.4f))}
   if(s.tracer)points.minByOrNull {it.distanceToSqr(player.position())}?.let {lines.add(WorldOverlay.Segment(origin,it.add(0.0,.5,0.0),s.color,false,2f))}
   WorldOverlay.publish(lines,fills,labels,"invisibug")
  } else {bugs.clear();WorldOverlay.clear("invisibug")}
  if(S.isEnabled(S.TIKI) && FeatureRuntime.context.skyblock) {
   fun texture(pos:BlockPos):String {
    if(!level.hasChunkAt(pos))return ""
    val skull=level.getBlockEntity(pos) as? SkullBlockEntity ?: return ""
    val payload=skull.ownerProfile?.partialProfile()?.properties()?.get("textures")?.firstOrNull()?.value() ?: return ""
    if(payload.length>16384)return ""
    return runCatching {JsonParser.parseString(String(Base64.getDecoder().decode(payload))).asJsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN")["url"].asString.substringAfterLast('/')}.getOrDefault("")
   }
   fun matches(pos:BlockPos,type:Int)= (0..2).all {texture(pos.above(it))==textures[type][it]}
   val p=player.blockPosition()
   if(center==null || center!!.x shr 4 != p.x shr 4 || center!!.z shr 4 != p.z shr 4) {center=p;scan=0}
   val cp=center!!
   repeat(32) {
    val i=scan++%625;val x=(cp.x shr 4)+i%25-12;val z=(cp.z shr 4)+i/25-12
    val chunk=level.chunkSource.getChunk(x,z,ChunkStatus.FULL,false) ?: return@repeat
    chunk.blockEntities.keys.asSequence().take(4096).filter {it.distSqr(p)<=192.0*192.0}.forEach {pos->
     val t=textures.indexOfFirst {it[0]==texture(pos)}
     if(t>=0 && towers.size<512 && matches(pos,t))towers[pos.immutable()]=t
    }
   }
   towers.entries.removeIf {it.key.distSqr(p)>192.0*192.0 || !matches(it.key,it.value)}
   val s=f.tiki;val lines=mutableListOf<WorldOverlay.Segment>();val fills=mutableListOf<WorldOverlay.Fill>();val labels=mutableListOf<WorldOverlay.Label>();val time=System.nanoTime()/1_000_000_000.0
   towers.forEach {(pos,type)->
    val color=s.color(type);val mid=Vec3.atCenterOf(pos).add(0.0,1.0,0.0);val box=AABB(pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble(),pos.x+1.0,pos.y+3.0,pos.z+1.0)
    if(s.style!=1)WorldOverlay.box(lines,box,color,false)
    if(s.style!=0)fills.add(WorldOverlay.Fill(box,color,false))
    if(s.tracer)lines.add(WorldOverlay.Segment(origin,mid,color,false,2.5f))
    if(s.distance)labels.add(WorldOverlay.Label(mid.add(0.0,2.0,0.0),"${listOf("Sneaky","Cheeky","Shrieky")[type]} Tiki • ${mid.distanceTo(player.position()).roundToInt()}m",color,false,1.2f))
    if(s.particles)repeat(s.count){i->
     val progress=i.toDouble()/s.count
     val (angle,radius,y)=when(s.particleStyle) {
      0->{val ring=i%3;val a=time*(1.2+ring*.22)+progress*PI*6;Triple(a,.68+sin(time*2.2+i*.7)*.08,-1.28+ring*1.24+sin(a*2)*.08)}
      1->Triple(time*1.7+progress*PI*4+(i and 1)*PI,.65,-1.42+((progress*3.15+time*.38)%3.15))
      else->Triple(i*2.399963+time*.35+sin(time*.9+i)*.35,.36+(i%6)*.075,-1.42+((i*.37+time*.48)%3.2))
     }
     val point=mid.add(cos(angle)*radius,y,sin(angle)*radius);val size=if(s.particleStyle==2).08+sin(time*4+i)*.025 else .075
     fills.add(WorldOverlay.Fill(AABB.ofSize(point,size*3,size*3,size*3),(s.particleColor and 0xffffff) or (28 shl 24),false));fills.add(WorldOverlay.Fill(AABB.ofSize(point,size,size,size),s.particleColor,false))
    }
   }
   WorldOverlay.publish(lines,fills,labels,"tiki")
  } else {towers.clear();WorldOverlay.clear("tiki")}
 }
}
