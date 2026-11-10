/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.client.ui.nofrills.SettingsPanel
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.NumberRange
import io.wispforest.owo.ui.core.UIComponent

object HudGlowUi {
 /** Append these rows after all primary feature controls. */
 fun controls(id:String,model:InstantSettings,panel:SettingsPanel,width:Int):List<UIComponent> {
  fun save(change:(dev.solyanka.utility.HudGlowSettings)->dev.solyanka.utility.HudGlowSettings) {
   model.updateGeneral {g->g.copy(features=g.features.copy(hudGlow=change(g.features.hudGlow)))}
   panel.refreshStatus()
  }
  return listOf(
   GuiKit.sliderRow(width,"Soft HUD Glow Intensity",model.general.features.hudGlow.intensity.value(id),NumberRange(.25,2.0,.05),"Applies only to this HUD when Soft HUD Glow is enabled.") { value ->
    save {it.copy(intensity=it.intensity.withValue(id,value))}
   },
   GuiKit.toggleRow(width,"Soft HUD Glow",model.general.features.hudGlow.enabled(id),"Show a soft halo around this HUD.") { value ->
    save {it.withEnabled(id,value)}
   }
  )
 }
}
