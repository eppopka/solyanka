/* SPDX-License-Identifier: GPL-3.0-only; impl.zip SimonSays adaptation, manual solver. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.event.player.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.*
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component

object SimonSolver {
 private val logic=SimonLogic()
 private val cells=(120..123).flatMap {y->(92..95).map {z->BlockPos(111,y,z)}}
 private val buttons=cells.map {it.west()}
 private val check=BlockPos(110,120,93);private val start=BlockPos(110,121,91)
 private var phase=false;private var monitoring=false;private var tick=0L;private var began=-1L
 private var quiet=0;private var canBreak=false;private var broken=false
 fun reset() {logic.reset();phase=false;monitoring=false;tick=0;began=-1;quiet=0;canBreak=false;broken=false;WorldOverlay.clear("simon")}
 private fun eligible()=S.isEnabled(S.SIMON) && FeatureRuntime.context.let {it.dungeon && it.floor==7 && it.boss} && Minecraft.getInstance().level!=null && Minecraft.getInstance().player!=null
 private fun active()=eligible() && phase
 fun observe(pos:BlockPos,state:BlockState) {
  if(!active())return;val level=Minecraft.getInstance().level ?: return
  if(state==level.getBlockState(pos))return
  val index=cells.indexOf(pos)
  if(index>=0 && state.`is`(Blocks.SEA_LANTERN))logic.flash(index,S.currentGeneral().features.simon.skip)
  if(pos==check) {if(state.`is`(Blocks.AIR))logic.check(false) else if(state.`is`(Blocks.STONE_BUTTON))logic.check(true)}
 }
 private fun click(pos:BlockPos):Boolean {
  if(!active())return false
  val c=Minecraft.getInstance();val level=c.level ?: return false
  if(pos==start) {logic.reset();monitoring=true;if(began<0)began=tick;return false}
  val i=buttons.indexOf(pos);if(i<0)return false
  val state=level.getBlockState(pos);if(!state.`is`(Blocks.STONE_BUTTON) || state.getValue(ButtonBlock.POWERED))return false
  return logic.click(i,tick,S.currentGeneral().features.simon.blockWrong,c.player?.isCrouching==true)
 }
 private fun message(raw:String,overlay:Boolean) {
  if(overlay || !eligible())return
  val text=ChatLogic.plain(raw)
  if(text=="[BOSS] Goldor: Who dares trespass into my domain?") {reset();phase=true;monitoring=true;began=0;return}
  if(!phase)return
  if(text.startsWith("[BOSS] Goldor:") && (text.contains("The Core entrance is opening") || text.contains("You have done it"))) {reset();return}
  val m=Regex("(.+) (activated|completed) a (terminal|device|lever)! \\((\\d+)/(\\d+)\\)").find(text) ?: return
  if(m.groupValues[3]=="device") {
   val p=Minecraft.getInstance().level?.players()?.firstOrNull {it.gameProfile.name==m.groupValues[1]} ?: return
   if(p.position().distanceToSqr(Vec3(110.5,121.5,93.5))<=25) {
    if(began>=0)local("Simon Says completed in ${"%.2f".format(java.util.Locale.ROOT,(tick-began)/20.0)}s")
    logic.reset();monitoring=false
   }
  }
 }
 private fun local(t:String) {Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[solyanka] $t"))}
 private fun alert(t:String) {
  val s=S.currentGeneral().features.simon;if(!s.alerts)return
  local(t)
  val p=Minecraft.getInstance().player ?: return
  if(s.sound)p.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND,1f,.8f)
  if(s.partyAlerts)p.connection.sendCommand("pc $t")
 }
 fun tick() {
  if(!eligible()) {if(phase)reset();return};if(!phase)return
  tick++;val level=Minecraft.getInstance().level ?: return;val s=S.currentGeneral().features.simon
  if(monitoring && (cells+buttons).all {level.hasChunkAt(it)}) {
   quiet--
   if(cells.any {!level.getBlockState(it).`is`(Blocks.OBSIDIAN)}) {
    quiet=12;canBreak=true;if(broken){broken=false;alert("SS Started Again!")}
   } else if(quiet<=0 && canBreak && buttons.all {level.getBlockState(it).`is`(Blocks.AIR)}) {
    canBreak=false;broken=true;alert("SS Broke!");logic.reset()
   }
  }
  val lines=mutableListOf<WorldOverlay.Segment>();val fills=mutableListOf<WorldOverlay.Fill>();val labels=mutableListOf<WorldOverlay.Label>()
  logic.pending.groupBy {it.cell}.forEach {(i,steps)->
   val index=logic.pending.indexOf(steps.first());val color=when(index){0->s.first;1->s.second;else->s.other};val p=buttons[i]
   if(level.hasChunkAt(p)) {
    val state=level.getBlockState(p);val depth=if(state.`is`(Blocks.STONE_BUTTON) && state.getValue(ButtonBlock.POWERED))1.0/16 else 2.0/16
    val box=AABB(p.x+1-depth,p.y+.375,p.z+.3125,p.x+1.002,p.y+.625,p.z+.6875)
    if(s.outline)WorldOverlay.box(lines,box,color,!s.throughWalls) else fills.add(WorldOverlay.Fill(box,color,!s.throughWalls))
    if(s.numbers)labels.add(WorldOverlay.Label(Vec3.atLowerCornerOf(p).add(.8,.6,.5),steps.joinToString(", "){it.number.toString()},s.numberColor,!s.throughWalls))
   }
  };WorldOverlay.publish(lines,fills,labels,"simon")
 }
 fun register() {
  ClientReceiveMessageEvents.GAME.register {m,o->message(m.string,o)};ClientReceiveMessageEvents.GAME_CANCELED.register {m,o->message(m.string,o)}
  UseBlockCallback.EVENT.register {p,l,_,h->if(l.isClientSide && p===Minecraft.getInstance().player && click(h.blockPos)) InteractionResult.FAIL else InteractionResult.PASS}
  AttackBlockCallback.EVENT.register {p,l,_,pos,_->if(l.isClientSide && p===Minecraft.getInstance().player && click(pos)) InteractionResult.FAIL else InteractionResult.PASS}
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","simon")) {g,_->
   val c=Minecraft.getInstance();val s=S.currentGeneral().features.simon;val h=s.hud
   if(active() && s.progress && h.enabled && logic.stage>0 && !c.gui.hud.isHidden && c.gui.screen() !is dev.solyanka.client.ui.HudEditorScreen) {
    g.pose().pushMatrix();try {g.pose().translate(h.x.toFloat(),h.y.toFloat());g.pose().scale(h.scale.toFloat(),h.scale.toFloat());g.text(c.font,"SS: ${logic.stage}/5",0,0,h.color,true)}finally{g.pose().popMatrix()}
   }
  }
 }
}
