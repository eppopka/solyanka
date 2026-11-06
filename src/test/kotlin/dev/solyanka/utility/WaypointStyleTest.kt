/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class WaypointStyleTest {
 @Test fun `styles round trip without changing legacy packs`() {
  val old=Waypoint(GridPos(1,70,2),title="old")
  val styled=old.copy(style=WayStyle("fog",0xff9933ff.toInt(),2.0,.7),filled=true)
  val pack=mapOf("Hub" to listOf(old,styled))
  assertEquals(pack,WaypointCodec.decode(WaypointCodec.encode(pack)))
  assertEquals(pack,WaypointCodec.decode(WaypointCodec.encode(pack,true)))
 }
 @Test fun `unknown waypoint extensions survive edits`() {
  val root=com.google.gson.JsonParser.parseString(WaypointCodec.encode(mapOf("Hub" to listOf(Waypoint(GridPos(1,70,2),style=WayStyle()))))).asJsonObject
  val obj=root["Hub"].asJsonArray[0].asJsonObject
  obj.addProperty("authorExtension","keep");obj["blockPos"].asJsonObject.addProperty("posExtension",7)
  obj["aabb"].asJsonObject.addProperty("boxExtension",true);obj["solyankaStyle"].asJsonObject.addProperty("styleExtension","keep")
  val pack=WaypointCodec.decode(root.toString());val rewritten=WaypointCodec.encode(pack.mapValues {(_,v)->v.map {it.copy(title="edited")}})
  val result=com.google.gson.JsonParser.parseString(rewritten).asJsonObject["Hub"].asJsonArray[0].asJsonObject
  assertEquals("keep",result["authorExtension"].asString);assertEquals(7,result["blockPos"].asJsonObject["posExtension"].asInt)
  assertTrue(result["aabb"].asJsonObject["boxExtension"].asBoolean);assertEquals("keep",result["solyankaStyle"].asJsonObject["styleExtension"].asString)
 }
 @Test fun `effects are bounded animated and invalid data rejected`() {
  val style=WayStyle("pulse",0xff0000ff.toInt(),1.0,1.0)
  assertNotEquals(style.colorAt(0xffff0000.toInt(),0.0,0.0,0.0,0.0),style.colorAt(0xffff0000.toInt(),0.0,0.0,0.0,1.0))
  assertEquals(0xffff0000.toInt(),style.copy(intensity=0.0).colorAt(0xffff0000.toInt(),1.0,2.0,3.0,8.0))
  assertFails {style.copy(speed=Double.NaN).validate()};assertFails {style.copy(mode="invalid").validate()}
 }
 @Test fun `mineshaft variants use separate keys and unknown variants fail closed`() {
  val a=ContextParser.parse("SKYBLOCK",listOf("⏣ TOPA_1"),listOf("Area: Glacite Mineshaft"),0.0,0.0)
  val b=ContextParser.parse("SKYBLOCK",listOf("⏣ TOPA_2"),listOf("Area: Glacite Mineshaft"),0.0,0.0)
  assertEquals("TOPA_1",a.mineshaftVariant);assertNotEquals(WaypointLogic.areaKey(a),WaypointLogic.areaKey(b))
  assertNull(WaypointLogic.areaKey(a.copy(mineshaftVariant=null)))
 }
}
