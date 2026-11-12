/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class HudLayoutsTest {
  @Test
  fun `layout share preserves non-HUD settings and private favorites`() {
    val source =
      FeatureSettings(
        scoreboardHud = ScoreboardHudSettings(hud = HudSpec(x = 7, y = 15), anchorRight = true),
        clickGui = ClickGuiSettings(theme = "Amber", favorites = listOf("hud.performance")),
      )
    val target = FeatureSettings(clickGui = ClickGuiSettings(favorites = listOf("hud.scoreboard")))
    val result = HudLayouts.decodeAndApply(target, HudLayouts.encode(source))
    assertEquals(7, result.scoreboardHud.hud.x)
    assertTrue(result.scoreboardHud.anchorRight)
    assertEquals("Amber", result.clickGui.theme)
    assertEquals(listOf("hud.scoreboard"), result.clickGui.favorites)
    assertEquals(target.music, result.music)
    assertEquals(target.chatCommands, result.chatCommands)
  }

  @Test
  fun `layout share rejects invalid strings and coordinates`() {
    val f = FeatureSettings()
    assertFails { HudLayouts.decodeAndApply(f, "not a layout") }
    assertFails {
      HudLayouts.apply(f, HudLayoutSnapshot(listOf(HudPlacement("extra.scoreboard", -1, 0, 1.0))))
    }
    assertFails {
      HudLayouts.apply(f, HudLayoutSnapshot(listOf(HudPlacement("unknown", 0, 0, 1.0))))
    }
  }
}
