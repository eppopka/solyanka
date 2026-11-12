/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.InstantSettings

object PuzzleSolversUi {
  fun open(host: NoFrillsClickGui, model: InstantSettings) {
    var error: String? = null
    host.showPanel(
      SettingsPanel("Puzzle Solvers", { host.closePanel() }, { error ?: model.error }) { panel, w ->
        buildList {
          add(
            GuiKit.description(
              w,
              "The Puzzle Solvers card controls all four solvers. Enable each solver separately; its settings remain saved when the card is off.",
            ),
          )
          listOf(
              ModuleIds.ICE to "Ice Fill",
              ModuleIds.SIMON to "Simon Says",
              ModuleIds.BLAZE to "Blaze",
              ModuleIds.BEAMS to "Creeper Beams",
            )
            .forEach { (id, name) ->
              add(
                GuiKit.hudRow(
                  w,
                  name,
                  S.states()[id] == true,
                  "Settings",
                  70,
                  { enabled ->
                    val states = S.states().toMutableMap()
                    states[id] = enabled
                    error = S.applySettings(S.currentProfile(), states, model.general)
                    panel.refreshStatus()
                  },
                  {
                    val back = { open(host, model) }
                    if (id == ModuleIds.SIMON) HuntingUi.open(id, host, model, back)
                    else AdvancedUi.open(id, host, model, back)
                  },
                ),
              )
            }
        }
      },
    )
  }
}
