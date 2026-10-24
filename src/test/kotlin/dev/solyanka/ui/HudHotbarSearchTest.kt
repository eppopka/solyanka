/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui
import dev.solyanka.utility.*
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test
import com.google.gson.*
class HudHotbarSearchTest {
 private fun ui(name:String)=Path.of("src/client/kotlin/dev/solyanka/client/ui/$name").toFile().readText()
 @Test fun `no enabled modules means empty editor without changing catalog`() {assertTrue(HudLayout.visibleEntries(FeatureSettings(),emptySet()).isEmpty());assertEquals(25,HudLayout.entries(FeatureSettings()).size)}
 @Test fun `every HUD owner resolves to a registered module`() {val ids=GuiCatalog.features.map {it.id}.toSet();HudLayout.entries(FeatureSettings()).forEach {assertTrue(HudLayout.owner(it.key) in ids,it.key)}}
 @Test fun `one module exposes only its own enabled HUDs`() {val f=FeatureSettings();assertEquals(listOf("extra.inventory"),HudLayout.visibleEntries(f,setOf("hud.inventory")).map {it.key});assertEquals(6,HudLayout.visibleEntries(f,setOf("dungeon.tick_timers")).size)}
 @Test fun `hidden sub HUD stays out even when parent is enabled`() {val f=FeatureSettings(timers=TimerSettings(goldor=HudSpec(enabled=false)));assertEquals(5,HudLayout.visibleEntries(f,setOf("dungeon.tick_timers")).size);assertEquals(25,HudLayout.entries(f).size)}
 @Test fun `route progress switch also filters editor`() {assertTrue(HudLayout.visibleEntries(FeatureSettings(route=RouteSettings(progress=false)),setOf("dungeon.waypoints")).isEmpty())}
 @Test fun `HUD editor handles empty selection and disappearance`() {val s=ui("HudEditorScreen.kt");assertTrue("HudLayout.visibleEntries(" in s);assertTrue("if(list.isEmpty())" in s);assertTrue("if(entry!=null)" in s);assertTrue("if(selected.isEmpty())" in s);assertTrue("firstOrNull()?.key" in s)}
 @Test fun `search shortcut is handled before modal input and focus is deferred`() {val s=ui("nofrills/NoFrillsClickGui.kt");val k=s.substringAfter("override fun keyPressed(").substringBefore("override fun mouseClicked(");assertTrue(k.indexOf("GLFW_KEY_F")<k.indexOf("panelInput"));assertTrue("GLFW_MOD_CONTROL" in k);assertTrue("navigation.close();focusSearch=true" in k);assertFalse("commitNavigation()" in k);assertTrue("box.setHighlightPos(0)" in s)}
 @Test fun `search suggestion clears when typing and mentions shortcut`() {val s=ui("nofrills/NoFrillsClickGui.kt");assertTrue("setSuggestion(if(it.isEmpty()) \"Search... (Ctrl+F)\" else null)" in s)}
 @Test fun `HUD position numbers replaced by direct editor navigation`() {for(name in listOf("FeatureUi.kt","ExtraHudUi.kt","ReleaseOneUi.kt","ArchiveVisualUi.kt")){val s=ui(name);assertFalse("listOf(\"x\",\"y\",\"scale\")" in s,name);assertTrue("HudNavigation.button(w)" in s,name)};assertFalse("\"$"+"key.x\",\"X\"" in ui("FeatureUi.kt"));assertTrue("Open HUD Editor" in ui("HudNavigation.kt"));assertFalse("sendCommand" in ui("HudNavigation.kt"))}
 @Test fun `old hotbar settings retain vanilla safe defaults`() {val f=FeatureCodec.decode(JsonParser.parseString("""{"moveHotbar":{"customPosition":true,"hud":{"x":33,"y":44}}}""").asJsonObject);assertEquals("vanilla",f.moveHotbar.style);assertEquals(33,f.moveHotbar.hud.x);assertEquals(44,f.moveHotbar.hud.y)}
 @Test fun `every style and layout roundtrips`() {for(style in listOf("vanilla","minimal","glass","neon"))for(vertical in listOf(false,true)){val f=FeatureSettings(moveHotbar=MoveHotbarSettings(style=style,vertical=vertical));assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))}}
 @Test fun `style presets are visibly distinct and preserve layout`() {val s=MoveHotbarSettings(hud=HudSpec(x=90,y=100,scale=2.0),vertical=true,moveXpLevel=true);val looks=listOf("minimal","glass","neon").map {HotbarLayout.preset(s,it)};assertEquals(3,looks.map {listOf(it.gradient,it.backgroundOpacity,it.cornerRadius,it.glow)}.distinct().size);looks.forEach {assertEquals(s.hud,it.hud);assertTrue(it.vertical);assertTrue(it.moveXpLevel)}}
 @Test fun `hotbar geometry contains all slots and optional side cells`() {for(style in listOf("vanilla","minimal","glass","neon"))for(vertical in listOf(false,true))for(before in listOf(false,true)){val g=HotbarLayout.geometry(MoveHotbarSettings(style=style,vertical=vertical),true,before,true);assertEquals(9,g.slots.size);(g.slots+listOfNotNull(g.offhand,g.attack)).forEach {assertTrue(it.x>=0&&it.y>=0);assertTrue(it.x+it.width<=g.width);assertTrue(it.y+it.height<=g.height)};assertNotEquals(g.offhand,g.attack)}}
 @Test fun `vertical geometry transposes horizontal positions`() {val s=MoveHotbarSettings(style="glass");val a=HotbarLayout.geometry(s,true,true,true);val b=HotbarLayout.geometry(s.copy(vertical=true),true,true,true);assertEquals(a.width,b.height);assertEquals(a.height,b.width);a.slots.zip(b.slots).forEach {(x,y)->assertEquals(x.x,y.y);assertEquals(x.y,y.x)}}
 @Test fun `selection moves smoothly without overshooting and resets on stalls`() {val a=HotbarSelection();assertEquals(0.0,a.update(0,0.0,16.0,true));val step=a.update(8,.016,16.0,true);assertTrue(step>0&&step<8);assertTrue(a.update(8,.016,16.0,true)>step);assertEquals(3.0,a.update(3,1.0,16.0,true));assertEquals(6.0,a.update(6,.016,16.0,false))}
 @Test fun `selection interpolation reaches exact end slots`() {val g=HotbarLayout.geometry(MoveHotbarSettings(style="glass"));assertEquals(g.slots[0],g.selection(0.0));assertEquals(g.slots[8],g.selection(8.0));assertEquals(g.slots[0],g.selection(-4.0))}
 @Test fun `style validation rejects malformed ranges`() {for(s in listOf(MoveHotbarSettings(style="invalid"),MoveHotbarSettings(padding=13),MoveHotbarSettings(slotGap=-1),MoveHotbarSettings(slideSpeed=41.0),MoveHotbarSettings(backgroundOpacity=Double.NaN),MoveHotbarSettings(itemScale=2.0)))assertFails {s.validate()}}
 @Test fun `knife source is explicit and old configs default to offline safe auto`() {assertEquals("auto",FeatureCodec.decode(JsonObject()).cs2Knife.source);for(mode in listOf("auto","install","pack"))Cs2KnifeSettings(source=mode).validate();assertFails {Cs2KnifeSettings(source="http://evil").validate()}}
 @Test fun `download UI requires confirmation and exposes cancellation`() {val s=ui("FinalCosmeticsUi.kt");assertTrue("Confirm download (134.6 MB)" in s);assertTrue("Cancel asset download" in s);assertTrue("Cs2AssetDownload.start()" in s);assertTrue("info={if(id==S.CS2_KNIFE)" in s)}
 @Test fun `aura uses reverse Z compare without writing hand depth`() {val s=Path.of("src/client/kotlin/dev/solyanka/client/feature/ItemAura.kt").toFile().readText();assertTrue("if(depth) CompareOp.GREATER_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS,false" in s);assertFalse("LESS_THAN_OR_EQUAL" in s)}
 @Test fun `hotbar native decorations survive and followers do not reset animation`() {val s=Path.of("src/client/kotlin/dev/solyanka/client/feature/VanillaHotbar.kt").toFile().readText();assertTrue("g.itemDecorations(c.font,stack,0,0)" in s);assertTrue("if(id==VanillaHudElements.HOTBAR)lastFrame=0" in s);assertTrue("p.isSpectator" in s);assertTrue("!c.gui.hud.isHidden" in s)}
 @Test fun `halo and fills respect reversed scene depth and preserve explicit bypass`() {
  val s=Path.of("src/client/kotlin/dev/solyanka/client/feature/WorldOverlay.kt").toFile().readText()
  assertEquals(2,Regex("if\\(depth\\) CompareOp.GREATER_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS,false").findAll(s).count())
  assertFalse("LESS_THAN_OR_EQUAL" in s)
  val cosmetics=Path.of("src/client/kotlin/dev/solyanka/client/feature/ArchiveCosmetics.kt").toFile().readText()
  assertTrue("!s.throughWalls" in cosmetics.substringAfter("if(S.isEnabled(S.HALO)"))
 }
}
