/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.solyanka.model.GuiCatalog
import dev.solyanka.config.ConfigStore
import java.nio.file.Files
import kotlin.test.*
import org.junit.jupiter.api.Test

class HudGlowSettingsTest {
 private val ids=GuiCatalog.features.filter {it.category=="HUD"}.map {it.id}
 @Test fun `each HUD feature has an independent default off toggle`() {
  assertEquals(14,ids.size)
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
  val glow=HudGlowSettings().withEnabled("render.picture_in_picture",true).withEnabled("hud.inventory",true).copy(intensity=HudGlowIntensity().withValue("hud.inventory",1.5))
  val f=FeatureSettings(hudGlow=glow)
  assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"hudGlow":{"inventory":"true"}}""").asJsonObject)}
 }
 @Test fun `glow survives a config save and reload`() {
  val dir=Files.createTempDirectory("hud-glow")
  try {
   val file=dir.resolve("config.json")
   val settings=GeneralSettings(features=FeatureSettings(hudGlow=HudGlowSettings().withEnabled("hud.inventory",true).copy(intensity=HudGlowIntensity().withValue("hud.inventory",1.65))))
   val store=ConfigStore(file){_,_->};assertTrue(store.save(emptyMap(),general=settings))
   val reloaded=ConfigStore(file){_,_->};reloaded.load()
   assertEquals(settings,reloaded.general())
  } finally {dir.toFile().deleteRecursively()}
 }
 @Test fun `strength migrates defaults and stays independent for every feature`() {
  val legacy=FeatureCodec.decode(JsonParser.parseString("""{"hudGlow":{"inventory":true}}""").asJsonObject).hudGlow
  assertTrue(legacy.inventory)
  assertTrue(ids.all {legacy.intensity.value(it)==1.0})
  for(id in ids) {
   val changed=legacy.intensity.withValue(id,1.75)
   assertEquals(1.75,changed.value(id))
   assertTrue(ids.filter {it!=id}.all {changed.value(it)==1.0},id)
   assertEquals(legacy.intensity,changed.withValue(id,1.0))
  }
  assertFails {legacy.intensity.withValue("hud.inventory",Double.NaN)}
  assertFails {legacy.intensity.withValue("hud.inventory",.1)}
  assertFails {legacy.intensity.withValue("hud.inventory",2.1)}
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"hudGlow":{"intensity":{"inventory":3}}}""").asJsonObject)}
 }
 @Test fun `glow controls are appended to each feature panel`() {
  val root=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/ui")
  val sources=mapOf(
   "HuntingUi.kt" to "if(id in setOf(ModuleIds.MOVE_HOTBAR,ModuleIds.PIP,ModuleIds.PET,ModuleIds.SIMON)) addAll(HudGlowUi.controls",
   "ArchiveVisualUi.kt" to "if(id==ModuleIds.MOVE_HEARTS) addAll(HudGlowUi.controls",
   "ReleaseOneUi.kt" to "if(id==ModuleIds.STOPWATCH || id==ModuleIds.DVD) addAll(HudGlowUi.controls",
   "HudStyleUi.kt" to "addAll(HudGlowUi.controls(id,model,panel,w))",
   "ExtraHudUi.kt" to "addAll(HudGlowUi.controls(id,model,panel,w))",
   "MusicUi.kt" to "addAll(HudGlowUi.controls(ModuleIds.MUSIC",
   "FeatureUi.kt" to "addAll(HudGlowUi.controls(ModuleIds.PLAYER_DISPLAY"
  )
  for((file,marker) in sources) {
   val text=Files.readString(root.resolve(file))
   assertTrue(text.contains(marker),file)
   assertFalse(text.contains("HudGlowUi.row("),file)
   // After the glow rows, the next statement closes the current buildList.
   assertTrue(text.substringAfter(marker).substringAfter('\n').trimStart().startsWith("}"),file)
   assertEquals(if(file=="FeatureUi.kt") 2 else 1,Regex("HudGlowUi\\.controls\\(").findAll(text).count(),file)
  }
  for((file,id) in listOf("FeatureUi.kt" to "ModuleIds.TICK_TIMERS", "AdvancedUi.kt" to "id", "WaypointUi.kt" to "ModuleIds.WAYPOINTS")) {
   val text=Files.readString(root.resolve(file));val marker="addAll(HudGlowUi.controls($id"
   assertTrue(text.contains(marker),file)
   assertTrue(text.substringAfter(marker).substringAfter('\n').trimStart().startsWith("}"),file)
  }
  val controls=Files.readString(root.resolve("HudGlowUi.kt"))
  assertTrue(controls.indexOf("\"Custom Style\"")<controls.indexOf("if(s.custom.value(id))"))
  assertTrue(controls.contains("if(s.custom.value(id))"));assertFalse(controls.contains("\"Soft HUD Glow\""))
 }
 @Test fun `feature renderers use the shared opt in halo`() {
  val root=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature")
  val files=listOf("ExtraHuds.kt","FeatureRuntime.kt","PetDisplay.kt","PictureInPicture.kt","ThemedChat.kt","VanillaHearts.kt","VanillaHotbar.kt","YandexMusic.kt")
  files.forEach {assertTrue(Files.readString(root.resolve(it)).contains("HudGlow.draw("),it)}
  val frame=Files.readString(root.resolve("HudFrame.kt"));assertTrue(frame.contains("HudGlow.draw(g, glowModule"))
  assertTrue(Files.readString(root.resolve("ArchiveHuds.kt")).contains("HudFrame.draw(g,h,160,34,atX,atY,ModuleIds.STOPWATCH)"))
  assertTrue(Files.readString(root.resolve("ScoreboardHud.kt")).contains("HudFrame.draw(g,s.hud,layout.width,layout.height,x,atY,ModuleIds.SCOREBOARD)"))
  assertTrue(Files.readString(root.resolve("StrayInventoryHud.kt")).contains("HudFrame.draw(g,s.hud,m.width,m.height,atX,atY,ModuleIds.INVENTORY_HUD)"))
  val draw=Files.readString(root.resolve("HudGlow.kt"))
  assertTrue(draw.contains("f.hudLighting.enabled"));assertTrue(draw.contains("RoundedGlow.draw("))
 }
}
