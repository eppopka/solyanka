/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.solyanka.ui.GuiCatalog
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.*
import org.junit.jupiter.api.Test

class ClickGuiHudMajorTest {
 @Test fun `both module sorting modes are deterministic within every category`() {
  for(cards in GuiCatalog.search("").values) {
   assertEquals(cards.sortedWith(compareBy({it.name.lowercase()},{it.id})),cards)
  }
  for(cards in GuiCatalog.search("",true).values) {
   assertEquals(cards.sortedWith(compareByDescending<dev.solyanka.ui.FeatureCardInfo> {it.name.length}.thenBy {it.name.lowercase()}.thenBy {it.id}),cards)
  }
 }
 @Test fun `gradient changes smoothly over time and varies by character`() {
  val a=ClickGradientMath.color(0,0.0);val b=ClickGradientMath.color(5,0.0)
  assertNotEquals(a,b);assertNotEquals(a,ClickGradientMath.color(0,1.0))
  val next=ClickGradientMath.color(0,.001)
  for(shift in listOf(0,8,16)) assertTrue(abs((a shr shift and 255)-(next shr shift and 255))<5)
 }
 @Test fun `global lighting and individual custom overrides are independent`() {
  val global=HudLightingSettings(color=0xff0044cc.toInt(),intensity=1.4)
  val id="hud.inventory";val other="render.picture_in_picture"
  val start=HudGlowSettings().withEnabled(id,true).withEnabled(other,true)
  val custom=start.copy(custom=start.custom.withValue(id,true),colors=start.colors.withValue(id,0xffff4400.toInt()),intensity=start.intensity.withValue(id,.5))
  assertEquals(HudGlowStyle(0xffff4400.toInt(),.5),custom.style(id,global))
  assertEquals(HudGlowStyle(global.color,global.intensity),custom.style(other,global))
  val fallback=custom.copy(custom=custom.custom.withValue(id,false))
  assertEquals(HudGlowStyle(global.color,global.intensity),fallback.style(id,global))
  assertEquals(0xffff4400.toInt(),fallback.colors.value(id))
 }
 @Test fun `non HUD category screen displays also support independent glow styles`() {
  val ids=listOf("dungeon.tick_timers","custom.simon_says","dungeon.waypoints","dungeon.map_info")
  var glow=HudGlowSettings()
  for((i,id) in ids.withIndex()) {
   glow=glow.withEnabled(id,true).copy(custom=glow.custom.withValue(id,true),colors=glow.colors.withValue(id,0xff000000.toInt()+i),intensity=glow.intensity.withValue(id,1.0+i*.1))
  }
  val global=HudLightingSettings()
  ids.forEachIndexed {i,id->
   assertTrue(glow.enabled(id));assertEquals(HudGlowStyle(0xff000000.toInt()+i,1.0+i*.1),glow.style(id,global))
  }
  assertEquals(glow,FeatureCodec.decode(Gson().toJsonTree(FeatureSettings(hudGlow=glow)).asJsonObject).hudGlow)
 }
 @Test fun `new global settings migrate and roundtrip through the config codec`() {
  val old=FeatureCodec.decode(JsonObject());assertFalse(old.clickGui.sortByLength)
  assertEquals(1.0,old.hudLighting.intensity)
  val f=old.copy(clickGui=ClickGuiSettings(0xff13579b.toInt(),0xffabcdef.toInt(),true),hudLighting=HudLightingSettings(0xff554433.toInt(),1.75),hudGlow=old.hudGlow.copy(custom=old.hudGlow.custom.withValue("hud.inventory",true)))
  assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
  assertFails {f.copy(hudLighting=f.hudLighting.copy(intensity=Double.NaN)).validate()}
 }
 @Test fun `rounded glow coverage is bounded symmetric and non overlapping`() {
  val w=40;val h=26;val spread=8
  for(r in listOf(0,5,10)) {
   val bands=RoundedGlowGeometry.bands(w,h,r,spread);assertTrue(bands.isNotEmpty())
   val grid=mutableMapOf<Pair<Int,Int>,Int>()
   for(b in bands) {
    assertTrue(b.width>0 && b.height>0 && b.level in 1..32)
    for(y in b.y until b.y+b.height) for(x in b.x until b.x+b.width) {
     assertTrue(x in -spread until w+spread && y in -spread until h+spread)
     assertNull(grid.put(x to y,b.level),"overlapping halo coverage")
    }
   }
   for((p,a) in grid) {
    assertEquals(a,grid[(w-1-p.first) to p.second],"horizontal symmetry")
    assertEquals(a,grid[p.first to (h-1-p.second)],"vertical symmetry")
   }
   assertNull(grid[(w/2) to (h/2)])
  }
 }
 @Test fun `glow geometry stays bounded for normal HUD sizes`() {
  assertTrue(RoundedGlowGeometry.bands(0,20,3,8).isEmpty())
  val b=RoundedGlowGeometry.bands(220,76,6,8)
  assertTrue(b.size<2000,"too many fill calls: ${b.size}")
  assertTrue(b.map {it.level}.distinct().size>4,"expected a feathered ramp")
 }
 @Test fun `145 migrates old HUD settings into globally enabled lighting`() {
  val legacy=FeatureCodec.decode(JsonParser.parseString("""{"hudGlow":{"inventory":false,"intensity":{"inventory":1.5}}}""").asJsonObject)
  assertTrue(legacy.hudLighting.enabled);assertFalse(legacy.hudGlow.custom.value("hud.inventory"))
  assertEquals(HudGlowStyle(legacy.hudLighting.color,legacy.hudLighting.intensity),legacy.hudGlow.style("hud.inventory",legacy.hudLighting))
  assertEquals(1.5,legacy.hudGlow.intensity.value("hud.inventory"))
  val off=legacy.copy(hudLighting=legacy.hudLighting.copy(enabled=false),clickGui=legacy.clickGui.copy(disabledBorderColor=0xff123456.toInt()))
  val roundTrip=FeatureCodec.decode(Gson().toJsonTree(off).asJsonObject)
  assertFalse(roundTrip.hudLighting.enabled);assertEquals(0xff123456.toInt(),roundTrip.clickGui.disabledBorderColor)
 }
 @Test fun `145 Dev settings action and logo-only gradient are wired`() {
  assertEquals("Dev",GuiCatalog.search("GUI Settings").values.flatten().single {it.id==GuiCatalog.GUI_SETTINGS}.category)
  val ui=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt"))
  assertEquals(1,Regex("ClickGradient\\.text\\(").findAll(ui).count());assertTrue(ui.contains("ClickGradient.text(\"solyanka\""))
  assertFalse(ui.contains("gradientHeaders"));assertTrue(ui.contains("info.id==GuiCatalog.GUI_SETTINGS ||"))
  assertTrue(ui.contains("palette.disabledBorderColor"));assertFalse(ui.contains("GuiKit.button(\"GUI Settings\""))
  val panel=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/ui/ClickGuiSettingsUi.kt"))
  assertTrue(panel.contains("Disabled Modules Border Color"));assertTrue(panel.contains("Soft HUD Glow"))
 }
 @Test fun `scoreboard editor uses live rows and actual bounds`() {
  val root=Path.of("src/client/kotlin/dev/solyanka/client")
  val s=Files.readString(root.resolve("feature/ScoreboardHud.kt"))
  assertTrue(s.contains("val current=layout()"));assertTrue(s.contains("board.listPlayerScores(objective)"))
  assertFalse(s.contains("layout(true)"));assertFalse(s.contains("Late Spring 12th"))
  assertTrue(s.contains("baseWidth=w,baseHeight=h"));assertTrue(s.contains("No active scoreboard"))
  val editor=Files.readString(root.resolve("ui/HudEditorScreen.kt"))
  assertTrue(editor.contains("ScoreboardHud.editorEntry"));assertTrue(editor.contains("anchorRight=scoreboardAnchor"))
 }
}
