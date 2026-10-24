/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import dev.solyanka.ui.HudLayout
import com.google.gson.*
import kotlin.test.*
import org.junit.jupiter.api.Test
class HudStyleTest {
 @Test fun `new styles roundtrip with old features unchanged`() {val f=FeatureSettings();assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject));assertEquals(f,FeatureCodec.decode(JsonObject()))}
 @Test fun `old visual settings gain new bounded defaults`() {
  val f=FeatureCodec.decode(JsonParser.parseString("""{"worldVisuals":{"nametags":{"range":192},"blockOutline":{"width":3}}}""").asJsonObject)
  assertEquals(192,f.worldVisuals.nametags.range);assertTrue(f.worldVisuals.nametags.distanceScaling);assertTrue(f.worldVisuals.blockOutline.glow);assertEquals(3.0,f.worldVisuals.blockOutline.width)
 }
 @Test fun `new scale opacity and halo ranges validated`() {
  assertFails {NametagSettings(opacity=.1).validate()};assertFails {BlockOutlineSettings(glowRadius=9.0).validate()};assertFails {BlockOutlineSettings(glowStrength=Double.NaN).validate()};assertFails {ThemedChatSettings(opacity=0.0).validate()};assertFails {ScoreboardHudSettings(maxWidth=99).validate()}
 }
 @Test fun `new style config rejects wrong types and fractional widths`() {
  for(raw in listOf("""{"themedChat":{"input":"true"}}""","""{"scoreboardHud":{"maxWidth":200.5}}""","""{"worldVisuals":{"nametags":{"opacity":2}}}""")) assertFails {FeatureCodec.decode(JsonParser.parseString(raw).asJsonObject)}
 }
 @Test fun `source distance scale shrinks to a readable minimum`() {assertEquals(1.35,VisualChrome.distanceScale(0.0));assertEquals(1.0,VisualChrome.distanceScale(12.0));assertEquals(.52,VisualChrome.distanceScale(256.0));assertFails {VisualChrome.distanceScale(Double.NaN)}}
 @Test fun `alpha multiplies existing alpha without changing RGB`() {assertEquals(0x40123456,VisualChrome.alpha(0x80123456.toInt(),.5));assertEquals(0x00123456,VisualChrome.alpha(0x80123456.toInt(),0.0));assertEquals(0x80123456.toInt(),VisualChrome.alpha(0x80123456.toInt(),2.0))}
 @Test fun `rounded panel rows mirror vertically`() {val rows=(0 until 19).map {VisualChrome.inset(it,19,5)};assertEquals(rows,rows.reversed());assertTrue(rows.first()>0);assertEquals(0,rows[9]);assertTrue((0 until 19).all {VisualChrome.inset(it,19,0)==0})}
 @Test fun `halo runs wide faint to narrow stronger within bounded passes`() {val lines=VisualChrome.halo(-1,2.0,4.0,.6);assertEquals(3,lines.size);assertEquals(lines.sortedByDescending {it.first},lines);assertTrue(lines.all {it.first>2f && (it.second ushr 24) in 1..255});assertTrue((lines.first().second ushr 24)<(lines.last().second ushr 24))}
 @Test fun `sidebar sizing bounds rows and width`() {assertEquals(176,ScoreboardLayout.height(15,true));assertEquals(176,ScoreboardLayout.height(100,true));assertEquals(220,ScoreboardLayout.width(10000,220));assertEquals(100,ScoreboardLayout.width(0,220));assertTrue(ScoreboardLayout.height(5,true)>ScoreboardLayout.height(5,false))}
 @Test fun `chat pane uses vanilla 200 tick lifetime and respects scroll`() {assertEquals(2,ChatPaneLayout.visible(listOf(0,199,200,300),false,15,0));assertEquals(4,ChatPaneLayout.visible(listOf(0,199,200,300),true,15,0));assertEquals(1,ChatPaneLayout.visible(listOf(0,199,200),false,15,1));assertEquals(0,ChatPaneLayout.visible(emptyList(),false,15,0))}
 @Test fun `chat pane applies scale without changing native hitboxes`() {val p=ChatPaneLayout.pane(300,320,1.0,9,5);assertEquals(ChatPane(0,209,332,57),p);assertTrue(ChatPaneLayout.pane(50,320,.5,9,5).y>=0);assertFails {ChatPaneLayout.pane(300,320,0.0,9,5)}}
 @Test fun `scoreboard HUD editor entry has dedicated immutable layout`() {val f=FeatureSettings();val e=HudLayout.entries(f).single {it.key=="extra.scoreboard"};assertEquals(220,e.baseWidth);val next=HudLayout.update(f,e.key) {it.copy(x=17,y=35,scale=1.5,enabled=false)};assertEquals(17,next.scoreboardHud.hud.x);assertFalse(next.scoreboardHud.hud.enabled);assertEquals(f.player,next.player);assertEquals(f.themedChat,next.themedChat)}
 @Test fun `chat hooks preserve message security and vanilla text interaction`() {
  val root=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client")
  val mixin=root.resolve("mixin/ThemedChatMixin.kt").toFile().readText();assertTrue(mixin.contains("finally {ThemedChat.end(previous)}"));assertTrue(mixin.contains("original.call(g,font,ticks,mx,my,mode,click)"))
  val fills=root.resolve("mixin/ThemedChatFillMixin.kt").toFile().readText();assertFalse(fills.contains("handleTag"));assertFalse(fills.contains("handleMessage"))
  val feature=root.resolve("feature/ThemedChat.kt").toFile().readText();assertTrue(feature.contains("ThreadLocal"));assertTrue(feature.contains("x1-x0>4"));assertFalse(feature.contains("sendChat"))
 }
 @Test fun `nameplate retains styled components and source scaling`() {
  val s=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/WorldVisuals.kt").toFile().readText();assertTrue(s.contains("e.displayName.copy()"));assertTrue(s.contains("VisualChrome.distanceScale(distance)"));assertTrue(s.contains("ChromeDraw.panel"));assertFalse(s.contains("Policy.tag("))
 }
 @Test fun `scoreboard uses team objective hidden entries and formatting`() {
  val s=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/ScoreboardHud.kt").toFile().readText();for(token in listOf("team?.color?.orElse(null)?.displaySlot()","filterNot {it.isHidden}","take(15)","PlayerTeam.formatNameForTeam","entry.formatValue","original.extractRenderState","finally {g.disableScissor()}")) assertTrue(s.contains(token),token)
 }
}
