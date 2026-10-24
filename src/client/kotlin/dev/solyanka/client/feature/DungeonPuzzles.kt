/* SPDX-License-Identifier: GPL-3.0-only
 * Ports Odin WorldScan/RoomRotation/IceFillSolver/BlazeSolver/BeamsSolver/Mimic;
 * (c)2025 odtheking BSD-3-Clause. Mimic Chest adapted from supplied impl.zip. */
package dev.solyanka.client.feature
import com.google.gson.*
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.DyeColor
import net.minecraft.world.phys.*
import net.minecraft.network.chat.Component
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import org.slf4j.LoggerFactory

object DungeonPuzzles {
 private val log=LoggerFactory.getLogger("solyanka/dungeons")
 data class Room(val name:String,val tx:Int,val tz:Int,val height:Int,val origin:GridPos?,val facing:RoomFacing?) {
  fun real(p:GridPos)=if(origin!=null && facing!=null) DungeonLogic.real(p,origin,facing).let {BlockPos(it.x,it.y,it.z)} else null
 }
 private val gson=Gson()
 private fun resource(name:String):JsonElement = javaClass.getResourceAsStream("/assets/solyanka/$name")?.use { it.reader().use(JsonParser::parseReader) } ?: error("Missing dungeon resource $name")
 private val definitions by lazy {resource("rooms.json").asJsonArray.flatMap { value->val o=value.asJsonObject;o.getAsJsonArray("cores").map {it.asInt to o} }.toMap()}
 private val ice by lazy {resource("puzzles/ice-fill-floors.json").asJsonObject}
 private val beams by lazy {resource("puzzles/creeper-beams-solutions.json").asJsonArray.map {it.asJsonArray.map(JsonElement::getAsInt)}}
 private val colors=intArrayOf(0xffffaa00.toInt(),0xff55ff55.toInt(),0xffff55ff.toInt(),0xff00aaaa.toInt(),0xffffff55.toInt(),0xffaa0000.toInt(),-1,0xffaa00aa.toInt())
 private var ticks=0
 private var room:Room?=null
 private data class Tile(val data:JsonObject,val height:Int)
 private val tiles=mutableMapOf<Pair<Int,Int>,Tile>()
 fun invalidate() {WorldOverlay.publish(emptyList());chestCountdown=0}
 private val deadTags=mutableSetOf<Int>()
 var mimicKilled=false;private set
 var princeKilled=false;private set
 var batsKilled=0;private set
 private var chestCountdown=0
 private val chestPositions=mutableSetOf<BlockPos>()
 fun currentRoom()=room
 fun reset() { ticks=0;room=null;tiles.clear();deadTags.clear();mimicKilled=false;princeKilled=false;batsKilled=0;chestCountdown=0;chestPositions.clear();WorldOverlay.publish(emptyList()) }
 private fun scan():Room? {
  val client=Minecraft.getInstance();val level=client.level ?: return null;val player=client.player ?: return null
  val tile=DungeonLogic.tile(player.blockX,player.blockZ) ?: return null
  // At most 36 loaded columns (129 blocks each), once per second. Unknown columns retry.
  if(ticks%20==0 || tile !in tiles) for(tz in 0..5) for(tx in 0..5) {
   val key=tx to tz;if(key in tiles) continue
   val cx=tx*32-185;val cz=tz*32-185;if(!level.hasChunkAt(BlockPos(cx,70,cz))) continue
   val column=(140 downTo 12).map { y->y to BuiltInRegistries.BLOCK.getKey(level.getBlockState(BlockPos(cx,y,cz)).block).toString() }
   val (core,height)=DungeonLogic.core(column);val data=definitions[core] ?: continue;tiles[key]=Tile(data,height)
  }
  val t=tiles[tile] ?: return null;val data=t.data;val height=t.height
  if(player.blockY>height-10) return null
  val name=data.get("name").asString;val shape=data.get("shape").asString
  val cx=tile.first*32-185;val cz=tile.second*32-185
  if(shape=="1x1") {
   val rot=if(name=="Fairy") RoomFacing.SOUTH else RoomFacing.entries.firstOrNull { f -> val probe=BlockPos(cx+f.dx,height,cz+f.dz);level.hasChunkAt(probe) && level.getBlockState(probe).block==Blocks.DYED_TERRACOTTA.pick(DyeColor.BLUE) }
   return Room(name,tile.first,tile.second,height,rot?.let { GridPos(cx+it.dx,0,cz+it.dz) },rot)
  }
  val segments=tiles.filterValues {it.data.get("name").asString==name}.keys
  val geometry=WaypointLogic.geometry(segments,shape,height)
  return Room(name,tile.first,tile.second,height,geometry?.origin,geometry?.facing)
 }
 fun tick() {
  if(++ticks%5!=0) return
  val client=Minecraft.getInstance();val level=client.level ?: return;val ctx=FeatureRuntime.context
  val active=listOf(SolyankaClient.ICE,SolyankaClient.BLAZE,SolyankaClient.BEAMS,SolyankaClient.MIMIC,SolyankaClient.MIMIC_CHEST,SolyankaClient.DOORS,SolyankaClient.MAP_INFO,SolyankaClient.WAYPOINTS).any(SolyankaClient::isEnabled)
  if(!ctx.dungeon || !active || ctx.boss && !SolyankaClient.isEnabled(SolyankaClient.MIMIC_CHEST)) {room=null;WorldOverlay.publish(emptyList());return}
  room=if(ctx.boss) null else scan()
  val cfg=SolyankaClient.currentGeneral().features.puzzles;val lines=mutableListOf<WorldOverlay.Segment>();val fills=mutableListOf<WorldOverlay.Fill>();val labels=mutableListOf<WorldOverlay.Label>();val current=room
  if(current?.name=="Ice Fill" && current.origin!=null && SolyankaClient.isEnabled(SolyankaClient.ICE)) {
   val ids=ice.getAsJsonArray("identifier");val patterns=ice.getAsJsonArray(if(cfg.optimizeIce) "hard" else "easy")
   val route=mutableListOf<Vec3>();var identified=0
   for(floor in 0..2) {
    val variants=ids[floor].asJsonArray
    for(index in 0 until variants.size()) {
     val pair=variants[index].asJsonArray.map {current.real(gson.fromJson(it,GridPos::class.java))!!}
     if(pair.all(level::hasChunkAt) && level.getBlockState(pair[0]).isAir && !level.getBlockState(pair[1]).isAir) {
      identified++;route.addAll(patterns[floor].asJsonArray[index].asJsonArray.map {current.real(gson.fromJson(it,GridPos::class.java))!!.let {p->Vec3(p.x+0.5,p.y+0.1,p.z+0.5)}});break
     }
    }
   }
   if(identified==3) route.zipWithNext().forEach {(a,b)->lines.add(WorldOverlay.Segment(a,b,0xff55ffff.toInt(),cfg.iceDepthTest))}
  }
  if(current?.name in setOf("Higher Blaze","Lower Blaze") && SolyankaClient.isEnabled(SolyankaClient.BLAZE)) {
   val candidates=level.entitiesForRendering().filterIsInstance<ArmorStand>().filter { it.isAlive && it.id !in deadTags && DungeonLogic.tile(it.blockX,it.blockZ)==(current!!.tx to current.tz) }
    .mapNotNull { e -> DungeonLogic.blazeHealth(e.name.string)?.let {hp->e to hp} }.sortedBy {if(current?.name=="Lower Blaze") -it.second else it.second}
   var previous:Vec3?=null
   candidates.forEachIndexed { i,(e,_) -> val b=e.boundingBox.inflate(0.5,1.0,0.5).move(0.0,-1.0,0.0);val color=when(i){0->0xff55ff55.toInt();1->0xffffff55.toInt();2->0xffff5555.toInt();else->-1}
    WorldOverlay.box(lines,b,color,cfg.blazeDepthTest);if(cfg.blazeLines && previous!=null && i<=3) lines.add(WorldOverlay.Segment(previous!!,b.center,color,cfg.blazeDepthTest));previous=b.center
   }
  }
  if(current?.name=="Creeper Beams" && current.origin!=null && SolyankaClient.isEnabled(SolyankaClient.BEAMS)) beams.forEachIndexed { i,v ->
   val a=current.real(GridPos(v[0],v[1],v[2]))!!;val b=current.real(GridPos(v[3],v[4],v[5]))!!
   if(level.hasChunkAt(a) && level.hasChunkAt(b) && level.getBlockState(a).block==Blocks.SEA_LANTERN && level.getBlockState(b).block==Blocks.SEA_LANTERN) {
    val color=colors[i%colors.size];WorldOverlay.box(lines,AABB(a),color,cfg.beamDepthTest);WorldOverlay.box(lines,AABB(b),color,cfg.beamDepthTest)
    if(cfg.beamLines) lines.add(WorldOverlay.Segment(Vec3.atCenterOf(a),Vec3.atCenterOf(b),color,false))
   }
  }
  if(!ctx.boss && SolyankaClient.isEnabled(SolyankaClient.MIMIC) && !mimicKilled && ctx.floor in setOf(6,7)) level.entitiesForRendering().filterIsInstance<Zombie>().filter {it.isBaby && it.isAlive}.forEach { WorldOverlay.box(lines,it.boundingBox.inflate(0.08),0xffff5555.toInt(),true) }
  if(SolyankaClient.isEnabled(SolyankaClient.MIMIC_CHEST)) {
   chestCountdown-=5
   if(chestCountdown<=0) {
    chestCountdown=20
    chestPositions.clear();val player=client.player!!;val cx=player.blockX shr 4;val cz=player.blockZ shr 4;val distance=client.options.effectiveRenderDistance
    for(x in cx-distance..cx+distance) for(z in cz-distance..cz+distance) {
     val chunk=level.chunkSource.getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false) ?: continue
     chunk.blockEntities.forEach { (pos,entity) ->if(entity.blockState.block==Blocks.TRAPPED_CHEST) chestPositions.add(pos.immutable()) }
    }
   }
   chestPositions.forEach {pos ->if(level.hasChunkAt(pos)) {val state=level.getBlockState(pos);if(state.block==Blocks.TRAPPED_CHEST) {val shape=state.getShape(level,pos);if(!shape.isEmpty) fills.add(WorldOverlay.Fill(shape.bounds().move(pos).inflate(.002),cfg.mimicChestColor,true))} } }
  }
  DungeonMap.geometry(lines,fills);DungeonWaypoints.geometry(current,lines,fills,labels)
  WorldOverlay.publish(lines,fills,labels)
 }
 fun event(e:Entity?,id:Int) {
  val ctx=FeatureRuntime.context
  if(!ctx.dungeon || ctx.boss || id!=3) return
  if(e is Blaze) deadTags.add(e.id+1)
  if(SolyankaClient.isEnabled(SolyankaClient.MIMIC) && DungeonLogic.mimicDeath(id,e is Zombie && e.isBaby,ctx.floor,true,mimicKilled)) {
   mimicKilled=true;message("Mimic Killed!",SolyankaClient.currentGeneral().features.puzzles.announceMimic)
  }
 }
 private fun message(text:String,party:Boolean) {val p=Minecraft.getInstance().player ?: return;p.sendSystemMessage(Component.literal("[solyanka] $text"));if(party) p.connection.sendCommand("pc $text")}
 fun register() { WorldOverlay.register();ClientReceiveMessageEvents.GAME.register {component,overlay ->
  if(!overlay && (SolyankaClient.isEnabled(SolyankaClient.MIMIC) || SolyankaClient.isEnabled(SolyankaClient.MAP_INFO)) && FeatureRuntime.context.dungeon && !FeatureRuntime.context.boss) {
   val cfg=SolyankaClient.currentGeneral().features.puzzles
   when(ChatLogic.plain(component.string)) {
    "A Prince falls. +1 Bonus Score" ->if(!princeKilled) {princeKilled=true;message("Prince Killed!",cfg.announcePrince)}
    "A Bat has been slain. +1 Bonus Score" ->{batsKilled++;message("Bat Killed!",cfg.announceBat)}
   }
  }
 } }
}
