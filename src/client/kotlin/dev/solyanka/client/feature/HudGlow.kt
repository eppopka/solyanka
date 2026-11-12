/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.style
import net.minecraft.client.gui.GuiGraphicsExtractor

object HudGlow {
  fun draw(g: GuiGraphicsExtractor, id: String, x: Int, y: Int, w: Int, h: Int, radius: Int = 6) {
    val f = SolyankaClient.currentGeneral().features
    if (w <= 0 || h <= 0 || !f.hudLighting.enabled) return
    val style = f.hudGlow.style(id, f.hudLighting)
    RoundedGlow.draw(g, x, y, w, h, radius, style.color, style.intensity)
  }
}
