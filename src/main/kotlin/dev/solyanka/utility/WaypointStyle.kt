/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.math.roundToInt
import kotlin.math.sin

data class WayStyle(
  val mode: String = "gradient",
  val color: Int = 0xff5555ff.toInt(),
  val speed: Double = 1.0,
  val intensity: Double = 1.0,
) {
  fun validate() {
    require(mode in setOf("gradient", "pulse", "fog"))
    require(speed.isFinite() && speed in 0.0..5.0)
    require(intensity.isFinite() && intensity in 0.0..1.0)
  }

  /** Vertex-color animation: effects are interpolated over the surfaces, not a pixel shader. */
  fun colorAt(base: Int, x: Double, y: Double, z: Double, time: Double): Int {
    val phase = time * speed
    val wave =
      when (mode) {
        "gradient" -> (0.5 + 0.5 * sin(y * 1.5 + phase))
        "pulse" -> (0.5 + 0.5 * sin(phase))
        else -> (0.5 + 0.5 * sin(x * 2.1 + y * 1.7 + z * 2.3 + phase))
      }
    val mix = (wave * intensity).coerceIn(0.0, 1.0)
    fun channel(shift: Int) =
      (((base ushr shift and 255) * (1 - mix) + (color ushr shift and 255) * mix).roundToInt())
        .coerceIn(0, 255)
    val alpha =
      if (mode == "fog")
        ((base ushr 24 and 255) * (1.0 - intensity * 0.65 * (1 - wave))).roundToInt()
      else (base ushr 24 and 255)
    return (alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
  }
}
