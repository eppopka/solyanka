/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

object PuzzleSolverModules {
  val children =
    setOf("solver.ice_fill", "custom.simon_says", "solver.blaze", "solver.creeper_beams")

  fun active(id: String, enabled: Boolean, master: Boolean) = enabled && (id !in children || master)

  fun masterEnabled(configured: Boolean?, legacyAnyEnabled: Boolean) =
    configured ?: legacyAnyEnabled
}
