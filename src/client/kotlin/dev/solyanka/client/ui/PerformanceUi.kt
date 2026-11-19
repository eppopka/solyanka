/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.InstantSettings
import dev.solyanka.utility.PerformanceSettings

object PerformanceUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Performance",{host.closePanel()},{model.error},{dev.solyanka.client.feature.PerformanceHud.status()}) {panel,w->
   val s=model.general.features.performance
   fun update(change:(PerformanceSettings)->PerformanceSettings) {
    model.updateGeneral {it.copy(features=it.features.copy(performance=change(it.features.performance)))};panel.refreshStatus()
   }
   buildList {
    add(GuiKit.description(w,"Live FPS and ping; TPS uses received server game ticks and elapsed time. Remote servers with frozen/reset clocks fall back to the usual 20-tick time-packet cadence (an estimate, not a client tick counter). Wait for two updates after joining. Singleplayer ping is Local."))
    add(GuiKit.toggleRow(w,"HUD Visible",s.hud.enabled) {v->update {it.copy(hud=it.hud.copy(enabled=v))}})
    add(HudNavigation.button(w))
    add(GuiKit.toggleRow(w,"Show FPS",s.showFps) {v->update {it.copy(showFps=v)}})
    add(GuiKit.toggleRow(w,"Show Ping",s.showPing) {v->update {it.copy(showPing=v)}})
    add(GuiKit.toggleRow(w,"Show TPS",s.showTps) {v->update {it.copy(showTps=v)}})
    add(GuiKit.choiceRow(w,"Direction",listOf("Horizontal","Vertical"),if(s.direction=="vertical")1 else 0) {v->update {it.copy(direction=if(v==1)"vertical" else "horizontal")}})
    add(GuiKit.colorRow(w,"Name Color",s.nameColor) {v->update {it.copy(nameColor=v)}})
    add(GuiKit.colorRow(w,"Value Color",s.valueColor) {v->update {it.copy(valueColor=v)}})
    add(GuiKit.toggleRow(w,"Metric status colors",s.metricColors,"Override value colors with performance thresholds.") {v->update {it.copy(metricColors=v)}})
    addAll(HudGlowUi.controls(ModuleIds.PERFORMANCE,model,panel,w))
   }
  })
 }
}
