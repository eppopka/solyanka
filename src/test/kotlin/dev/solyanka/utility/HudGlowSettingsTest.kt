/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.solyanka.ui.GuiCatalog
import dev.solyanka.config.ConfigStore
import java.nio.file.Files
import kotlin.test.*
import org.junit.jupiter.api.Test

class HudGlowSettingsTest {
 private val ids=GuiCatalog.features.filter {it.category=="HUD"}.map {it.id}
 @Test fun `each HUD feature has an independent default off toggle`() {
  assertEquals(13,ids.size)
  val original=FeatureCodec.decode(JsonObject()).hudGlow
  assertTrue(ids.none {original.enabled(it)})
  for(id in ids) {
   val changed=original.withEnabled(id,true)
   assertTrue(changed.enabled(id),id)
   assertTrue(ids.filter {it!=id}.none {changed.enabled(it)},id)
   assertEquals(original,changed.withEnabled(id,false))
  }
  assertFalse(original.enabled("unknown"));assertFails {original.withEnabled("unknown",true)}
 }
 @Test fun `settings roundtrip and reject non boolean values`() {
  val glow=HudGlowSettings().withEnabled("render.picture_in_picture",true).withEnabled("hud.inventory",true)
  val f=FeatureSettings(hudGlow=glow)
  assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"hudGlow":{"inventory":"true"}}""").asJsonObject)}
 }
 @Test fun `glow survives a config save and reload`() {
  val dir=Files.createTempDirectory("hud-glow")
  try {
   val file=dir.resolve("config.json")
   val settings=GeneralSettings(features=FeatureSettings(hudGlow=HudGlowSettings().withEnabled("hud.inventory",true)))
   val store=ConfigStore(file){_,_->};assertTrue(store.save(emptyMap(),general=settings))
   val reloaded=ConfigStore(file){_,_->};reloaded.load()
   assertEquals(settings,reloaded.general())
  } finally {dir.toFile().deleteRecursively()}
 }
 @Test fun `feature renderers use the shared opt in halo`() {
  val root=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature")
  val files=listOf("ArchiveHuds.kt","ExtraHuds.kt","FeatureRuntime.kt","PetDisplay.kt","PictureInPicture.kt","ScoreboardHud.kt","StrayInventoryHud.kt","ThemedChat.kt","VanillaHearts.kt","VanillaHotbar.kt","YandexMusic.kt")
  files.forEach {assertTrue(Files.readString(root.resolve(it)).contains("HudGlow.draw("),it)}
  val draw=Files.readString(root.resolve("HudGlow.kt"))
  assertTrue(draw.contains("hudGlow.enabled(id)"));assertTrue(draw.contains("4 downTo 1"))
 }
}
