/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import dev.solyanka.model.GuiCatalog
import dev.solyanka.model.RecentModules
import kotlin.test.*
import org.junit.jupiter.api.Test

class ThemeFavoritesTest {
  @Test
  fun `preset changes colors but keeps unrelated configuration and positions`() {
    val start =
      FeatureSettings(scoreboardHud = ScoreboardHudSettings(hud = HudSpec(x = 77, y = 88)))
    val changed = ThemeColors.apply(start, "Amber")
    assertEquals("Amber", changed.clickGui.theme)
    assertEquals(77, changed.scoreboardHud.hud.x)
    assertEquals(start.performance, changed.performance)
    assertEquals(start.music, changed.music)
    assertEquals(changed, FeatureCodec.decode(Gson().toJsonTree(changed).asJsonObject))
  }

  @Test
  fun `favorites appear first and remain searchable`() {
    val f = listOf("hud.performance", "hud.scoreboard")
    val result = GuiCatalog.search("", favorites = f)
    assertEquals("Favorites", result.keys.first())
    assertEquals(f.toSet(), result.getValue("Favorites").map { it.id }.toSet())
    assertEquals(
      listOf("hud.performance"),
      GuiCatalog.search("Performance", favorites = f).getValue("Favorites").map { it.id },
    )
  }

  @Test
  fun `recent modules are bounded and ordered by latest change`() {
    val recent = RecentModules(2)
    recent.record("hud.performance")
    recent.record("hud.scoreboard")
    recent.record("hud.performance")
    recent.record("hud.inventory")
    assertEquals(listOf("hud.inventory", "hud.performance"), recent.snapshot())
    val result = GuiCatalog.search("", recent = recent.snapshot())
    assertEquals(
      listOf("hud.inventory", "hud.performance"),
      result.getValue("Recently changed").map { it.id },
    )
    assertEquals(
      listOf("hud.performance"),
      GuiCatalog.search("Performance", recent = recent.snapshot())
        .getValue("Recently changed")
        .map { it.id },
    )
  }

  @Test
  fun `invalid favorites and themes are rejected`() {
    assertFails { FeatureSettings(clickGui = ClickGuiSettings(theme = "broken")).validate() }
    assertFails {
      FeatureSettings(clickGui = ClickGuiSettings(favorites = listOf("hud.a", "hud.a"))).validate()
    }
  }
}
