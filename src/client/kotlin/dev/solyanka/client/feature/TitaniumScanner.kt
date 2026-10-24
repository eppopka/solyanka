/* SPDX-License-Identifier: GPL-3.0-only
 * Stray CC0 TitaniumTracker/MiningTracker adaptation; bounded loaded-only scan. */
package dev.solyanka.client.feature
import com.google.gson.JsonParser
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.*
import java.util.ArrayDeque
import kotlin.math.sqrt
object TitaniumScanner {
 private data class Job(val pos:GridPos,var offset:Int=0)
 private val jobs=ArrayDeque<Job>()
 private val candidates=linkedSetOf<BlockPos>()
 private var plan:String?=null
 private var cooldown=0
 private val markers:List<TitaniumMarker> by lazy {
  javaClass.getResourceAsStream("/assets/solyanka/mining/titanium-regions.json")!!.bufferedReader().use {reader->
   JsonParser.parseReader(reader).asJsonArray.map {raw->val j=raw.asJsonObject;TitaniumMarker(if(j["region"].isJsonNull) null else TitaniumRegion.valueOf(j["region"].asString),j["x"].asInt,j["y"].asInt,j["z"].asInt)}
  }
 }
 fun reset() {jobs.clear();candidates.clear();plan=null;cooldown=0;WorldOverlay.clear("titanium")}
 fun tick(c:Minecraft) {
  val level=c.level;val player=c.player
  if(level==null || player==null || !S.isEnabled(S.TITANIUM) || !TitaniumLogic.miningArea(FeatureRuntime.context.skyblock,FeatureRuntime.context.area)) {reset();return}
  val s=S.currentGeneral().features.worldVisuals.titanium
  val tab=c.connection?.listedOnlinePlayers?.sortedWith(compareByDescending<net.minecraft.client.multiplayer.PlayerInfo> {it.tabListOrder}.thenBy {if(it.gameMode==net.minecraft.world.level.GameType.SPECTATOR) 1 else 0}.thenBy {it.team?.name ?: ""}.thenBy(String.CASE_INSENSITIVE_ORDER) {it.profile.name})?.mapNotNull {it.tabListDisplayName?.string} ?: emptyList()
  val filter=TitaniumLogic.filter(TitaniumLogic.commissions(tab),s.all)
  if(!filter.active) {reset();return}
  val center=player.blockPosition();val key="${center.x shr 4}:${center.y shr 4}:${center.z shr 4}:${s.range}:$filter"
  fun allowed(pos:BlockPos)=TitaniumLogic.within(pos.x,pos.y,pos.z,center.x,center.y,center.z,s.range) && filter.allows(if(filter.all) null else TitaniumLogic.region(markers,pos.x,pos.y,pos.z))
  if(plan!=key) {jobs.clear();plan=key;cooldown=0}
  candidates.removeIf {pos->!level.hasChunk(pos.x shr 4,pos.z shr 4) || !allowed(pos) || !level.getBlockState(pos).`is`(Blocks.POLISHED_DIORITE)}
  if(jobs.isEmpty() && cooldown--<=0) {
   TitaniumLogic.sections(center.x,center.y,center.z,s.range,level.minY,level.maxY-1).forEach {jobs.add(Job(it))};cooldown=12
  }
  var budget=32768;var sections=0
  val cursor=BlockPos.MutableBlockPos()
  while(jobs.isNotEmpty() && budget>0 && sections++<64) {
   val job=jobs.first;val p=job.pos
   val chunk=level.chunkSource.getChunk(p.x,p.z,ChunkStatus.FULL,false)
   val index=chunk?.getSectionIndexFromSectionY(p.y) ?: -1
   val section=chunk?.sections?.getOrNull(index)
   if(section==null || section.hasOnlyAir() || !section.maybeHas {it.`is`(Blocks.POLISHED_DIORITE)}) {jobs.removeFirst();continue}
   while(job.offset<4096 && budget>0) {
    val i=job.offset++;budget--;val x=i and 15;val z=(i shr 4) and 15;val y=i shr 8
    if(!section.getBlockState(x,y,z).`is`(Blocks.POLISHED_DIORITE)) continue
    cursor.set(p.x*16+x,p.y*16+y,p.z*16+z)
    if(allowed(cursor) && candidates.size<4096) candidates.add(cursor.immutable())
   }
   if(job.offset>=4096) jobs.removeFirst()
  }
  val lines=mutableListOf<WorldOverlay.Segment>();val fills=mutableListOf<WorldOverlay.Fill>();val labels=mutableListOf<WorldOverlay.Label>()
  candidates.sortedBy {it.distSqr(center)}.take(s.maxMarkers).forEach {pos->
   val box=AABB(pos).inflate(.002);WorldOverlay.box(lines,box,s.color,!s.throughWalls)
   if(s.filled) fills.add(WorldOverlay.Fill(box,s.fillColor,!s.throughWalls))
   if(s.distance) labels.add(WorldOverlay.Label(Vec3.atCenterOf(pos).add(0.0,.7,0.0),"Titanium ${sqrt(pos.distSqr(center)).toInt()}m",s.color,!s.throughWalls))
   if(s.tracers) lines.add(WorldOverlay.Segment(c.gameRenderer.mainCamera().position().add(c.player!!.lookAngle.scale(.5)),Vec3.atCenterOf(pos),s.color,!s.throughWalls))
  }
  WorldOverlay.publish(lines,fills,labels,owner="titanium")
 }
}
