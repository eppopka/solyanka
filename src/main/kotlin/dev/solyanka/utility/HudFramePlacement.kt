/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.math.roundToInt

/** Default placement is clamped; explicit editor coordinates are preserved as supplied. */
object HudFramePlacement {
  fun coordinate(
    configured: Int,
    screen: Int,
    size: Int,
    scale: Float,
    override: Int? = null,
  ): Int = override ?: configured.coerceIn(0, (screen - size * scale).roundToInt().coerceAtLeast(0))
}
