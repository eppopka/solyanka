/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import kotlin.test.*
import org.junit.jupiter.api.Test

class PuzzleSolverModulesTest {
 @Test fun `master migrates old enabled solvers but respects explicit choice`() {
  assertTrue(PuzzleSolverModules.masterEnabled(null,true))
  assertFalse(PuzzleSolverModules.masterEnabled(null,false))
  assertFalse(PuzzleSolverModules.masterEnabled(false,true))
  assertTrue(PuzzleSolverModules.masterEnabled(true,false))
 }
 @Test fun `master gates all solvers without affecting unrelated modules`() {
  PuzzleSolverModules.children.forEach {id->
   assertFalse(PuzzleSolverModules.active(id,true,false));assertFalse(PuzzleSolverModules.active(id,false,true));assertTrue(PuzzleSolverModules.active(id,true,true))
  }
  assertTrue(PuzzleSolverModules.active("dungeon.map_info",true,false))
 }
 @Test fun `all four solver ids remain stable`() {
  assertEquals(setOf("solver.ice_fill","custom.simon_says","solver.blaze","solver.creeper_beams"),PuzzleSolverModules.children)
 }
}
