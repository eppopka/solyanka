/* SPDX-License-Identifier: GPL-3.0-only
 * Odin DoorHighlight/MapInfo adapters; (c)2025 odtheking, BSD-3-Clause. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.resources.Identifier
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
object DungeonMap {
 val keys=DoorKeys();val stats=MapStats()
 private var doors=emptyList<MapDoor>();private val announced=mutableSetOf<Int>();private var shown300=false
 private var wasDungeon=false;private var floor:Int?=null;private var master=false
 private var roomSecrets:Pair<Int,Int>?=null;private var roomId="";private var waitingRoomSample=true
 private var portal:AABB?=null
 var score:DungeonScore?=null;private set
 fun reset() {keys.reset();stats.reset();doors=emptyList();announced.clear();shown300=false;score=null;roomSecrets=null;roomId="";waitingRoomSample=true;portal=null;wasDungeon=false;floor=null;master=false}
 fun tick() {
  val c=Minecraft.getInstance();val ctx=FeatureRuntime.context
  if(!ctx.dungeon) {if(wasDungeon) reset();return}
  if(wasDungeon && (ctx.floor!=floor || ctx.master!=master)) reset()
  wasDungeon=true;floor=ctx.floor;master=ctx.master
  if(!SolyankaClient.isEnabled(SolyankaClient.MAP_INFO) && !SolyankaClient.isEnabled(SolyankaClient.DOORS)) return
  val lines=c.connection?.listedOnlinePlayers?.mapNotNull {it.tabListDisplayName?.string} ?: emptyList()
  val board=c.level!!.scoreboard;val obj=board.getDisplayObjective(DisplaySlot.SIDEBAR)
  val sidebar=obj?.let {board.listPlayerScores(it).map {s->PlayerTeam.formatNameForTeam(board.getPlayersTeam(s.owner()),s.ownerName()).string}} ?: emptyList()
  stats.update(lines,sidebar)
  val cfg=SolyankaClient.currentGeneral().features.map
  val bonus=(stats.crypts ?: 0).coerceAtMost(5)+(if(DungeonPuzzles.mimicKilled)2 else 0)+(if(DungeonPuzzles.princeKilled)1 else 0)+DungeonPuzzles.batsKilled+(if(cfg.paul)10 else 0)
  score=stats.score(ctx.floor,ctx.master,ctx.boss,bonus)
  val s=score!!
  if(SolyankaClient.isEnabled(SolyankaClient.MAP_INFO) && cfg.alert300 && s.completeInputs && s.value>=300 && !shown300) {
   shown300=true;c.player?.sendSystemMessage(Component.literal("[solyanka] Estimated ${s.value} score in ${stats.elapsed}. Check tab/Paul/bonus state before entering boss."));c.gui.hud.setOverlayMessage(Component.literal("300 Score (estimate)"),false)
  }
  if(!ctx.boss) {
   val inventory=c.player!!.inventory
   for(i in 0 until inventory.containerSize) {
    val item=inventory.getItem(i)
    if(item.isEmpty || !ChatLogic.plain(item.hoverName.string).contains("Magical Map")) continue
    val data=MapItem.getSavedData(item,c.level!!) ?: continue
    DungeonMapLogic.doors(data.colors)?.let {doors=it};break
   }
  }
  val r=DungeonPuzzles.currentRoom()
  val id=r?.let {"${it.name}:${it.tx}:${it.tz}"} ?: ""
  if(id!=roomId) {roomId=id;roomSecrets=null;waitingRoomSample=true}
  if(r?.name=="Blood" && r.origin!=null) {val a=r.real(GridPos(14,69,29));val b=r.real(GridPos(16,69,29));if(a!=null && b!=null) portal=AABB.encapsulatingFullBlocks(a,b).inflate(0.0,4.0,0.0)}
 }
 fun geometry(lines:MutableList<WorldOverlay.Segment>,fills:MutableList<WorldOverlay.Fill>) {
  val c=Minecraft.getInstance();val level=c.level ?: return;val ctx=FeatureRuntime.context
  if(!ctx.dungeon || ctx.boss) return
  if(SolyankaClient.isEnabled(SolyankaClient.DOORS)) {
   doors.forEach {d ->
    if(d.blood && keys.bloodOpened) return@forEach
    // Once its loaded center is air, do not render an already-opened map-stale door.
    val pos=BlockPos(d.x,70,d.z)
    if(level.hasChunkAt(pos) && level.getBlockState(pos).isAir) return@forEach
    val open=if(d.blood) keys.blood else keys.wither>0;val color=if(open)0xff55ff55.toInt() else 0xffff5555.toInt()
    val box=AABB(d.x-1.0,69.0,d.z-1.0,d.x+2.0,73.0,d.z+2.0)
    WorldOverlay.box(lines,box,color,false);fills.add(WorldOverlay.Fill(box,(color and 0xffffff) or 0x60000000,false))
   }
   level.entitiesForRendering().filterIsInstance<ArmorStand>().forEach {e ->
    val name=ChatLogic.plain(e.name.string)
    if(e.isAlive && name in setOf("Wither Key","Blood Key")) {
     val color=if(name=="Blood Key")0xffff5555.toInt() else 0xff555555.toInt();val b=AABB.unitCubeFromLowerCorner(e.position().add(-.5,1.0,-.5))
     WorldOverlay.box(lines,b,color,true);fills.add(WorldOverlay.Fill(b,(color and 0xffffff) or 0x60000000,true))
     if(announced.add(e.id) && SolyankaClient.currentGeneral().features.map.announceKeySpawn) c.gui.hud.setOverlayMessage(Component.literal("$name spawned!"),false)
    }
   }
  }
  val s=score
  if(SolyankaClient.isEnabled(SolyankaClient.MAP_INFO) && SolyankaClient.currentGeneral().features.map.highlightPortal && s?.completeInputs==true && s.value>=300) portal?.let {fills.add(WorldOverlay.Fill(it,0x8055ff55.toInt(),true))}
 }
 fun register() {
  ClientReceiveMessageEvents.GAME.register {message,overlay ->
   if(FeatureRuntime.context.dungeon) {
    if(!overlay) {if(SolyankaClient.isEnabled(SolyankaClient.DOORS)) keys.message(message.string);if(SolyankaClient.isEnabled(SolyankaClient.MAP_INFO)) stats.message(message.string)}
    else if(SolyankaClient.isEnabled(SolyankaClient.MAP_INFO)) {
     Regex("([0-9]+)/([0-9]+) Secrets").find(ChatLogic.plain(message.string))?.let {m->
      val found=m.groupValues[1].toIntOrNull();val max=m.groupValues[2].toIntOrNull()
      if(found!=null && max!=null && found<=max) {if(waitingRoomSample) waitingRoomSample=false else roomSecrets=found to max}
     }
    }
   }
  }
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","map_info")) {g,_ ->
   val c=Minecraft.getInstance();val ctx=FeatureRuntime.context;val cfg=SolyankaClient.currentGeneral().features.map
   if(SolyankaClient.isEnabled(SolyankaClient.MAP_INFO) && ctx.dungeon && !(cfg.disableInBoss && ctx.boss) && !c.gui.hud.isHidden && c.player!=null && c.gui.screen() !is dev.solyanka.client.ui.HudEditorScreen) {
    val s=score
    val text=listOf("Secrets: ${stats.secrets ?: "?"}/${s?.totalSecrets ?: "?"} | ${if(cfg.remaining) "Need: ${s?.neededSecrets?.let {(it-(stats.secrets ?: 0)).coerceAtLeast(0)} ?: "?"} | " else ""}Score${if(s?.completeInputs==true) "~" else "?"}: ${s?.value ?: "?"}",
     "Deaths: ${stats.deaths ?: "?"} | Crypts: ${stats.crypts ?: "?"} | M:${DungeonPuzzles.mimicKilled} P:${DungeonPuzzles.princeKilled} B:${DungeonPuzzles.batsKilled}",
     "${DungeonPuzzles.currentRoom()?.name ?: "Unknown room"}${if(cfg.roomSecrets && roomSecrets!=null) " | Room secrets: ${roomSecrets!!.first}/${roomSecrets!!.second}" else ""} | ${stats.elapsed}")
    val x=cfg.x.coerceAtMost((c.window.guiScaledWidth-text.maxOf {c.font.width(it)}).coerceAtLeast(0));val y=cfg.y.coerceAtMost((c.window.guiScaledHeight-30).coerceAtLeast(0))
    text.forEachIndexed {i,t->g.text(c.font,Component.literal(t),x,y+i*10,-1,true)}
   }
  }
 }
}
