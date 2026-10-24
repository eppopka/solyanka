/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import org.junit.jupiter.api.Test
import org.joml.Matrix4f
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class FinalCosmeticsTest {
 @Test fun `new settings default and old profile migration validate`() {
  val s=FeatureCodec.decode(null);s.validate();assertEquals(ItemAuraSettings(),s.itemAura);assertEquals(Cs2KnifeSettings(),s.cs2Knife)
  val old=com.google.gson.JsonParser.parseString("""{"heldItem":{"speed":0.5}}""").asJsonObject
  val next=FeatureCodec.decode(old);assertEquals(.5,next.heldItem.speed);assertEquals(s.itemAura,next.itemAura)
 }
 @Test fun `aura rejects invalid and non finite settings`() {
  for(s in listOf(ItemAuraSettings(style=0),ItemAuraSettings(shape=4),ItemAuraSettings(count=65),ItemAuraSettings(trail=17),ItemAuraSettings(radius=Double.NaN),ItemAuraSettings(size=.001),ItemAuraSettings(speed=0.0),ItemAuraSettings(rotX=181.0))) assertFails {s.validate()}
 }
 @Test fun `knife rejects invalid identifiers wear seed and offsets`() {
  for(s in listOf(Cs2KnifeSettings(seed=-1),Cs2KnifeSettings(wear=Double.NaN),Cs2KnifeSettings(knife="../x"),Cs2KnifeSettings(finish="../x"),Cs2KnifeSettings(offsetX=3.0),Cs2KnifeSettings(applyTo="all_players")))assertFails{s.validate()}
 }
 @Test fun `five aura styles produce deterministic finite triangle meshes`() {
  for(style in 1..5) {
   val m=ItemAuraMesh(ItemAuraSettings(style=style));val a=m.build(10.0);assertTrue(a.isNotEmpty());assertEquals(0,a.size%21);assertTrue(a.all {it.isFinite()});assertContentEquals(a,m.build(10.0))
   assertFalse(a.contentEquals(m.build(10.1)))
  }
 }
 @Test fun `aura shape modes are distinct and glow adds geometry`() {
  val counts=(1..3).map{ItemAuraMesh(ItemAuraSettings(shape=it,trail=0)).build(1.0).size}
  assertEquals(3,counts.toSet().size);assertTrue(ItemAuraMesh(ItemAuraSettings(glow=true)).build(1.0).size>ItemAuraMesh(ItemAuraSettings()).build(1.0).size)
 }
 @Test fun `aura max settings stay inside whole triangle budget`() {
  for(style in 1..5){val a=ItemAuraMesh(ItemAuraSettings(style=style,count=64,trail=16,quality=24,glow=true,ribbon=true)).build(30.0);assertTrue(a.size<=ItemAuraMesh.MAX_FLOATS);assertEquals(0,a.size%21)}
 }
 @Test fun `aura colors stay normalized with transparent rims`() {
  val a=ItemAuraMesh(ItemAuraSettings(rainbow=true)).build(4.0)
  var transparent=false
  for(i in a.indices step 7){for(j in 3..6)assertTrue(a[i+j] in 0f..1f);if(a[i+6]==0f)transparent=true}
  assertTrue(transparent)
 }
 @Test fun `source orbit formula retains radius and animation`() {
  val s=ItemAuraSettings(style=1);val m=ItemAuraMesh(s)
  val (x,y,z)=m.position(0,0.0);assertEquals(.272,x,1e-8);assertEquals(0.0,y,1e-8);assertEquals(0.0,z,1e-8)
 }
 @Test fun `ribbon adds triangles and no nan time accepted`() {
  val base=ItemAuraMesh(ItemAuraSettings()).build(1.0)
  assertTrue(ItemAuraMesh(ItemAuraSettings(ribbon=true)).build(1.0).size>base.size)
  assertFails{ItemAuraMesh(ItemAuraSettings()).build(Double.NaN)}
 }
 @Test fun `knife skinning bind pose and view transformation`() {
  val a=KnifeSkinning.transform(floatArrayOf(1f,2f,3f),floatArrayOf(0f,1f,0f),null,null,null,Matrix4f().translation(2f,0f,0f))
  assertContentEquals(floatArrayOf(3f,2f,3f,0f,1f,0f),a)
 }
 @Test fun `knife skinning uses and normalizes four weights`() {
  val a=KnifeSkinning.transform(floatArrayOf(0f,0f,0f),floatArrayOf(0f,2f,0f),intArrayOf(0,1,0,0),floatArrayOf(.25f,.75f,0f,0f),arrayOf(Matrix4f().translation(4f,0f,0f),Matrix4f().translation(0f,4f,0f)),Matrix4f())
  assertContentEquals(floatArrayOf(1f,3f,0f,0f,1f,0f),a)
 }
 @Test fun `knife skinning missing bone fallback and malformed channels`() {
  val a=KnifeSkinning.transform(floatArrayOf(1f,2f,3f),floatArrayOf(0f,0f,0f),intArrayOf(9,9,9,9),null,arrayOf(Matrix4f()),Matrix4f());assertContentEquals(floatArrayOf(1f,2f,3f,0f,1f,0f),a)
  assertFails{KnifeSkinning.transform(floatArrayOf(0f),floatArrayOf(),null,null,null,Matrix4f())}
  assertFails{KnifeSkinning.transform(floatArrayOf(Float.NaN,0f,0f),floatArrayOf(0f,1f,0f),null,null,null,Matrix4f())}
 }
 @Test fun `inventory is title free and armor row starts at padding`() {
  val m=InventoryHudLayout.metrics(InventoryHudSettings());assertEquals(m.pad,m.armorY)
  val s=Path.of("src/client/kotlin/dev/solyanka/client/feature/StrayInventoryHud.kt").readText();assertFalse("\"INVENTORY\"" in s);assertTrue("s.hud.color else LINE" in s)
 }
 @Test fun `inventory count uses footer only without armor and never overlaps main grid`() {
  val a=InventoryHudLayout.metrics(InventoryHudSettings(armor=false,count=true));val b=InventoryHudLayout.metrics(InventoryHudSettings(armor=false,count=false))
  assertEquals(b.height+11,a.height);assertEquals(a.mainY,b.mainY)
  val full=InventoryHudLayout.metrics(InventoryHudSettings());assertEquals(118,full.height)
 }
 @Test fun `new modules have actual settings and registered implementations`() {
  val client=Path.of("src/client/kotlin/dev/solyanka/client/SolyankaClient.kt").readText();assertTrue("ModuleInfo(ITEM_AURA" in client);assertTrue("ModuleInfo(CS2_KNIFE" in client)
  val ui=Path.of("src/client/kotlin/dev/solyanka/client/ui/FeatureUi.kt").readText();assertTrue("FinalCosmeticsUi.open(id,host,model)" in ui)
 }
 @Test fun `aura flush is after hand features but before matrix pop`() {
  val s=Path.of("src/client/kotlin/dev/solyanka/client/mixin/ItemAuraPassMixin.kt").readText()
  assertTrue("FeatureRenderDispatcher;renderAllFeatures" in s);assertTrue("shift=At.Shift.AFTER" in s);assertTrue("ItemAura.begin()" in s)
  val loader=Path.of("src/client/java/dev/solyanka/client/cs2/Cs2Knives.java").readText()
  assertFalse("openDownloaded" in loader);assertFalse("HttpClient" in loader);assertTrue("b.generation==generation" in loader);assertTrue("if(texturesUploaded)" in loader)
 }
}
