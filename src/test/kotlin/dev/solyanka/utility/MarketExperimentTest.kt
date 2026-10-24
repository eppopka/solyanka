/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import kotlin.test.*
import org.junit.jupiter.api.Test
import com.google.gson.JsonParser
class MarketExperimentTest {
 private fun card(id:String,count:Int=1,name:String="")=ExperimentCard("minecraft:$id",name,count)
 @Test fun `experiment title and island guards`() {
  assertEquals(ExperimentType.CHRONO,ExperimentLogic.type("Chronomatron (1)",true))
  assertEquals(ExperimentType.NONE,ExperimentLogic.type("Chronomatron (1)",false))
  assertEquals(ExperimentType.NONE,ExperimentLogic.type("Chest",true))
 }
 @Test fun `chrono records repeated pulses and ordered clicks`() {
  val e=ExperimentLogic();e.reset(ExperimentType.CHRONO)
  e.update(10,card("red_terracotta"),emptyMap());e.update(10,card("red_stained_glass"),emptyMap());e.update(12,card("blue_terracotta"),emptyMap())
  assertTrue(e.blocked(10));e.update(49,card("clock"),emptyMap())
  assertEquals(setOf(10),e.next());assertEquals(setOf(12),e.second());assertTrue(e.blocked(12))
  e.click(12);assertEquals(setOf(10),e.next());e.click(10);assertEquals(setOf(12),e.next());e.click(12);assertTrue(e.next().isEmpty())
 }
 @Test fun `chrono same color repeated remains distinct`() {
  val e=ExperimentLogic();e.reset(ExperimentType.CHRONO)
  repeat(2) {e.update(10,card("red_terracotta"),emptyMap());e.update(10,card("red_stained_glass"),emptyMap())}
  e.update(49,card("clock"),emptyMap());assertEquals(setOf(10),e.next());assertTrue(e.second().isEmpty());e.click(10);assertEquals(setOf(10),e.next())
 }
 @Test fun `ultra captures visible dyes and sorts by count`() {
  val e=ExperimentLogic();e.reset(ExperimentType.ULTRA)
  val snapshot=mapOf(12 to card("red_dye",3).copy(dye=true),10 to card("blue_dye",1).copy(dye=true),11 to card("green_dye",2).copy(dye=true))
  e.update(49,card("glowstone"),snapshot);e.update(49,card("clock"),emptyMap())
  assertEquals(setOf(10),e.next());assertEquals(setOf(11),e.second());e.click(10);assertEquals(setOf(11),e.next())
 }
 @Test fun `new memory round resets ultra answers`() {
  val e=ExperimentLogic();e.reset(ExperimentType.ULTRA)
  e.update(10,card("red_dye",1).copy(dye=true),mapOf(10 to card("red_dye",1).copy(dye=true)));e.update(49,card("clock"),emptyMap());e.update(49,card("glowstone"),mapOf(15 to card("blue_dye",1).copy(dye=true)))
  e.update(49,card("clock"),emptyMap());assertEquals(setOf(15),e.next())
 }
 @Test fun `superpairs requires item name count and texture equality`() {
  val a=card("player_head").copy(name="Reward",texture="a");assertFalse(a.matches(a.copy(texture="b")));assertFalse(a.matches(a.copy(count=2)));assertFalse(a.matches(a.copy(name="Other")))
  val e=ExperimentLogic();e.reset(ExperimentType.SUPER);e.update(10,a,mapOf(10 to a));e.update(11,a,mapOf(10 to a,11 to a));assertEquals(0x7000ff00,e.colors[10]);assertEquals(e.colors[10],e.colors[11])
 }
 @Test fun `super hidden known pair gets yellow and is remembered`() {
  val e=ExperimentLogic();e.reset(ExperimentType.SUPER);val a=card("diamond",2,"XP")
  e.update(10,a,mapOf(10 to a));e.update(11,a,mapOf(10 to card("red_stained_glass"),11 to a));assertEquals(0x70ffff00,e.colors[10]);assertEquals(a,e.rewards[10]);assertFalse(e.blocked(12))
 }
 @Test fun `powerup color does not enter reward map`() {
  val e=ExperimentLogic();e.reset(ExperimentType.SUPER);e.update(10,card("diamond").copy(lore=listOf("POWERUP")),emptyMap());assertEquals(0x70ff00ff,e.colors[10]);assertTrue(e.rewards.isEmpty())
 }
 @Test fun `empty status and hidden cards never count as rewards`() {
  val e=ExperimentLogic();e.reset(ExperimentType.SUPER)
  listOf(card("air",0),card("clock"),card("bookshelf"),card("glowstone"),card("cauldron"),card("red_stained_glass_pane")).forEach {e.update(10,it,emptyMap())};assertTrue(e.rewards.isEmpty())
  e.update(10,card("glowstone",1,"Enchanted Book"),emptyMap());assertEquals(1,e.rewards.size)
 }
 @Test fun `reset clears all experimental state`() {
  val e=ExperimentLogic();e.reset(ExperimentType.SUPER);e.update(10,card("diamond"),emptyMap());e.reset();assertTrue(e.rewards.isEmpty());assertTrue(e.colors.isEmpty());assertEquals(ExperimentType.NONE,e.type);assertTrue(e.remember)
 }
 @Test fun `source quantity cases and malformed values`() {
  assertEquals(1250,PriceLogic.quantity(64,listOf("Stored: 1,250 / 2,000")));assertEquals(25,PriceLogic.quantity(1,listOf("Compost Available: 25")));assertEquals(12,PriceLogic.quantity(1,listOf("Syphon 12 more to level up!")));assertEquals(234,PriceLogic.quantity(1,listOf("Owned: 234 items")))
  assertEquals(64,PriceLogic.quantity(64,listOf("Owned: nonsense")));assertEquals(1,PriceLogic.quantity(64,listOf("Stored: 0 / 1")))
 }
 @Test fun `purchase cost parses only coin cost line`() {assertEquals(1250,PriceLogic.purchaseCost(listOf("Cost: 1,250 coins")));assertEquals(0,PriceLogic.purchaseCost(listOf("Cost: nope")))}
 @Test fun `unknown ingredients are not free crafting`() {
  val recipes=mapOf("A" to listOf(CraftRecipe(1,mapOf("B" to 2))))
  assertNull(PriceLogic.craft("A",emptyMap(),recipes));assertNull(PriceLogic.craft("B",mapOf("B" to MarketPrice(buy=10.0)),recipes))
 }
 @Test fun `craft output count and cheapest acquisition`() {
  val recipes=mapOf("A" to listOf(CraftRecipe(2,mapOf("B" to 10))))
  assertEquals(50.0,PriceLogic.craft("A",mapOf("B" to MarketPrice(buy=10.0)),recipes));assertEquals(20.0,PriceLogic.craft("A",mapOf("B" to MarketPrice(buy=10.0),"A" to MarketPrice(bin=20.0)),recipes))
 }
 @Test fun `indirect cycles terminate without stack overflow`() {
  val recipes=mapOf("A" to listOf(CraftRecipe(1,mapOf("B" to 1))),"B" to listOf(CraftRecipe(1,mapOf("A" to 1))))
  assertNull(PriceLogic.craft("A",emptyMap(),recipes));assertEquals(12.0,PriceLogic.craft("A",mapOf("B" to MarketPrice(buy=12.0)),recipes))
 }
 @Test fun `malformed recipes are not valid estimates`() {
  for(r in listOf(CraftRecipe(0,mapOf("B" to 1)),CraftRecipe(1,emptyMap()),CraftRecipe(1,mapOf("B" to -1)))) assertNull(PriceLogic.craft("A",mapOf("B" to MarketPrice(buy=12.0)),mapOf("A" to listOf(r))))
 }
 @Test fun `all source pricing sections merge`() {
  val p=PriceLogic.parsePricing("""{"auction":{"A":123},"bazaar":{"A":{"buy":120,"sell":110}},"npc":{"A":{"coin":10,"mote":2}}}""")
  assertEquals(MarketPrice(120.0,110.0,123.0,10.0,2.0),p["A"])
 }
 @Test fun `nonfinite and negative prices rejected`() {
  assertFails {PriceLogic.parsePricing("""{"auction":{"A":-1}}""")};assertFails {PriceLogic.parsePricing("""{"auction":{"A":1e999}}""")};assertFails {PriceLogic.parsePricing("{}")}
 }
 @Test fun `settings fill strict defaults and bounded enum values`() {
  assertTrue(FeatureCodec.decode(null).experiments.blockWrongClicks);assertFalse(FeatureCodec.decode(null).prices.network)
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"prices":{"burgers":6}}""").asJsonObject)}
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"suppression":{"vignette":4}}""").asJsonObject)}
  assertFails {FeatureCodec.decode(JsonParser.parseString("""{"experiments":{"superpairs":"true"}}""").asJsonObject)}
 }
}
