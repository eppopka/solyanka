/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.utility.ClickGuiSettings
import dev.solyanka.utility.ThemeColors
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path

class AuditRegressionTest {
 private fun source(path:String)=Path.of("src/client/kotlin/dev/solyanka/client",path).toFile().readText()
 @Test fun `shared colors preserve existing defaults`() {
  val s=ClickGuiSettings()
  assertEquals(ThemeColors.ACCENT,s.enabledColor);assertEquals(ThemeColors.LINE,s.disabledBorderColor)
  assertEquals(0xff7dd3fc.toInt(),s.enabledColor);assertEquals(0xffc3cede.toInt(),s.disabledColor)
 }
 @Test fun `editor uses one entry snapshot for a normal frame`() {
  val frame=source("ui/HudEditorScreen.kt").substringAfter("override fun extractRenderState").substringBefore("override fun mouseClicked")
  assertEquals(1,Regex("val current=entries ").findAll(frame).count())
  assertTrue("ScoreboardHud.editorSnapshot()" in frame);assertTrue("scoreboardPreview.entry(it,width)" in frame)
  assertTrue("scoreboardPreview.draw(g,b.x,b.y)" in frame)
  assertTrue("current.firstOrNull" in frame)
 }
 @Test fun `HUD style edits are typed and title colors only update when changed`() {
  val style=source("ui/HudStyleUi.kt")
  assertFalse("toJsonTree" in style);assertFalse("FeatureCodec.decode" in style)
  assertTrue("scoreboardHud=change(" in style);assertTrue("themedChat=change(" in style)
  val gui=source("ui/nofrills/NoFrillsClickGui.kt")
  assertTrue("if(color!=lastColor)" in gui);assertTrue("if(highlight!=lastMatchColor)" in gui)
  assertTrue("ClickGradient.text(\"solyanka\"" in gui)
  assertFalse("ClickGradient.text(name" in gui)
 }
 @Test fun `runtime version is read from fabric metadata`() {
  assertTrue("metadata.version.friendlyString" in source("ModInfo.kt"))
  for(path in listOf("SolyankaClient.kt","feature/YandexMusic.kt","feature/PriceTooltips.kt","feature/InfoTooltips.kt"))
   assertFalse(Regex("Solyanka/[0-9]").containsMatchIn(source(path)),path)
 }
}
