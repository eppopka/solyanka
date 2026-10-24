/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import com.google.gson.*
import java.util.UUID
import kotlin.test.*
import org.junit.jupiter.api.Test
class WorldVisualTest {
 @Test fun `defaults validate and round trip without changing old settings`() {
  val f=FeatureSettings();f.validate();assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
  assertEquals(f.worldVisuals,FeatureCodec.decode(JsonObject()).worldVisuals)
 }
 @Test fun `new config types reject strings nulls and fractional integers`() {
  for(value in listOf("\"64\"","null","1.5","999","-1")) assertFails {FeatureCodec.decode(JsonParser.parseString("""{"worldVisuals":{"titanium":{"range":$value}}}""").asJsonObject)}
 }
 @Test fun `all range and scale bounds enforced`() {
  assertFails {PlayerGlowSettings(range=15).validate()};assertFails {NametagSettings(scale=Double.NaN).validate()}
  assertFails {BlockOutlineSettings(width=0.0).validate()};assertFails {TitaniumSettings(maxMarkers=257).validate()}
  WorldVisualSettings(PlayerGlowSettings(range=256),NametagSettings(scale=3.0),BlockOutlineSettings(width=5.0),TitaniumSettings(range=96,maxMarkers=256)).validate()
 }
 @Test fun `NPC UUID policy supports explicit offline opt out`() {
  val npc=UUID.fromString("00000000-0000-2000-8000-000000000000")
  assertFalse(PlayerVisualPolicy.account(npc,false,true));assertTrue(PlayerVisualPolicy.account(npc,true,true));assertTrue(PlayerVisualPolicy.account(npc,false,false));assertTrue(PlayerVisualPolicy.account(UUID.randomUUID(),false,true))
 }
 private fun eligible(enabled:Boolean=true,self:Boolean=false,allow:Boolean=false,first:Boolean=true,alive:Boolean=true,spectator:Boolean=false,invisible:Boolean=false,real:Boolean=true,d:Double=100.0)=PlayerVisualPolicy.eligible(enabled,self,allow,first,alive,spectator,invisible,real,d,16)
 @Test fun `player policy excludes hidden dead NPC and self targets`() {
  assertTrue(eligible());assertFalse(eligible(enabled=false));assertFalse(eligible(self=true));assertFalse(eligible(self=true,allow=true));assertTrue(eligible(self=true,allow=true,first=false))
  assertFalse(eligible(alive=false));assertFalse(eligible(spectator=true));assertFalse(eligible(invisible=true));assertFalse(eligible(real=false))
 }
 @Test fun `distance is finite nonnegative and inclusive`() {assertTrue(eligible(d=256.0));assertFalse(eligible(d=256.01));assertFalse(eligible(d=-1.0));assertFalse(eligible(d=Double.NaN));assertFalse(eligible(d=Double.POSITIVE_INFINITY))}
 @Test fun `native silhouette forces opaque alpha`() {assertEquals(0xff123456.toInt(),PlayerVisualPolicy.glowColor(0x00123456))}
 @Test fun `tag text removes formatting and unsafe linebreaks`() {
  assertEquals("Alex  20/20 HP [5m]",PlayerVisualPolicy.tag("§aAlex\n",25.0,20.0,20.0,true,true))
  assertEquals("Alex",PlayerVisualPolicy.tag("Alex",Double.NaN,Double.NaN,0.0,true,true))
 }
 @Test fun `worldvisuals have no forced enabled module flags`() {
  val source=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/SolyankaClient.kt").toFile().readText()
  assertTrue(source.contains("config.enabled(module.info.id)"));assertFalse(source.contains("setEnabled(true)"))
 }
 @Test fun `overlay channels keep dungeon and mining independent`() {
  val text=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/WorldOverlay.kt").toFile().readText()
  assertTrue(text.contains("owner:String=\"dungeon\""));assertTrue(text.contains("channels=channels-owner"));assertTrue(text.contains("BlockSelection.collect(c,snapshot,filled)"));assertTrue(text.contains("setLineWidth(s.width)"))
 }
 @Test fun `outline callback accepts nullable Fabric selection`() {
  val text=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/BlockSelection.kt").toFile().readText();assertTrue(text.contains("BlockOutlineRenderState?"));assertTrue(text.contains("outline==null"))
 }
 @Test fun `state flag resets unconditionally on every extraction`() {
  val text=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/mixin/VisualEntityRendererMixin.kt").toFile().readText()
  assertTrue(text.contains("solyankaHideName=WorldVisuals.hideName(e)"));assertTrue(text.contains("!e.isCurrentlyGlowing"));assertTrue(text.contains("submitNameDisplay("))
 }
}
