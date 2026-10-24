/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import kotlin.test.*
import org.junit.jupiter.api.Test
import com.google.gson.*
import java.security.MessageDigest
class DungeonAssetTest {
 private fun bytes(name:String)=javaClass.getResourceAsStream("/assets/solyanka/$name")!!.use {it.readBytes()}
 private fun json(name:String)=JsonParser.parseString(bytes(name).toString(Charsets.UTF_8))
 @Test fun `bundled room and solver data match pinned Odin bytes`() {
  mapOf("rooms.json" to "aea05d2eafc753c9b782652165ff3424da485d3a6ca0f62704c019e27cd70947","puzzles/ice-fill-floors.json" to "c88a3ac64777ff7d682711207242bfae233a4bced74c9e87b4c57cddab854a5a","puzzles/creeper-beams-solutions.json" to "8ad2b145f36933e18cf14635ed690d932baa952d906a2f96beb31b267a4849d1").forEach {(name,sha)->assertEquals(sha,MessageDigest.getInstance("SHA-256").digest(bytes(name)).joinToString("") {"%02x".format(it)})}
 }
 @Test fun `all Ice Fill variants have valid identifier pairs and routes`() {val o=json("puzzles/ice-fill-floors.json").asJsonObject;assertEquals(3,o["identifier"].asJsonArray.size());for(floor in 0..2) {
  val ids=o["identifier"].asJsonArray[floor].asJsonArray
  listOf("easy","hard").forEach {mode->val routes=o[mode].asJsonArray[floor].asJsonArray;assertEquals(ids.size(),routes.size());routes.forEach {assertTrue(it.asJsonArray.size()>1);it.asJsonArray.forEach {p->listOf("x","y","z").forEach {assertTrue(p.asJsonObject[it].asJsonPrimitive.isNumber)}}}}
  ids.forEach {assertEquals(2,it.asJsonArray.size())}
 }}
 @Test fun `beam solutions are six-coordinate pairs and known solver rooms are single tile`() {json("puzzles/creeper-beams-solutions.json").asJsonArray.forEach {assertEquals(6,it.asJsonArray.size());it.asJsonArray.forEach {assertTrue(it.asJsonPrimitive.isNumber)}};val rooms=json("rooms.json").asJsonArray.map {it.asJsonObject};listOf("Ice Fill","Higher Blaze","Lower Blaze","Creeper Beams").forEach {name->assertEquals("1x1",rooms.single {it["name"].asString==name}["shape"].asString)}}
}
