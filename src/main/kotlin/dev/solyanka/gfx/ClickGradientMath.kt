/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.gfx

import kotlin.math.sin

object ClickGradientMath {
  fun color(index: Int, seconds: Double): Int {
    val hue = (0.61 + 0.13 * sin(seconds * 1.15 - index * 0.38)).toFloat()
    return java.awt.Color.HSBtoRGB(hue, 0.72f, 1f) and 0xffffff
  }
}
