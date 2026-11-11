/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.NumberRange

object ClickGuiSettingsUi {
 fun open(host:NoFrillsClickGui) {
  val model=InstantSettings(SolyankaClient.currentProfile(),SolyankaClient.currentGeneral()) {p,g->
   SolyankaClient.applySettings(p,SolyankaClient.states(),g)
  }
  host.showPanel(SettingsPanel("ClickGUI Settings",{host.closePanel()},{model.error}) {panel,w->
   fun save(change:(dev.solyanka.utility.FeatureSettings)->dev.solyanka.utility.FeatureSettings) {
    if(model.updateGeneral {it.copy(features=change(it.features))}) host.refreshCards()
    panel.refreshStatus()
   }
   val f=model.general.features
   listOf(
    GuiKit.section(w,"Module appearance","Colors and order apply across ClickGUI categories."),
    GuiKit.toggleRow(w,"Toggle Animations",f.clickGui.toggleAnimations,"Smooth module on/off transitions.") {v->save {it.copy(clickGui=it.clickGui.copy(toggleAnimations=v))}},
    GuiKit.colorRow(w,"Enabled Modules Color",f.clickGui.enabledColor) {v->save {it.copy(clickGui=it.clickGui.copy(enabledColor=v))}},
    GuiKit.colorRow(w,"Disabled Modules Text Color",f.clickGui.disabledColor) {v->save {it.copy(clickGui=it.clickGui.copy(disabledColor=v))}},
    GuiKit.colorRow(w,"Disabled Modules Border Color",f.clickGui.disabledBorderColor) {v->save {it.copy(clickGui=it.clickGui.copy(disabledBorderColor=v))}},
    GuiKit.choiceRow(w,"Module Sorting",listOf("Alphabetical (A–Z)","Name Length (longest first)"),if(f.clickGui.sortByLength) 1 else 0,readCurrent={if(model.general.features.clickGui.sortByLength) 1 else 0}) {v->save {it.copy(clickGui=it.clickGui.copy(sortByLength=v==1))}},
    GuiKit.section(w,"Global HUD Lighting","HUD elements inherit this glow by default; Custom Style overrides color and intensity."),
    GuiKit.toggleRow(w,"Soft HUD Glow",f.hudLighting.enabled,"Enable or disable the shared HUD glow.") {v->save {it.copy(hudLighting=it.hudLighting.copy(enabled=v))}},
    GuiKit.colorRow(w,"Global HUD Glow Color",f.hudLighting.color) {v->save {it.copy(hudLighting=it.hudLighting.copy(color=v))}},
    GuiKit.sliderRow(w,"Global HUD Glow Intensity",f.hudLighting.intensity,NumberRange(.25,2.0,.05)) {v->save {it.copy(hudLighting=it.hudLighting.copy(intensity=v))}}
   )
  })
 }
}
