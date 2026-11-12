/* SPDX-License-Identifier: GPL-3.0-only; Stray rounded-panel visual adaptation. */
package dev.solyanka.client.feature

import dev.solyanka.gfx.PanelBand
import dev.solyanka.gfx.PanelGeometry
import net.minecraft.client.gui.GuiGraphicsExtractor

object ChromeDraw {
  private data class Key(val width: Int, val height: Int, val radius: Int)

  // Size-only cache: moving panels and changing colors never grow it. GUI/render thread only.
  private val geometry =
    object : LinkedHashMap<Key, List<PanelBand>>(128, .75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<PanelBand>>?) =
        size > 128
    }

  /** Rounded border only; never submits a transparent interior over the HUD. */
  fun outline(
    g: GuiGraphicsExtractor,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    color: Int,
    radius: Int = 5,
  ) {
    if (w <= 0 || h <= 0) return
    val key = Key(w, h, radius.coerceIn(0, minOf(w, h) / 2))
    val bands = geometry.getOrPut(key) { PanelGeometry.bands(w, h, key.radius) }
    for (band in bands) {
      val left = x + band.inset
      val right = x + w - band.inset
      if (right <= left) continue
      val top = y + band.top
      val bottom = y + band.bottom
      if (band.borderOnly) g.fill(left, top, right, bottom, color)
      else {
        g.fill(left, top, left + 1, bottom, color)
        g.fill(right - 1, top, right, bottom, color)
      }
    }
  }

  fun panel(
    g: GuiGraphicsExtractor,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    fill: Int,
    border: Int,
    radius: Int = 5,
  ) {
    if (w <= 0 || h <= 0) return
    val key = Key(w, h, radius.coerceIn(0, minOf(w, h) / 2))
    val bands = geometry.getOrPut(key) { PanelGeometry.bands(w, h, key.radius) }
    for (band in bands) {
      val left = x + band.inset
      val right = x + w - band.inset
      if (right <= left) continue
      val top = y + band.top
      val bottom = y + band.bottom
      if (band.borderOnly) g.fill(left, top, right, bottom, border)
      else {
        g.fill(left, top, right, bottom, fill)
        g.fill(left, top, left + 1, bottom, border)
        g.fill(right - 1, top, right, bottom, border)
      }
    }
  }

  /** Static rounded gradient fill; the category text itself stays unanimated. */
  fun gradientPanel(
    g: GuiGraphicsExtractor,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    top: Int,
    bottom: Int,
    border: Int,
    radius: Int = 5,
  ) {
    if (w <= 0 || h <= 0) return
    val r = radius.coerceIn(0, minOf(w, h) / 2)
    for (row in 0 until h step 2) {
      val end = minOf(row + 2, h)
      val inset =
        maxOf(
          dev.solyanka.utility.VisualChrome.inset(row, h, r),
          dev.solyanka.utility.VisualChrome.inset(end - 1, h, r),
        )
      if (x + inset < x + w - inset)
        g.fill(
          x + inset,
          y + row,
          x + w - inset,
          y + end,
          dev.solyanka.gfx.ToggleAnimation.mix(
            top,
            bottom,
            row.toDouble() / (h - 1).coerceAtLeast(1),
          ),
        )
    }
    outline(g, x, y, w, h, border, r)
    if (w > r * 2 && h > 2) g.fill(x + r, y + 1, x + w - r, y + 2, 0x1affffff)
  }
}
