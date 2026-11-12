/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.InstantSettings
import dev.solyanka.utility.PerformanceSettings

object PerformanceUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Performance",{host.closePanel()},{model.error}) {panel,w->
   val s=model.general.features.performance
   fun update(change:(PerformanceSettings)->PerformanceSettings) {
    model.updateGeneral {it.copy(features=it.features.copy(performance=change(it.features.performance)))};panel.refreshStatus()
   }
   buildList {
    add(GuiKit.description(w,"Live FPS and ping; TPS is estimated from received world-time updates. Singleplayer ping is Local."))
    add(GuiKit.toggleRow(w,"HUD Visible",s.hud.enabled) {v->update {it.copy(hud=it.hud.copy(enabled=v))}})
    add(HudNavigation.button(w))
    add(GuiKit.toggleRow(w,"Show FPS",s.showFps) {v->update {it.copy(showFps=v)}})
    add(GuiKit.toggleRow(w,"Show Ping",s.showPing) {v->update {it.copy(showPing=v)}})
    add(GuiKit.toggleRow(w,"Show TPS",s.showTps) {v->update {it.copy(showTps=v)}})
    add(GuiKit.colorRow(w,"Text Color",s.hud.color) {v->update {it.copy(hud=it.hud.copy(color=v))}})
    addAll(HudGlowUi.controls(ModuleIds.PERFORMANCE,model,panel,w))
   }
  })
 }
}
