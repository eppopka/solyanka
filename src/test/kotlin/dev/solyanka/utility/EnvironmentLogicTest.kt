/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import kotlin.test.*
import org.junit.jupiter.api.Test

class EnvironmentLogicTest {
  @Test
  fun `clock choices preserve source constants and no-change`() {
    assertNull(EnvironmentLogic.time(0))
    assertEquals(
      listOf(23041L, 1000L, 6000L, 12610L, 13000L, 18000L),
      (1..6).map { EnvironmentLogic.time(it) },
    )
  }

  @Test
  fun `weather returns source levels without changing no-change input`() {
    assertEquals(0.3f, EnvironmentLogic.rain(0, 0.3f))
    assertEquals(0.2f, EnvironmentLogic.thunder(0, 0.2f))
    assertEquals(0f, EnvironmentLogic.rain(1, 1f))
    assertEquals(1f, EnvironmentLogic.rain(2, 0f))
    assertEquals(1f, EnvironmentLogic.thunder(3, 0f))
    assertEquals(0.9f, EnvironmentLogic.rain(4, 0f))
    assertEquals(0f, EnvironmentLogic.thunder(4, 1f))
  }

  @Test
  fun `ignore blocks affects height but never forces rain`() {
    assertEquals(70, EnvironmentLogic.roofHeight(false, 70))
    assertEquals(Int.MIN_VALUE, EnvironmentLogic.roofHeight(true, 70))
    assertEquals(0f, EnvironmentLogic.rain(0, 0f))
    assertEquals(0f, EnvironmentLogic.rain(1, 1f))
    assertFalse(FeatureCodec.decode(null).environment.ignoreWeatherBlocks)
    val s = FeatureSettings(environment = EnvironmentSettings(ignoreWeatherBlocks = true))
    assertTrue(
      FeatureCodec.decode(com.google.gson.Gson().toJsonTree(s).asJsonObject)
        .environment
        .ignoreWeatherBlocks,
    )
    assertFails {
      FeatureCodec.decode(
        JsonParser.parseString("""{"environment":{"ignoreWeatherBlocks":1}}""").asJsonObject,
      )
    }
  }

  @Test
  fun `fog minimum view and valid ordered distances`() {
    val f = EnvironmentLogic.fog(EnvironmentSettings(fogStart = 0.95, fogEnd = 0.1), 1, false)
    assertTrue(f.start >= 0)
    assertTrue(f.end > f.start)
    assertEquals(1f, f.alpha)
    assertEquals(32f * 0.95f, f.start)
  }

  @Test
  fun `Sodium uses native distance ramp without unsupported shader patch`() {
    val s = EnvironmentSettings(fogStart = 0.2, fogEnd = 1.0, fogDensity = 0.5)
    val sodium = EnvironmentLogic.fog(s, 10, true)
    val vanilla = EnvironmentLogic.fog(s, 10, false)
    assertEquals(160f, sodium.end)
    assertEquals(0.5f, sodium.alpha)
    assertTrue(vanilla.end < sodium.end)
    assertEquals(1f, vanilla.alpha)
  }

  @Test
  fun `zero density remains transparent and color uses RGB`() {
    val f =
      EnvironmentLogic.fog(
        EnvironmentSettings(fogDensity = 0.0, fogColor = 0xff0080ff.toInt()),
        10,
        false,
      )
    assertEquals(0f, f.alpha)
    assertEquals(0f, f.red)
    assertEquals(128 / 255f, f.green)
    assertEquals(1f, f.blue)
  }

  @Test
  fun `environment settings validate range and numeric types`() {
    assertEquals(
      0.8,
      FeatureCodec.decode(
          JsonParser.parseString("""{"environment":{"aspectRatio":0.8}}""").asJsonObject,
        )
        .environment
        .aspectRatio,
    )
    listOf(
        """{"clock":8}""",
        """{"weather":-1}""",
        """{"aspectRatio":0}""",
        """{"gradient":1.1}""",
        """{"fogDensity":-0.1}""",
      )
      .forEach {
        assertFails {
          FeatureCodec.decode(JsonParser.parseString("""{"environment":$it}""").asJsonObject)
        }
      }
  }

  @Test
  fun `secret drop filter does not classify ordinary mob drops`() {
    assertTrue(WaypointLogic.secretDrop("§fSpirit Leap"))
    assertTrue(WaypointLogic.secretDrop("Healing VIII Splash Potion"))
    assertFalse(WaypointLogic.secretDrop("Rotten Flesh"))
    assertFalse(WaypointLogic.secretDrop("Gold Ingot"))
  }
}
