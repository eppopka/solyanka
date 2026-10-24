/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class TooltipCrashRegressionTest {
 private val ui=Path.of("src/client/kotlin/dev/solyanka/client/ui")
 @Test fun `plain labels normalize absent hover style at lookup boundary`() {
  val s=ui.resolve("nofrills/GuiKit.kt").readText().substringAfter("class PlainLabel(").substringBefore("class FlatTextbox(")
  assertTrue("override fun styleAt(" in s)
  assertTrue("super.styleAt(mouseX,mouseY) ?: net.minecraft.network.chat.Style.EMPTY" in s)
 }
 @Test fun `common label factory uses protected plain labels`() {
  val s=ui.resolve("nofrills/GuiKit.kt").readText()
  assertTrue("fun label(text:String) = PlainLabel(" in s)
  listOf("nofrills/SettingsPanel.kt","HeldItemUi.kt").forEach {file->
   assertFalse("UIComponents.label(" in ui.resolve(file).readText())
  }
 }
 @Test fun `style guard does not disable library tooltip rendering`() {
  val s=ui.resolve("nofrills/GuiKit.kt").readText().substringAfter("class PlainLabel(").substringBefore("class FlatTextbox(")
  assertFalse("override fun drawTooltip" in s)
  assertTrue("super.styleAt(mouseX,mouseY)" in s)
  assertFalse("catch(" in s)
 }
}
