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

 @Test fun `search hints and empty state have English and Russian translations`() {
  val lang=Path.of("src/main/resources/assets/solyanka/lang")
  val en=JsonParser.parseString(lang.resolve("en_us.json").readText()).asJsonObject
  val ru=JsonParser.parseString(lang.resolve("ru_ru.json").readText()).asJsonObject
  val expected=mapOf(
   "search_ctrl_f" to ("Search... (Ctrl+F)" to "Поиск... (Ctrl+F)"),
   "ctrl_f_focus_feature_search" to ("Ctrl+F: focus feature search" to "Ctrl+F: перейти к поиску функций"),
   "no_matching_features" to ("No matching features" to "Совпадений не найдено"),
   "right_click_settings_ctrl_f_search" to ("Right-click: settings | Ctrl+F: search" to "Правый щелчок: настройки | Ctrl+F: поиск"),
   "drag_the_settings_title_bar_to_move_it" to ("Drag the settings title bar to move it" to "Перетащите заголовок настроек, чтобы переместить панель"),
   "left_toggle_right_settings_middle_favorite" to ("Left: toggle  |  Right: settings  |  Middle: favorite" to "ЛКМ: вкл./выкл.  |  ПКМ: настройки  |  СКМ: избранное"),
  )
  expected.forEach { (key,translations) ->
   assertEquals(translations.first,en.get("solyanka.ui.$key")?.asString,key)
   assertEquals(translations.second,ru.get("solyanka.ui.$key")?.asString,key)
  }
  val gui=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt").readText()
  assertTrue("GuiText.component(\"Search... (Ctrl+F)\").string" in gui)
  assertTrue("GuiText.component(\"Ctrl+F: focus feature search\")" in gui)
  assertTrue("GuiKit.label(\"No matching features\")" in gui)
  assertTrue("GuiText.component(\"Left: toggle  |  Right: settings  |  Middle: favorite\")" in gui)
 }

 @Test fun `feature card names have localized entries with English fallbacks`() {
  val lang=Path.of("src/main/resources/assets/solyanka/lang")
  val en=JsonParser.parseString(lang.resolve("en_us.json").readText()).asJsonObject
  val ru=JsonParser.parseString(lang.resolve("ru_ru.json").readText()).asJsonObject
  val catalog=Path.of("src/main/kotlin/dev/solyanka/model/GuiModels.kt").readText()
  val gui=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt").readText()
  val kit=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/GuiKit.kt").readText()
  val names=Regex("""FeatureCardInfo\("([^"]+)",\s*"([^"]+)"""").findAll(catalog).map { it.groupValues[2] }.toSet()

  assertTrue(names.isNotEmpty())
  assertTrue("GuiKit.label(GuiText.component(info.name))" in gui)
  assertTrue("font.width(GuiText.component(it.name))" in gui)
  assertTrue("GuiCatalog.search(query,config.sortByLength,config.favorites,SolyankaClient.recentModules()) { GuiText.component(it.name).string }" in gui)
  assertTrue("fun label(text:String) = label(GuiText.component(text))" in kit)
  names.forEach { name ->
   val key="solyanka.ui."+name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"),"_").trim('_')
   assertEquals(name,en.get(key)?.asString,"English fallback for $key")
   assertTrue(ru.has(key),"Missing Russian translation for $name")
   assertTrue(ru.get(key).asString.isNotBlank(),key)
  }
 }

 @Test fun `feature card advance updates title with localization and search highlight`() {
  val gui=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt").readText()
  val advance=gui.substringAfter("fun advance(now:Long) {").substringBefore("override fun onMouseDown")
  val colorUpdate=advance.substringAfter("if(color!=lastColor) {").substringBefore("if(highlight!=lastMatchColor)")
  val titleUpdate=advance.substringAfter("if(highlight!=lastMatchColor) {")
  val titleBuilder=gui.substringAfter("internal fun featureCardTitle(")

  assertTrue("val localized=GuiText.component(info.name)" in titleUpdate)
  assertTrue("title.text(featureCardTitle(localized,info.name,localized.string,query,highlight))" in titleUpdate)
  assertTrue("if(highlight!=lastMatchColor)" in advance)
  assertTrue("if(color!=lastColor)" in advance)
  assertTrue("title.color(Color.ofArgb(color or 0xff000000.toInt()))" in colorUpdate)
  assertFalse("title.text(" in colorUpdate)
  assertFalse("Component.literal(info.name)" in advance)
  assertFalse("title.color(" in titleUpdate)
  assertFalse("title.text(" in advance.substringBefore("if(highlight!=lastMatchColor)"))
  assertTrue("localizedName.indexOf(term,ignoreCase=true)" in titleBuilder)
  assertTrue("if(index<0) return localized" in titleBuilder)
  assertTrue("localizedName.substring(index,index+term.length)).withStyle(Style.EMPTY.withColor(highlightColor))" in titleBuilder)
 }
}
