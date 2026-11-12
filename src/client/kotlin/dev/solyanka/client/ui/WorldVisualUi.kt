/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.*
import dev.solyanka.utility.FeatureSettings

object WorldVisualUi {
  fun open(id: String, host: NoFrillsClickGui, model: InstantSettings) {
    val key =
      when (id) {
        ModuleIds.PLAYER_GLOW -> "playerGlow"
        ModuleIds.NAMETAGS -> "nametags"
        ModuleIds.BLOCK_OUTLINE -> "blockOutline"
        else -> "titanium"
      }
    val title =
      when (key) {
        "playerGlow" -> "Player Glow"
        "nametags" -> "Nametags"
        "blockOutline" -> "Block Outline"
        else -> "Titanium ESP"
      }
    var error: String? = null
    host.showPanel(
      SettingsPanel(title, { host.closePanel() }, { error ?: model.error }) { panel, w ->
        val section = FeatureFields.fields(model.general.features, "worldVisuals.$key")
        fun change(edit: (FeatureSettings) -> FeatureSettings) {
          try {
            val next = edit(model.general.features)
            next.validate()
            model.updateGeneral { it.copy(features = next) }
            error = null
          } catch (e: Exception) {
            error = e.message ?: "Invalid setting"
          }
          panel.refreshStatus()
        }
        buildList {
          add(
            GuiKit.description(
              w,
              when (key) {
                "playerGlow" ->
                  "Native silhouette outlines, not the source blur shader. Real accounts filter uses UUID v4; disable for offline players. Existing server glow colors are preserved."
                "nametags" ->
                  "Rounded, bordered player plates with styled names, muted distance and source distance scaling. Optional health row. Real accounts filter skips most NPCs. Through Walls off uses visual line of sight. F1 hides custom labels."
                "blockOutline" ->
                  "Voxel contour/fill with a layered soft-edge halo. This is not the source framebuffer blur shader; use Glow off for the previous crisp contour."
                else ->
                  "Loaded blocks only, SkyBlock mining areas. Polished diorite marks titanium. All off follows unfinished commissions with approximate source regions; unknown areas fail closed. Scan: 32768 block visits/tick."
              },
            ),
          )
          section.forEach { field ->
            val name = field.name
            val label =
              if (name == "all") "Show All Titanium"
              else
                name.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
            val range =
              when (name) {
                "scale" -> NumberRange(0.5, 3.0, 0.05)
                "opacity" -> NumberRange(0.15, 1.0, 0.05)
                "glowStrength" -> NumberRange(0.1, 1.0, 0.05)
                "glowRadius" -> NumberRange(1.0, 8.0, 0.25)
                "width" -> NumberRange(1.0, 5.0, 0.25)
                "maxMarkers" -> NumberRange(1.0, 256.0, 1.0)
                else -> NumberRange(16.0, if (key == "titanium") 96.0 else 256.0, 1.0)
              }
            when (field) {
              is FeatureField.Flag ->
                add(GuiKit.toggleRow(w, label, field.value) { v -> change { field.set(it, v) } })
              is FeatureField.Integer ->
                if (name.endsWith("Color") || name == "color")
                  add(GuiKit.colorRow(w, label, field.value) { v -> change { field.set(it, v) } })
                else
                  add(
                    GuiKit.sliderRow(w, label, field.value.toDouble(), range) { v ->
                      change { field.set(it, v.toInt()) }
                    },
                  )
              is FeatureField.Decimal ->
                add(
                  GuiKit.sliderRow(w, label, field.value, range) { v ->
                    change { field.set(it, v) }
                  },
                )
            }
          }
        }
      },
    )
  }
}
