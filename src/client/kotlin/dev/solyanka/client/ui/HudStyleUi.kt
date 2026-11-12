/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.*
import dev.solyanka.utility.ScoreboardHudSettings
import dev.solyanka.utility.ThemedChatSettings

/**
 * Typed edits preserve unrelated settings and avoid serializing the entire configuration on slider
 * ticks.
 */
object HudStyleUi {
  fun open(id: String, host: NoFrillsClickGui, model: InstantSettings) {
    val scoreboard = id == ModuleIds.SCOREBOARD
    host.showPanel(
      SettingsPanel(
        if (scoreboard) "Scoreboard" else "Themed Chat",
        { host.closePanel() },
        { model.error },
      ) { panel, w ->
        fun score(change: (ScoreboardHudSettings) -> ScoreboardHudSettings) {
          model.updateGeneral { g ->
            g.copy(features = g.features.copy(scoreboardHud = change(g.features.scoreboardHud)))
          }
          panel.refreshStatus()
        }
        fun chat(change: (ThemedChatSettings) -> ThemedChatSettings) {
          model.updateGeneral { g ->
            g.copy(features = g.features.copy(themedChat = change(g.features.themedChat)))
          }
          panel.refreshStatus()
        }
        buildList {
          add(
            GuiKit.description(
              w,
              if (scoreboard)
                "Rounded sidebar with team-colored objective selection and original styled names/scores. Move and scale with /solyanka gui. Long rows are clipped to avoid score overlap. No invented live data."
              else
                "One rounded pane plus styled input. Vanilla text, links, hover, scroll, suggestions and message-security tags remain intact. No frost blur or slide animation; click hitboxes stay unchanged.",
            ),
          )
          add(
            GuiKit.button("Apply Stray classic colors", w) {
              model.updateGeneral { old ->
                old.copy(
                  features =
                    if (scoreboard)
                      old.features.copy(
                        scoreboardHud =
                          dev.solyanka.utility.StrayClassicStyle.scoreboard(
                            old.features.scoreboardHud,
                          ),
                      )
                    else
                      old.features.copy(
                        themedChat =
                          dev.solyanka.utility.StrayClassicStyle.chat(old.features.themedChat),
                      ),
                )
              }
              host.closePanel()
              open(id, host, model)
            },
          )
          if (scoreboard) {
            val s = model.general.features.scoreboardHud
            add(HudNavigation.button(w))
            add(
              GuiKit.toggleRow(w, "Enabled", s.hud.enabled) { v ->
                score { it.copy(hud = it.hud.copy(enabled = v)) }
              },
            )
            add(
              GuiKit.colorRow(w, "Color", s.hud.color) { v ->
                score { it.copy(hud = it.hud.copy(color = v)) }
              },
            )
            add(
              GuiKit.colorRow(w, "Background Color", s.backgroundColor) { v ->
                score { it.copy(backgroundColor = v) }
              },
            )
            add(
              GuiKit.colorRow(w, "Border Color", s.borderColor) { v ->
                score { it.copy(borderColor = v) }
              },
            )
            add(
              GuiKit.colorRow(w, "Accent Color", s.accentColor) { v ->
                score { it.copy(accentColor = v) }
              },
            )
            add(
              GuiKit.toggleRow(w, "Background", s.background) { v ->
                score { it.copy(background = v) }
              },
            )
            add(
              GuiKit.toggleRow(w, "Objective title", s.header) { v ->
                score { it.copy(header = v) }
              },
            )
            add(GuiKit.toggleRow(w, "Scores", s.scores) { v -> score { it.copy(scores = v) } })
            add(GuiKit.toggleRow(w, "Shadow", s.shadow) { v -> score { it.copy(shadow = v) } })
            add(
              GuiKit.sliderRow(
                w,
                "Max Width",
                s.maxWidth.toDouble(),
                NumberRange(100.0, 400.0, 1.0),
              ) { v ->
                score { it.copy(maxWidth = v.toInt()) }
              },
            )
          } else {
            val s = model.general.features.themedChat
            add(
              GuiKit.colorRow(w, "Background Color", s.backgroundColor) { v ->
                chat { it.copy(backgroundColor = v) }
              },
            )
            add(
              GuiKit.colorRow(w, "Border Color", s.borderColor) { v ->
                chat { it.copy(borderColor = v) }
              },
            )
            add(
              GuiKit.colorRow(w, "Input Color", s.inputColor) { v ->
                chat { it.copy(inputColor = v) }
              },
            )
            add(GuiKit.toggleRow(w, "Rounded", s.rounded) { v -> chat { it.copy(rounded = v) } })
            add(GuiKit.toggleRow(w, "Input", s.input) { v -> chat { it.copy(input = v) } })
            add(
              GuiKit.sliderRow(w, "Opacity", s.opacity, NumberRange(.1, 1.0, .05)) { v ->
                chat { it.copy(opacity = v) }
              },
            )
          }
          addAll(HudGlowUi.controls(id, model, panel, w))
        }
      },
    )
  }
}
