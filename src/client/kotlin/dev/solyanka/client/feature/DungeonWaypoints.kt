/* SPDX-License-Identifier: GPL-3.0-only
 * Odin DungeonWaypoints/SecretWaypoints/pack persistence adaptation.
 * (c)2025 odtheking, BSD-3-Clause; see bundled notice. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.*
import org.lwjgl.glfw.GLFW
object DungeonWaypoints {
 private val store by lazy {WaypointStore(FabricLoader.getInstance().configDir.resolve("solyanka-waypoints"))}
 private var packs=emptyMap<String,WayPack>();private var signature="";private val clicked=mutableSetOf<String>()
 var error:String?=null;private set
 private val routeCursors=mutableMapOf<String,Int>();private var routeScope="";private var routeText:String?=null;private var routeAt=0L
 private var lastEther:BlockPos?=null;private var lastEtherTime=0L
 private fun cfg()=SolyankaClient.currentGeneral().features.waypoints
 fun reset() {routeCursors.clear();routeScope="";routeText=null;clicked.clear();lastEther=null;lastEtherTime=0;signature="";packs=emptyMap();error=null}
 fun names():List<String> = store.names()
 fun reload() {
  routeCursors.clear();routeText=null
  val next=linkedMapOf<String,WayPack>();val errors=mutableListOf<String>()
  try {if(store.names().isEmpty()) store.write("default",emptyMap(),true)} catch(e:Exception) {errors.add(e.message ?: "Cannot create default pack")}
  cfg().selectedPacks.split(',').map(String::trim).distinct().forEach {name->try {next[name]=store.load(name)} catch(e:Exception) {errors.add("$name: ${e.message}")}}
  packs=next;error=errors.takeIf {it.isNotEmpty()}?.joinToString("; ");signature=cfg().selectedPacks;DungeonPuzzles.invalidate()
 }
 fun create(name:String) {store.write(name,emptyMap(),true);reload()}
 fun rename(old:String,next:String) {store.rename(old,next);reload()}
 fun delete(name:String) {store.delete(name);reload()}
 fun import(name:String,text:String) {val pack=WaypointCodec.decode(text);store.write(name,pack,true);reload()}
 fun export(name:String)=WaypointCodec.encode(store.load(name),true)
 fun resetClicked() {routeCursors.clear();routeText=null;clicked.clear();DungeonPuzzles.invalidate()}
 private fun editable(change:(MutableMap<String,List<Waypoint>>,DungeonPuzzles.Room)->Unit) {
  require(SolyankaClient.isEnabled(SolyankaClient.WAYPOINTS)) {"Enable Dungeon Waypoints first"}
  require(cfg().allowEdits) {"Enable Allow Edits first"}
  val r=DungeonPuzzles.currentRoom() ?: error("No recognized dungeon room")
  require(FeatureRuntime.context.dungeon && !FeatureRuntime.context.boss && r.origin!=null && r.facing!=null) {"Room rotation is not ready; load all room segments"}
  val next=store.load(cfg().editPack).toMutableMap();change(next,r);store.write(cfg().editPack,next);clicked.clear();reload()
 }
 fun clearRoom()=editable {p,r->p.remove(r.name)}
 fun remove(index:Int)=editable {p,r->val rows=p[r.name]?.toMutableList() ?: error("Room has no waypoints");require(index in rows.indices);rows.removeAt(index);p[r.name]=rows}
 fun roomRows():List<Waypoint> = DungeonPuzzles.currentRoom()?.let {store.load(cfg().editPack)[it.name]} ?: emptyList()
 fun addTarget(title:String="",toggle:Boolean=false) {
  require(title.length<=128 && '\n' !in title && '\r' !in title)
  val c=Minecraft.getInstance();val hit=c.hitResult as? BlockHitResult ?: error("Look at a block first")
  require(hit.type==HitResult.Type.BLOCK) {"Look at a block within normal reach"}
  val pos=hit.blockPos;val level=c.level ?: error("Not in a world")
  require(level.hasChunkAt(pos));val settings=cfg()
  editable {p,r->
   val rel=DungeonLogic.relative(GridPos(pos.x,pos.y,pos.z),r.origin!!,r.facing!!);val list=p[r.name].orEmpty().toMutableList();val old=list.indexOfFirst {it.blockPos==rel}
   if(old>=0) list.removeAt(old)
   if(!toggle || old<0) {
    val shape=level.getBlockState(pos).getShape(level,pos)
    val b=if(settings.blockSize && !shape.isEmpty) shape.bounds() else AABB((1-settings.sizeX)/2,(1-settings.sizeY)/2,(1-settings.sizeZ)/2,(1+settings.sizeX)/2,(1+settings.sizeY)/2,(1+settings.sizeZ)/2)
    val inverse=when(r.facing) {RoomFacing.WEST->RoomFacing.EAST;RoomFacing.EAST->RoomFacing.WEST;else->r.facing!!}
    val box=WaypointLogic.box(WayBox(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ),GridPos(0,0,0),GridPos(0,0,0),inverse)
    list.add(Waypoint(rel,settings.color,settings.filled,settings.depth,box,title,WayType.entries[settings.type]))
   }
   require(list.size<=2000);p[r.name]=list
  }
 }
 private fun key(pack:String,room:String,index:Int,instance:DungeonPuzzles.Room)="$pack:$room:$index:${instance.origin}"
 fun geometry(r:DungeonPuzzles.Room?,lines:MutableList<WorldOverlay.Segment>,fills:MutableList<WorldOverlay.Fill>,labels:MutableList<WorldOverlay.Label>) {
  routeText=null;routeAt=System.nanoTime()
  if(!SolyankaClient.isEnabled(SolyankaClient.WAYPOINTS) || r?.origin==null || r.facing==null) return
  if(signature!=cfg().selectedPacks) reload()
  val s=cfg();val route=SolyankaClient.currentGeneral().features.route
  val scope="${r.name}:${r.origin}:${r.facing}";if(scope!=routeScope){routeCursors.clear();routeScope=scope}
  val player=Minecraft.getInstance().player
  packs.forEach {(pack,rooms)->
   val ways=rooms[r.name].orEmpty();val entries=RouteLogic.entries(ways)
   if(entries.isNotEmpty() && player!=null) {
    fun point(e:RouteLogic.Entry)=Vec3.atCenterOf(r.real(ways[e.waypoint].blockPos)!!)
    val near=entries.filter {point(it).distanceToSqr(player.position())<=route.radius*route.radius}.map {it.waypoint}.toSet()
    val cursor=RouteLogic.advance(entries,routeCursors[pack] ?: -1,near);routeCursors[pack]=cursor
    if(route.progress && routeText==null)routeText="Route: ${if(cursor>=0) (if(entries[cursor].number==0) "START" else entries[cursor].number.toString()) else "—"}/${entries.maxOf {it.number}}"
    val remaining=if(route.skipCollected)entries.filter {key(pack,r.name,it.waypoint,r) !in clicked} else entries
    val current=if(cursor<0)-1 else remaining.indexOfLast {it.number<=entries[cursor].number}
    if(route.lines)RouteLogic.segments(remaining,current,route).forEach {(a,b)->
     val color=if(route.rainbow) (java.awt.Color.HSBtoRGB(((System.nanoTime()/4_000_000_000.0)%1).toFloat(),.75f,1f) and 0xffffff) or (route.color and -0x1000000) else route.color
     lines.add(WorldOverlay.Segment(point(a),point(b),color,!route.throughWalls,route.width.toFloat()))
    }
   }
  }
  packs.forEach { (pack,rooms)->rooms[r.name]?.forEachIndexed {i,w->
   if(key(pack,r.name,i,r) in clicked) return@forEachIndexed
   val b=WaypointLogic.box(w.aabb,w.blockPos,r.origin,r.facing);val box=AABB(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);val depth=w.depth && !s.globalEsp
   if(w.filled) fills.add(WorldOverlay.Fill(box,w.color,depth)) else WorldOverlay.box(lines,box,w.color,depth)
   if(s.titles && w.title.isNotBlank()) {val p=r.real(w.blockPos)!!;labels.add(WorldOverlay.Label(Vec3.atCenterOf(p).add(0.0,.1*s.titleScale,0.0),w.title,w.color,depth,s.titleScale.toFloat()))}
  }}
 }
 fun secret(pos:BlockPos,maxSquared:Int,type:WayType=WayType.SECRET) {
  if(!SolyankaClient.isEnabled(SolyankaClient.WAYPOINTS) || !FeatureRuntime.context.dungeon || FeatureRuntime.context.boss) return
  val r=DungeonPuzzles.currentRoom() ?: return;if(r.origin==null || r.facing==null)return
  val relative=DungeonLogic.relative(GridPos(pos.x,pos.y,pos.z),r.origin,r.facing)
  data class Candidate(val key:String,val distance:Long)
  val near=packs.flatMap {(pack,rooms)->rooms[r.name].orEmpty().mapIndexedNotNull {i,w->
   val k=key(pack,r.name,i,r);val dx=(w.blockPos.x-relative.x).toLong();val dy=(w.blockPos.y-relative.y).toLong();val dz=(w.blockPos.z-relative.z).toLong();val d=dx*dx+dy*dy+dz*dz
   if(w.type==type && k !in clicked && d<=maxSquared) Candidate(k,d) else null
  }}.minByOrNull {it.distance}
  if(near!=null) {clicked.add(near.key);DungeonPuzzles.invalidate()}
 }
 fun editorInput(device:InputDevice,key:Int):Boolean {
  val c=Minecraft.getInstance()
  if(device!=InputDevice.MOUSE || key!=GLFW.GLFW_MOUSE_BUTTON_RIGHT || c.gui.screen()!=null || !SolyankaClient.isEnabled(SolyankaClient.WAYPOINTS) || !FeatureRuntime.context.dungeon || FeatureRuntime.context.boss) return false
  // Ray-cast only a manually initiated etherwarp; never use, aim or teleport automatically.
  val player=c.player ?: return false
  val id=player.mainHandItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","")
  if(id in setOf("ASPECT_OF_THE_VOID","ASPECT_OF_THE_END") && player.isCrouching) {
   val tuned=player.mainHandItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)?.copyTag()?.getIntOr("tuned_transmission",0) ?: 0
   val target=player.pick((56+tuned.coerceIn(0,10)).toDouble(),0f,false) as? BlockHitResult
   if(target?.type==HitResult.Type.BLOCK) {lastEther=target.blockPos.immutable();lastEtherTime=System.nanoTime()}
  }
  if(!cfg().allowEdits) return false
  try {addTarget(toggle=true)} catch(e:Exception) {player.sendSystemMessage(Component.literal("[solyanka] ${e.message}"))}
  return true
 }
 fun pickup(e:net.minecraft.world.entity.item.ItemEntity) {val player=Minecraft.getInstance().player ?: return;if(WaypointLogic.secretDrop(e.item.hoverName.string) && e.distanceTo(player)<=6) secret(e.blockPosition(),3)}
 fun teleport(pos:Vec3) {val target=lastEther ?: return;if(System.nanoTime()-lastEtherTime<=1_000_000_000L && pos.distanceTo(Vec3.atLowerCornerOf(target))<=3) {secret(target,0,WayType.ETHERWARP);lastEther=null;lastEtherTime=0}}
 fun register() {
  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,net.minecraft.resources.Identifier.fromNamespaceAndPath("solyanka","route")) {g,_->
   val c=Minecraft.getInstance();val s=SolyankaClient.currentGeneral().features.route;val h=s.hud;val text=routeText
   if(text!=null && System.nanoTime()-routeAt<500_000_000L && s.progress && h.enabled && SolyankaClient.isEnabled(SolyankaClient.WAYPOINTS) && FeatureRuntime.context.dungeon && !FeatureRuntime.context.boss && c.level!=null && !c.gui.hud.isHidden && c.gui.screen() !is dev.solyanka.client.ui.HudEditorScreen) {
    g.pose().pushMatrix();try{g.pose().translate(h.x.toFloat(),h.y.toFloat());g.pose().scale(h.scale.toFloat(),h.scale.toFloat());g.text(c.font,text,0,0,h.color,true)}finally{g.pose().popMatrix()}
   }
  }
  UseBlockCallback.EVENT.register {player,level,hand,hit ->
  if(level.isClientSide && player===Minecraft.getInstance().player && !cfg().allowEdits) {
   val block=level.getBlockState(hit.blockPos).block
   val skull=if(block is net.minecraft.world.level.block.SkullBlock) (level.getBlockEntity(hit.blockPos) as? net.minecraft.world.level.block.entity.SkullBlockEntity)?.ownerProfile?.partialProfile()?.id()?.toString() else null
   if(block==Blocks.CHEST || block==Blocks.TRAPPED_CHEST || block==Blocks.LEVER || skull in setOf("2865274b-3097-394e-8149-ec629c72d850","fed95410-aba1-39df-9b95-1d4f361eb66e")) secret(hit.blockPos,0)
  }
  InteractionResult.PASS
 } }
}
