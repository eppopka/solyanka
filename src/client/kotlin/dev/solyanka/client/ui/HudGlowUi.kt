/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.client.ui.nofrills.SettingsPanel
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.NumberRange
import io.wispforest.owo.ui.core.UIComponent

object HudGlowUi {
 fun controls(id:String,model:InstantSettings,panel:SettingsPanel,width:Int):List<UIComponent> {
  fun save(change:(dev.solyanka.utility.HudGlowSettings)->dev.solyanka.utility.HudGlowSettings) {
   model.updateGeneral {g->g.copy(features=g.features.copy(hudGlow=change(g.features.hudGlow)))};panel.refreshStatus()
  }
  val s=model.general.features.hudGlow
  return listOf(
   GuiKit.colorRow(width,"Custom HUD Glow Color",s.colors.value(id)) {v->save {it.copy(colors=it.colors.withValue(id,v))}},
   GuiKit.sliderRow(width,"Custom HUD Glow Intensity",s.intensity.value(id),NumberRange(.25,2.0,.05),"Used only when Custom Style is enabled.") {v->save {it.copy(intensity=it.intensity.withValue(id,v))}},
   GuiKit.toggleRow(width,"Custom Style",s.custom.value(id),"Use this HUD's own color and intensity; otherwise use global HUD lighting.") {v->save {it.copy(custom=it.custom.withValue(id,v))}},
   GuiKit.toggleRow(width,"Soft HUD Glow",s.enabled(id),"Show a glow for this HUD.") {v->save {it.withEnabled(id,v)}}
  )
 }
}
