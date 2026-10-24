/* SPDX-License-Identifier: GPL-3.0-only; Stray ScoreboardHudRenderer CC0 adaptation. */
package dev.solyanka.client.feature
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
 private fun layout(sample:Boolean=false):Layout? {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.scoreboardHud
  var title:Component=Component.literal("SKYBLOCK");var rows=List(5) {Row(Component.literal(listOf("Late Spring 12th","Your Island","Purse: 12,345","Bits: 400","www.hypixel.net")[it]),Component.literal("${5-it}"))}
  if(!sample) {
   val level=c.level ?: return null;val player=c.player ?: return null;val board=level.scoreboard
   val team=board.getPlayersTeam(player.scoreboardName)
   val colored=team?.color?.orElse(null)?.displaySlot()
   val objective=colored?.let {board.getDisplayObjective(it)} ?: board.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
   title=objective.displayName
   rows=board.listPlayerScores(objective).filterNot {it.isHidden}.sortedWith(compareByDescending<PlayerScoreEntry> {it.value()}.thenBy(String.CASE_INSENSITIVE_ORDER) {it.owner()}).take(15).map {entry->
    Row(PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()),entry.display() ?: Component.literal(entry.owner())),if(s.scores) entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)) else Component.empty())
   }
   if(rows.isEmpty()) return null
  }
  val content=maxOf(c.font.width(title),rows.maxOfOrNull {c.font.width(it.name)+(if(s.scores && it.score.string.isNotBlank()) c.font.width(it.score)+8 else 0)} ?: 0)
  return Layout(title,rows,if(sample) s.maxWidth else ScoreboardLayout.width(content,s.maxWidth),ScoreboardLayout.height(rows.size,s.header))
 }
 private fun draw(g:GuiGraphicsExtractor,layout:Layout,atX:Int?=null,atY:Int?=null) {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.scoreboardHud;val scale=s.hud.scale.toFloat()
  val x=atX ?: s.hud.x.coerceIn(0,(c.window.guiScaledWidth-layout.width*scale).roundToInt().coerceAtLeast(0));val y=atY ?: s.hud.y.coerceIn(0,(c.window.guiScaledHeight-layout.height*scale).roundToInt().coerceAtLeast(0))
  g.pose().pushMatrix()
  try {
   g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale)
   if(s.background) ChromeDraw.panel(g,0,0,layout.width,layout.height,s.backgroundColor,s.borderColor,6)
   fun textClipped(text:Component,tx:Int,ty:Int,right:Int,color:Int) {
    // Scissor respects the active GUI pose, retaining styled text without overlapping scores.
    g.enableScissor(tx,ty,right,ty+10)
    try {g.text(c.font,text,tx,ty,color,s.shadow)} finally {g.disableScissor()}
   }
   if(s.header) textClipped(layout.title,11,7,layout.width-7,s.hud.color)
   var rowY=if(s.header) 21 else 7
   layout.rows.forEach {row->
    val scoreWidth=if(s.scores) c.font.width(row.score) else 0;val scoreX=layout.width-7-scoreWidth
    textClipped(row.name,11,rowY,(if(scoreWidth>0) scoreX-8 else layout.width-7).coerceAtLeast(12),s.hud.color)
    if(s.scores && scoreWidth>0) textClipped(row.score,scoreX.coerceAtLeast(11),rowY,layout.width-7,s.hud.color)
    rowY+=10
   }
  } finally {g.pose().popMatrix()}
 }
 fun register() {HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD) {original->HudElement {g,d->
  val s=S.currentGeneral().features.scoreboardHud
  if(!S.isEnabled(S.SCOREBOARD)) original.extractRenderState(g,d)
  else if(s.hud.enabled && !Minecraft.getInstance().gui.hud.isHidden && Minecraft.getInstance().gui.screen() !is dev.solyanka.client.ui.HudEditorScreen) layout()?.let {draw(g,it)}
 }}}
 fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {if(key!="extra.scoreboard") return false;draw(g,layout(true)!!,x,y);return true}
}
