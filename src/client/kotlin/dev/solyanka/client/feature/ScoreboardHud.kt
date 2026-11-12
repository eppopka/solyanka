/* SPDX-License-Identifier: GPL-3.0-only; Stray ScoreboardHudRenderer CC0 adaptation. */
package dev.solyanka.client.feature

import dev.solyanka.core.ModuleIds
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.StyledFormat
import net.minecraft.world.scores.*
import kotlin.math.roundToInt
object ScoreboardHud {
 private data class Row(val name:Component,val score:Component)
 private data class Layout(val title:Component,val rows:List<Row>,val width:Int,val height:Int)
 private fun layout():Layout? {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.scoreboardHud
  val level=c.level ?: return null;val player=c.player ?: return null;val board=level.scoreboard
  val team=board.getPlayersTeam(player.scoreboardName)
  val colored=team?.color?.orElse(null)?.displaySlot()
  val objective=colored?.let {board.getDisplayObjective(it)} ?: board.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
  val title=objective.displayName
  val rows=board.listPlayerScores(objective).filterNot {it.isHidden}.sortedWith(compareByDescending<PlayerScoreEntry> {it.value()}.thenBy(String.CASE_INSENSITIVE_ORDER) {it.owner()}).take(15).map {entry->
   Row(PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()),entry.display() ?: Component.literal(entry.owner())),if(s.scores) entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)) else Component.empty())
  }
  val content=maxOf(c.font.width(title),rows.maxOfOrNull {c.font.width(it.name)+(if(s.scores && it.score.string.isNotBlank()) c.font.width(it.score)+8 else 0)} ?: 0)
  return Layout(title,rows,ScoreboardLayout.width(content,s.maxWidth),ScoreboardLayout.height(rows.size,s.header))
 }
 /** Use live content dimensions and position for the editor's hit box. */
 fun editorEntry(entry:dev.solyanka.model.HudEntry,screenWidth:Int):dev.solyanka.model.HudEntry {
  if(entry.key!="extra.scoreboard") return entry
  return editorEntry(entry,screenWidth,layout())
 }
 private fun editorEntry(entry:dev.solyanka.model.HudEntry,screenWidth:Int,live:Layout?):dev.solyanka.model.HudEntry {
  val s=S.currentGeneral().features.scoreboardHud
  val w=live?.width ?: 160;val h=live?.height ?: 20
  val x=ScoreboardLayout.liveX(s.hud.x,screenWidth,w,s.hud.scale,s.anchorRight)
  return entry.copy(hud=entry.hud.copy(x=x),baseWidth=w,baseHeight=h,lines=listOf(if(live==null) "No active scoreboard" else "Scoreboard"))
 }
 private fun draw(g:GuiGraphicsExtractor,layout:Layout,atX:Int?=null,atY:Int?=null) {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.scoreboardHud
  val x=atX ?: ScoreboardLayout.liveX(s.hud.x,c.window.guiScaledWidth,layout.width,s.hud.scale,s.anchorRight)
  HudFrame.draw(g,s.hud,layout.width,layout.height,x,atY,ModuleIds.SCOREBOARD) {
   if(s.background) ChromeDraw.panel(g,0,0,layout.width,layout.height,s.backgroundColor,s.borderColor,6)
   fun textClipped(text:Component,tx:Int,ty:Int,right:Int,color:Int) {
    // Scissor respects the active GUI pose, retaining styled text without overlapping scores.
    g.enableScissor(tx,ty,right,ty+10)
    try {g.text(c.font,text,tx,ty,color,s.shadow)} finally {g.disableScissor()}
   }
   if(s.header) textClipped(layout.title,11,7,layout.width-7,s.hud.color)
   if(s.header) g.fill(11,18,layout.width-11,19,s.accentColor)
   var rowY=if(s.header) 21 else 7
   layout.rows.forEachIndexed {index,row->
    if(s.background && index%2==1) g.fill(6,rowY-1,layout.width-6,rowY+9,0x08ffffff)
    val scoreWidth=if(s.scores) c.font.width(row.score) else 0;val scoreX=layout.width-7-scoreWidth
    textClipped(row.name,11,rowY,(if(scoreWidth>0) scoreX-8 else layout.width-7).coerceAtLeast(12),s.hud.color)
    if(s.scores && scoreWidth>0) textClipped(row.score,scoreX.coerceAtLeast(11),rowY,layout.width-7,s.hud.color)
    rowY+=10
   }
  }
 }
 fun register() {HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD) {original->HudElement {g,d->
  val s=S.currentGeneral().features.scoreboardHud
  if(!S.isEnabled(ModuleIds.SCOREBOARD)) original.extractRenderState(g,d)
  else if(s.hud.enabled && !Minecraft.getInstance().gui.hud.isHidden && Minecraft.getInstance().gui.screen() !is dev.solyanka.client.ui.HudEditorScreen) layout()?.let {draw(g,it)}
 }}}
 /** A snapshot is scoped to a single editor frame, keeping paint and hit box in sync. */
 class EditorSnapshot internal constructor(
  private val entryFn:(dev.solyanka.model.HudEntry,Int)->dev.solyanka.model.HudEntry,
  private val paintFn:(GuiGraphicsExtractor,Int,Int)->Unit
 ) {
  fun entry(entry:dev.solyanka.model.HudEntry,width:Int)=entryFn(entry,width)
  fun draw(g:GuiGraphicsExtractor,x:Int,y:Int)=paintFn(g,x,y)
 }
 fun editorSnapshot():EditorSnapshot {
  val live=layout()
  return EditorSnapshot({entry,width->editorEntry(entry,width,live)},{g,x,y->drawEditor(g,x,y,live)})
 }
 private fun drawEditor(g:GuiGraphicsExtractor,x:Int,y:Int,current:Layout?) {
  if(current!=null) draw(g,current,x,y)
  else {
   val scale=S.currentGeneral().features.scoreboardHud.hud.scale.toFloat()
   g.pose().pushMatrix()
   try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);g.text(Minecraft.getInstance().font,"No active scoreboard",3,5,0xffc3cede.toInt(),false)} finally {g.pose().popMatrix()}
  }
 }
 fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
  if(key!="extra.scoreboard") return false
  drawEditor(g,x,y,layout());return true
 }
}
