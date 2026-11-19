/* SPDX-License-Identifier: GPL-3.0-only
 * Layout/colors adapted from user-supplied Odin PerformanceHUD.kt (BSD-3-Clause); see NOTICE. */
package dev.solyanka.client.feature

import dev.solyanka.core.ModuleIds

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.model.HudEntry
import dev.solyanka.utility.ServerTpsEstimator
import dev.solyanka.utility.PerformanceText
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

object PerformanceHud {
 private val tps=ServerTpsEstimator(staleNanos=15_000_000_000L)
 fun reset()=tps.reset()
 fun observe(ticks:Long) {tps.observe(ticks,System.nanoTime(),cadenceFallback=!Minecraft.getInstance().hasSingleplayerServer())}
 fun status():String {
  val value=tps.value(System.nanoTime())
  val state=if(value==null) "Waiting for fresh server time updates (need 2 packets)" else
      if(tps.usingPacketCadence) "TPS estimate: time-packet cadence (server clock not advancing)" else "TPS estimate: server game ticks / elapsed time"
  return "$state · ${tps.updates} packets"
 }
 private fun metrics(sample:Boolean=false):List<dev.solyanka.utility.PerformanceMetric> {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.performance
  val ping=if(sample) "42 ms" else if(c.hasSingleplayerServer()) "Local" else c.player?.let {c.connection?.getPlayerInfo(it.uuid)?.latency}?.takeIf {it>=0}?.let {"$it ms"} ?: "—"
  return dev.solyanka.utility.PerformanceLayout.metrics(s,if(sample)120 else c.fps,ping,if(sample)20.0 else tps.value(System.nanoTime()))
 }
 private fun text():String=dev.solyanka.utility.PerformanceLayout.lines(S.currentGeneral().features.performance,metrics()).joinToString(" | ")
 fun editorEntry(entry:HudEntry):HudEntry {
  if(entry.key!="extra.performance")return entry
  val s=S.currentGeneral().features.performance;val list=dev.solyanka.utility.PerformanceLayout.lines(s,metrics(true))
  return entry.copy(lines=list.ifEmpty {listOf("No metrics selected")},baseWidth=(list.maxOfOrNull {Minecraft.getInstance().font.width(it)} ?: 0)+4,baseHeight=maxOf(11,list.size*Minecraft.getInstance().font.lineHeight+2))
 }
 fun preview(g:GuiGraphicsExtractor,x:Int,y:Int) {draw(g,true,x,y)}
 private fun draw(g:GuiGraphicsExtractor,sample:Boolean=false,x:Int?=null,y:Int?=null) {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.performance;val h=s.hud;val values=metrics(sample)
  if(values.isEmpty())return
  val rows=dev.solyanka.utility.PerformanceLayout.lines(s,values)
  val width=(rows.maxOfOrNull {c.font.width(it)} ?: 0)+4
  val height=if(s.direction=="vertical")values.size*c.font.lineHeight+2 else c.font.lineHeight+2
  HudFrame.draw(g,h,width,height,x,y,ModuleIds.PERFORMANCE,glowRadius=0) {
   var dx=2;var dy=1
   values.forEachIndexed {i,m->
    if(i>0 && s.direction=="horizontal") {g.text(c.font," | ",dx,dy,s.nameColor,true);dx+=c.font.width(" | ")}
    g.text(c.font,m.label,dx,dy,s.nameColor,true);dx+=c.font.width(m.label)
    val color=if(s.metricColors)PerformanceText.metricColor(m.label+m.value)?.let {0xff000000.toInt() or it} ?: s.valueColor else s.valueColor
    g.text(c.font,m.value,dx,dy,color,true);dx+=c.font.width(m.value)
    if(s.direction=="vertical") {dx=2;dy+=c.font.lineHeight}
   }
  }
 }
 fun register() {
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","performance")) {g,_->
   val c=Minecraft.getInstance();val screen=c.gui.screen()
   if(S.isEnabled(ModuleIds.PERFORMANCE) && S.currentGeneral().features.performance.hud.enabled && c.level!=null && c.player!=null && !c.gui.hud.isHidden && screen !is HudEditorScreen && (screen==null || screen is ChatScreen || screen is dev.solyanka.client.ui.nofrills.NoFrillsClickGui)) draw(g)
  }
 }
}
