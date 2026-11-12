/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.client.ui.nofrills.UiSounds
import dev.solyanka.model.GuiCatalog
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/** Opened by a held, user-bound key. No module is toggled without an explicit click. */
class QuickToggleScreen(private val key:KeyMapping,private val originX:Int,private val originY:Int):BaseOwoScreen<FlowLayout>(Component.literal("Quick Toggle")) {
 override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) {w,h->UIContainers.verticalFlow(w,h)}
 override fun build(root:FlowLayout) {
  val ids=SolyankaClient.currentGeneral().features.clickGui.favorites.toSet()
  val cards=GuiCatalog.features.filter {it.id in ids && it.id!=GuiCatalog.GUI_SETTINGS}.sortedBy {it.name.lowercase()}
  val panelWidth=180;val panelHeight=(cards.size*24+32).coerceIn(48,(height-8).coerceAtLeast(48))
  val rows=UIContainers.verticalFlow(Sizing.fixed(panelWidth-8),Sizing.content()).apply {gap(3)}
  rows.child(GuiKit.label(if(cards.isEmpty()) "No favorites: middle-click a card" else "Quick Toggle"))
  cards.forEach {info->
   val button=GuiKit.button("",panelWidth-12) {}
   fun refresh() {button.message=Component.literal("${if(SolyankaClient.isEnabled(info.id)) "●" else "○"}  ${info.name}")}
   refresh();button.onPress {
    val states=SolyankaClient.states().toMutableMap();states[info.id]=!SolyankaClient.isEnabled(info.id)
    val error=SolyankaClient.applySettings(SolyankaClient.currentProfile(),states)
    if(error!=null) Minecraft.getInstance().player?.sendSystemMessage(Component.literal(error))
    UiSounds.toggle(SolyankaClient.isEnabled(info.id));refresh()
   }
   rows.child(button)
  }
  val scroll=UIContainers.verticalScroll(Sizing.fixed(panelWidth-8),Sizing.fixed(panelHeight-8),rows)
  root.child(UIContainers.verticalFlow(Sizing.fixed(panelWidth),Sizing.fixed(panelHeight)).apply {
   positioning(Positioning.absolute(originX.coerceIn(0,(width-panelWidth).coerceAtLeast(0)),originY.coerceIn(0,(height-panelHeight).coerceAtLeast(0))))
   padding(Insets.of(4));surface {g,c->dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),GuiKit.PANEL,GuiKit.LINE,6)}
   child(scroll)
  })
 }
 override fun extractRenderState(g:net.minecraft.client.gui.GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
  dev.solyanka.client.ui.nofrills.GuiFrameClock.beginFrame()
  super.extractRenderState(g,mx,my,delta)
 }
 override fun keyReleased(event:KeyEvent):Boolean {if(key.matches(event)) {onClose();return true};return super.keyReleased(event)}
 override fun mouseReleased(event:MouseButtonEvent):Boolean {if(key.matchesMouse(event)) {onClose();return true};return super.mouseReleased(event)}
 override fun isPauseScreen()=false
 override fun removed() {super.removed();dispose();uiAdapter=null}
 override fun onClose() {Minecraft.getInstance().gui.setScreen(null)}
}
