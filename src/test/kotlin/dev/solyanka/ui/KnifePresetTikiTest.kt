/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui
import dev.solyanka.utility.*
import dev.solyanka.config.ConfigStore
import com.google.gson.*
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import kotlin.test.*
class KnifePresetTikiTest {
 private val choice=KnifeChoice("weapon_knife_butterfly","am_fade",42,.0123)
 private fun source(name:String)=Path.of("src/client/kotlin/dev/solyanka/client/$name.kt").toFile().readText()
 @Test fun `named preset keeps exact model finish seed and wear`() {val p=KnifePresets.save(emptyList(),"  My   Butterfly  ",choice).single();assertEquals("My Butterfly",p.name);assertEquals(choice,p.choice())}
 @Test fun `same name replaces without adding duplicates`() {val first=KnifePresets.save(emptyList(),"Knife",choice);val next=KnifePresets.save(first,"KNIFE",choice.copy(seed=5));assertEquals(1,next.size);assertEquals(5,next.single().seed)}
 @Test fun `distinct presets preserve order and independent choices`() {val p=KnifePresets.save(KnifePresets.save(emptyList(),"One",choice),"Two",choice.copy(knife="weapon_knife_karambit"));assertEquals(listOf("One","Two"),p.map {it.name});assertNotEquals(p[0].knife,p[1].knife)}
 @Test fun `bad names rejected`() {for(n in listOf("", " ", "a".repeat(49),"bad\u0000name"))assertFails {KnifePresets.save(emptyList(),n,choice)}}
 @Test fun `bad choices rejected before persistence`() {for(c in listOf(choice.copy(knife="../bad"),choice.copy(seed=1001),choice.copy(wear=Double.NaN)))assertFails {KnifePresets.save(emptyList(),"One",c)}}
 @Test fun `preset limit allows overwrite but rejects growth`() {val p=(1..64).map {KnifePreset("Preset $it")};assertEquals(64,KnifePresets.save(p,"Preset 1",choice).size);assertFails {KnifePresets.save(p,"Preset 65",choice)}}
 @Test fun `duplicate preset config rejected case insensitively`() {assertFails {Cs2KnifeSettings(presets=listOf(KnifePreset("One"),KnifePreset("ONE"))).validate()}}
 @Test fun `old config has no presets and keeps bindings`() {val f=FeatureCodec.decode(JsonParser.parseString("""{"cs2Knife":{"knife":"weapon_knife_butterfly"}}""").asJsonObject);assertTrue(f.cs2Knife.presets.isEmpty());assertTrue(f.cs2Knife.bindings.isEmpty())}
 @Test fun `preset array type and null entries rejected`() {for(v in listOf("null","{}","[null]"))assertFails {FeatureCodec.decode(JsonParser.parseString("""{"cs2Knife":{"presets":$v}}""").asJsonObject)}}
 @Test fun `presets survive feature codec round trip`() {val f=FeatureSettings(cs2Knife=Cs2KnifeSettings(presets=KnifePresets.save(emptyList(),"One",choice)));assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))}
 @Test fun `presets survive config save and reload`() {val d=Files.createTempDirectory("knife-presets");try{val file=d.resolve("config.json");val store=ConfigStore(file){_,_->};val g=GeneralSettings(features=FeatureSettings(cs2Knife=Cs2KnifeSettings(presets=KnifePresets.save(emptyList(),"One",choice))));assertTrue(store.save(emptyMap(),general=g));val read=ConfigStore(file){_,_->};read.load();assertEquals(g,read.general())}finally{d.toFile().deleteRecursively()}}
 @Test fun `preset browser edits active selection and never needs item UUID to save`() {val s=source("ui/KnifeBrowserPanel");val method=s.substringAfter("private fun presets(").substringBefore("override fun mouseClicked");assertTrue("KnifePresets.save" in method);assertTrue("edit {preset.choice()}" in method);assertFalse("heldUuid()" in method);assertTrue("model.error==null" in method)}
 @Test fun `box topology consists of six distinct planar quads`() {val coords=listOf(Triple(0,0,0),Triple(1,0,0),Triple(1,1,0),Triple(0,1,0),Triple(0,0,1),Triple(1,0,1),Triple(1,1,1),Triple(0,1,1));assertEquals(6,OverlayBoxFaces.indices.size);val edges=mutableMapOf<Set<Int>,Int>();for(face in OverlayBoxFaces.indices){assertEquals(4,face.toSet().size);val v=face.map {coords[it]};assertTrue(v.map {it.first}.distinct().size==1 || v.map {it.second}.distinct().size==1 || v.map {it.third}.distinct().size==1);face.indices.forEach {i->val edge=setOf(face[i],face[(i+1)%4]);edges[edge]=(edges[edge]?:0)+1}};assertEquals(12,edges.size);assertTrue(edges.values.all {it==2})}
 @Test fun `Tiki visuals collect once per frame not tick published particles`() {val h=source("feature/HuntingFeatures");val render=h.substringAfter("fun collectTiki(");assertTrue("System.nanoTime()" in render);assertTrue("cameraRenderState.pos" in render);assertTrue("towerIndex<32" in render);assertFalse("WorldOverlay.publish(lines,fills,labels,\"tiki\")" in h);assertTrue("renderTowers=emptyList()" in h);val w=source("feature/WorldOverlay");assertEquals(1,Regex("HuntingFeatures.collectTiki").findAll(w).count())}
 @Test fun `translucent faces sorted and camera subtracted before float conversion`() {val w=source("feature/WorldOverlay");assertTrue("sortedByDescending {it.distance}" in w);assertTrue("(a.x-camera.x).toFloat()" in w);assertTrue("(s.a.x-camera.x).toFloat()" in w);assertFalse("pose.translate(-camera.x" in w);assertTrue("CompareOp.GREATER_THAN_OR_EQUAL" in w);assertTrue("CompareOp.ALWAYS_PASS,false" in w)}
}
