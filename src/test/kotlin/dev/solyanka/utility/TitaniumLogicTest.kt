/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import kotlin.test.*
import org.junit.jupiter.api.Test
class TitaniumLogicTest {
 @Test fun `progress accepts percentages ratios and done`() {assertEquals(.5,TitaniumLogic.progress("50%"));assertEquals(.25,TitaniumLogic.progress("1/4"));assertEquals(1.0,TitaniumLogic.progress("Done"));assertEquals(1.0,TitaniumLogic.progress("✔"))}
 @Test fun `invalid progress fails closed`() {for(raw in listOf("101%","-1%","NaN%","2/0","5/4","999999999999999999999/2","abc")) assertNull(TitaniumLogic.progress(raw),raw)}
 @Test fun `commissions parse inline and multiline only inside widget`() {
  assertEquals(listOf(MiningCommission("Lava Springs Titanium",.5),MiningCommission("Titanium Miner",.25)),TitaniumLogic.commissions(listOf("Titanium Miner: 1%","§aCommissions:","Lava Springs Titanium: 50%","Titanium Miner","1/4","Powder: 100","Royal Mines Titanium: 0%")))
 }
 @Test fun `standalone completion checkmark stays a progress token`() {assertEquals(listOf(MiningCommission("Titanium Miner",1.0)),TitaniumLogic.commissions(listOf("Commissions","Titanium Miner","✔")))}
 @Test fun `unfinished regional commissions restrict region`() {
  val f=TitaniumLogic.filter(listOf(MiningCommission("Lava Springs Titanium",.5),MiningCommission("Royal Mines Titanium",1.0)))
  assertTrue(f.allows(TitaniumRegion.LAVA_SPRINGS));assertFalse(f.allows(TitaniumRegion.ROYAL_MINES));assertFalse(f.allows(null))
 }
 @Test fun `generic titanium and all toggle permit unknown region`() {assertTrue(TitaniumLogic.filter(listOf(MiningCommission("Titanium Miner",.2))).allows(null));assertTrue(TitaniumLogic.filter(emptyList(),true).allows(null))}
 @Test fun `unknown commissions completed titanium and mithril cannot activate scan`() {
  assertFalse(TitaniumLogic.filter(listOf(MiningCommission("Unknown Titanium",.1))).active)
  assertFalse(TitaniumLogic.filter(listOf(MiningCommission("Titanium Miner",1.0),MiningCommission("Mithril Miner",.5))).active)
 }
 @Test fun `nearest marker and radius constraint preserve blockers`() {
  val a=TitaniumMarker(TitaniumRegion.ROYAL_MINES,0,0,0);val b=TitaniumMarker(null,10,0,0)
  assertEquals(TitaniumRegion.ROYAL_MINES,TitaniumLogic.region(listOf(a,b),0,0,0));assertNull(TitaniumLogic.region(listOf(a,b),9,0,0));assertNull(TitaniumLogic.region(listOf(a),33,0,0));assertEquals(TitaniumRegion.ROYAL_MINES,TitaniumLogic.region(listOf(a),32,0,0))
 }
 @Test fun `mining areas require SkyBlock and explicit known location`() {assertTrue(TitaniumLogic.miningArea(true,"Dwarven Mines"));assertFalse(TitaniumLogic.miningArea(false,"Dwarven Mines"));assertFalse(TitaniumLogic.miningArea(true,"Hub"))}
 @Test fun `negative chunk coordinates and vertical bounds are respected`() {
  val jobs=TitaniumLogic.sections(-1,0,-1,16,-64,319)
  assertEquals(setOf(-2,-1,0),jobs.map {it.x}.toSet());assertEquals(setOf(-1,0,1),jobs.map {it.y}.toSet());assertTrue(TitaniumLogic.sections(0,500,0,16,-64,319).isEmpty())
 }
 @Test fun `max scan queue remains bounded and sorted nearest first`() {
  val jobs=TitaniumLogic.sections(0,128,0,96,-64,319);assertTrue(jobs.size<=13*13*13)
  fun d(p:GridPos):Long {val x=p.x*16+8L;val y=p.y*16+8L-128;val z=p.z*16+8L;return x*x+y*y+z*z}
  assertEquals(jobs.sortedBy(::d),jobs);assertFails {TitaniumLogic.sections(0,0,0,100,-64,319)}
 }
 @Test fun `sphere distance uses inclusive boundary and long math`() {assertTrue(TitaniumLogic.within(96,0,0,0,0,0,96));assertFalse(TitaniumLogic.within(96,1,0,0,0,0,96));assertFalse(TitaniumLogic.within(30000000,0,0,0,0,0,96))}
 @Test fun `scanner budgets loaded-only access and own overlay are wired`() {
  val text=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/TitaniumScanner.kt").toFile().readText()
  assertTrue(text.contains("budget=32768"));assertTrue(text.contains("sections++<64"));assertTrue(text.contains("ChunkStatus.FULL,false"));assertTrue(text.contains("candidates.size<4096"));assertTrue(text.contains("owner=\"titanium\""));assertFalse(text.contains("ChunkStatus.FULL,true"))
 }
 @Test fun `all source marker names deserialize and preserve 320 nodes`() {
  val array=com.google.gson.JsonParser.parseReader(javaClass.getResourceAsStream("/assets/solyanka/mining/titanium-regions.json")!!.reader()).asJsonArray
  assertEquals(320,array.size());array.forEach {val j=it.asJsonObject;if(!j["region"].isJsonNull) TitaniumRegion.valueOf(j["region"].asString)}
 }
}
