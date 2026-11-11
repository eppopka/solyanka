/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.ui.HudEntry
import dev.solyanka.utility.ServerTpsEstimator
import dev.solyanka.utility.PerformanceText
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

object PerformanceHud {
 private val tps=ServerTpsEstimator()
 fun reset()=tps.reset()
 fun observe(ticks:Long) {tps.observe(ticks,System.nanoTime())}
 private fun text():String {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.performance
  val ping=if(c.hasSingleplayerServer()) "Local" else c.player?.let {c.connection?.getPlayerInfo(it.uuid)?.latency}?.takeIf {it>=0}?.let {"$it ms"} ?: "—"
  return PerformanceText.format(s,c.fps,ping,tps.value(System.nanoTime()))
 }
 fun editorEntry(entry:HudEntry):HudEntry=if(entry.key=="extra.performance") entry.copy(lines=listOf(text().ifEmpty {"No metrics selected"})) else entry
 private fun draw(g:GuiGraphicsExtractor) {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.performance;val h=s.hud;val text=text()
  if(text.isEmpty()) return
  val scale=h.scale.toFloat();val width=c.font.width(text)+4;val height=11
  val x=h.x.coerceIn(0,(c.window.guiScaledWidth-width*scale).roundToInt().coerceAtLeast(0))
  val y=h.y.coerceIn(0,(c.window.guiScaledHeight-height*scale).roundToInt().coerceAtLeast(0))
  g.pose().pushMatrix()
  try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);HudGlow.draw(g,S.PERFORMANCE,0,0,width,height,0);g.text(c.font,text,2,1,h.color,true)} finally {g.pose().popMatrix()}
 }
 fun register() {
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","performance")) {g,_->
   val c=Minecraft.getInstance();val screen=c.gui.screen()
   if(S.isEnabled(S.PERFORMANCE) && S.currentGeneral().features.performance.hud.enabled && c.level!=null && c.player!=null && !c.gui.hud.isHidden && screen !is HudEditorScreen && (screen==null || screen is ChatScreen || screen is dev.solyanka.client.ui.nofrills.NoFrillsClickGui)) draw(g)
  }
 }
}
