/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.gfx

import dev.solyanka.utility.Point3

/** Line normals are stored in signed normalized bytes by Minecraft. */
object OverlayLine {
  fun direction(dx: Double, dy: Double, dz: Double): Point3? {
    val length = kotlin.math.hypot(kotlin.math.hypot(dx, dy), dz)
    if (!length.isFinite() || length < 1e-9) return null
    return Point3(dx / length, dy / length, dz / length)
  }
}
