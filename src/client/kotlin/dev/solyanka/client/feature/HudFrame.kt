/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.utility.HudFramePlacement
import dev.solyanka.utility.HudSpec
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Positions an unscaled HUD surface and guarantees its pose is restored. */
internal object HudFrame {
  inline fun draw(
    g: GuiGraphicsExtractor,
    spec: HudSpec,
    width: Int,
    height: Int,
    atX: Int? = null,
    atY: Int? = null,
    glowModule: String? = null,
    glowRadius: Int = 6,
    paint: () -> Unit,
  ) {
    val window = Minecraft.getInstance().window
    val scale = spec.scale.toFloat()
    val x = HudFramePlacement.coordinate(spec.x, window.guiScaledWidth, width, scale, atX)
    val y = HudFramePlacement.coordinate(spec.y, window.guiScaledHeight, height, scale, atY)
    g.pose().pushMatrix()
    try {
      g.pose().translate(x.toFloat(), y.toFloat())
      g.pose().scale(scale, scale)
      if (glowModule != null) HudGlow.draw(g, glowModule, 0, 0, width, height, glowRadius)
      paint()
    } finally {
      g.pose().popMatrix()
    }
  }
}
