/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.client.ui.nofrills.SettingsPanel
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.NumberRange
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent

object HudGlowUi {
  fun controls(
    id: String,
    model: InstantSettings,
    panel: SettingsPanel,
    width: Int,
  ): List<UIComponent> {
    fun save(
      change: (dev.solyanka.utility.HudGlowSettings) -> dev.solyanka.utility.HudGlowSettings,
    ): Boolean {
      val ok = model.updateGeneral { g ->
        g.copy(features = g.features.copy(hudGlow = change(g.features.hudGlow)))
      }
      panel.refreshStatus()
      return ok
    }
    // Rebuild only these rows, not the whole panel, preserving its position and scroll.
    val group = UIContainers.verticalFlow(Sizing.fixed(width), Sizing.content()).apply { gap(2) }
    fun rebuild() {
      group.clearChildren()
      val s = model.general.features.hudGlow
      group.child(
        GuiKit.toggleRow(
          width,
          "Custom Style",
          s.custom.value(id),
          "Override the global HUD glow color and intensity.",
        ) { v ->
          save { it.copy(custom = it.custom.withValue(id, v)) }
          rebuild()
        },
      )
      if (s.custom.value(id)) {
        group.child(
          GuiKit.colorRow(width, "Custom HUD Glow Color", s.colors.value(id)) { v ->
            save { it.copy(colors = it.colors.withValue(id, v)) }
          },
        )
        group.child(
          GuiKit.sliderRow(
            width,
            "Custom HUD Glow Intensity",
            s.intensity.value(id),
            NumberRange(.25, 2.0, .05),
          ) { v ->
            save { it.copy(intensity = it.intensity.withValue(id, v)) }
          },
        )
      }
    }
    rebuild()
    return listOf(group)
  }
}
