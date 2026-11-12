/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class SpreadLogicTest {
  @Test
  fun `mining group matches in both directions and does not absorb other blocks`() {
    val group =
      listOf(
        "minecraft:cyan_terracotta",
        "minecraft:gray_wool",
        "minecraft:light_blue_wool",
        "minecraft:prismarine",
        "minecraft:prismarine_bricks",
        "minecraft:dark_prismarine",
      )
    for (a in group) for (b in group) assertTrue(SpreadLogic.equivalent(a, b), "$a vs $b")
    assertFalse(SpreadLogic.equivalent("minecraft:blue_wool", "minecraft:prismarine"))
    assertFalse(SpreadLogic.equivalent("minecraft:gray_wool", "minecraft:stone"))
    assertTrue(SpreadLogic.equivalent("minecraft:stone", "minecraft:stone"))
    assertFalse(SpreadLogic.equivalent("minecraft:stone", "minecraft:cobblestone"))
  }

  @Test
  fun `settings load defaults and round trip`() {
    val defaults = FeatureCodec.decode(com.google.gson.JsonObject()).spreadGuesser
    assertEquals(SpreadGuesserSettings(), defaults)
    assertFalse(defaults.filled)
    val settings =
      FeatureSettings(spreadGuesser = SpreadGuesserSettings(0xff123456.toInt(), true, 0x40123456))
    assertEquals(
      settings,
      FeatureCodec.decode(com.google.gson.Gson().toJsonTree(settings).asJsonObject),
    )
  }

  @Test
  fun `reads the labelled stat without using gemstone spread`() {
    assertEquals(
      1250.5,
      SpreadLogic.readTab(listOf("§6▚ Mining Spread: §f1,250.5", "Gemstone Spread: 20")),
    )
    assertEquals(0.0, SpreadLogic.readTab(listOf("Mining Spread 0")))
    assertEquals(250.0, SpreadLogic.readTab(listOf("§6Mining Spread: §e▚§f250")))
    assertEquals(1250.5, SpreadLogic.readTab(listOf("Mining Spread: \uE0161,250.5")))
    assertEquals(300.0, SpreadLogic.readTab(listOf("Mining Spread: +\uE016 300")))
    assertNull(SpreadLogic.readTab(listOf("Mining Spread: -100")))
    assertNull(SpreadLogic.readTab(listOf("Mining Spread: unknown 250")))
    assertNull(SpreadLogic.readTab(listOf("Gemstone Spread: 250")))
    assertNull(SpreadLogic.readTab(listOf("Mining Spread: 200", "Mining Spread: 300")))
    assertNull(SpreadLogic.readTab(listOf("Mining Spread: 10001")))
    assertNull(SpreadLogic.readTab(emptyList()))
  }
}
