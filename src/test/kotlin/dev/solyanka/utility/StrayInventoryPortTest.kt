/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class StrayInventoryPortTest {
 private fun source()=Path.of("src/client/kotlin/dev/solyanka/client/feature/StrayInventoryHud.kt").readText()
 @Test fun `inventory port keeps Stray palette and two quad wells`() {
  val s=source();for(color in listOf("0xff2fb5ff","0xff1c2430","0xff1a222c","0xff171b24","0xff8a9aab")) assertTrue(color in s)
  assertTrue("sx+17,sy+17" in s);assertFalse("GuiKit.border" in s)
 }
 @Test fun `filled label is cached by player identity and tick`() {
  val s=source();assertTrue("countPlayer.get()===player && countTick==player.tickCount" in s)
  assertTrue("for(i in 0 until 36)" in s);assertTrue("player.offhandItem.isEmpty" in s)
 }
 @Test fun `preview and live HUD use the same port and restore poses`() {
  val s=source();assertTrue("finally {g.pose().popMatrix()}" in s)
  val extra=Path.of("src/client/kotlin/dev/solyanka/client/feature/ExtraHuds.kt").readText()
  assertTrue("StrayInventoryHud.draw(g,s,sample,x,y)" in extra)
  assertTrue("inventory(g,f.inventoryHud,true,x,y)" in extra)
 }
 @Test fun `default inventory colors match classic Stray`() {
  assertEquals(0xff2fb5ff.toInt(),InventoryHudSettings().hud.color)
  assertEquals(0xff0b0e14.toInt(),InventoryHudSettings().backgroundColor)
 }
}
