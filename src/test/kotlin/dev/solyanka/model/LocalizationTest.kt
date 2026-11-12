/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import com.google.gson.JsonParser
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.readText
import kotlin.test.*
import org.junit.jupiter.api.Test

class LocalizationTest {
 @Test fun `Russian catalog covers all English translation keys`() {
  val lang=Path.of("src/main/resources/assets/solyanka/lang")
  val en=JsonParser.parseString(lang.resolve("en_us.json").readText()).asJsonObject
  val ru=JsonParser.parseString(lang.resolve("ru_ru.json").readText()).asJsonObject
  assertEquals(en.keySet(),ru.keySet())
  ru.entrySet().forEach {(key,value)->assertTrue(value.isJsonPrimitive && value.asString.isNotBlank(),key)}
 }
 @Test fun `common control labels use translations with original text fallback`() {
  val root=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills")
  val kit=root.resolve("GuiKit.kt").readText();val text=root.resolve("GuiText.kt").readText()
  assertTrue("fun label(text:Component) = PlainLabel(text)" in kit)
  assertTrue("fun button(text:Component" in kit)
  assertTrue("Component.translatableWithFallback" in text)
 }

 @Test fun `feature card names have localized entries with English fallbacks`() {
  val lang=Path.of("src/main/resources/assets/solyanka/lang")
  val en=JsonParser.parseString(lang.resolve("en_us.json").readText()).asJsonObject
  val ru=JsonParser.parseString(lang.resolve("ru_ru.json").readText()).asJsonObject
  val catalog=Path.of("src/main/kotlin/dev/solyanka/model/GuiModels.kt").readText()
  val gui=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt").readText()
  val names=Regex("""FeatureCardInfo\("([^"]+)",\s*"([^"]+)"""").findAll(catalog).map { it.groupValues[2] }.toSet()

  assertTrue(names.isNotEmpty())
  assertTrue("GuiKit.label(info.name)" in gui)
  names.forEach { name ->
   val key="solyanka.ui."+name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"),"_").trim('_')
   assertEquals(name,en.get(key)?.asString,"English fallback for $key")
   assertTrue(ru.has(key),"Missing Russian translation for $name")
   assertTrue(ru.get(key).asString.isNotBlank(),key)
  }
 }
}
