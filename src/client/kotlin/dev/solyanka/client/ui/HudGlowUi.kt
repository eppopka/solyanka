/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.client.ui.nofrills.SettingsPanel
import dev.solyanka.ui.InstantSettings

object HudGlowUi {
 fun row(id:String,model:InstantSettings,panel:SettingsPanel,width:Int) =
  GuiKit.toggleRow(width,"Soft HUD Glow",model.general.features.hudGlow.enabled(id),"A subtle halo around this HUD; independent of other HUDs.") { value ->
   model.updateGeneral {g->g.copy(features=g.features.copy(hudGlow=g.features.hudGlow.withEnabled(id,value)))}
   panel.refreshStatus()
  }
}
