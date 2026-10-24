/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui
import dev.solyanka.utility.*
import dev.solyanka.config.ConfigStore
import com.google.gson.*
import java.nio.file.Path
import java.nio.file.Files
import org.junit.jupiter.api.Test
import kotlin.test.*
class KnifeBindingsTest {
 private val a="01234567-89ab-cdef-0123-456789abcdef"
 private val b="fedcba98-7654-3210-fedc-ba9876543210"
 private fun bound()=Cs2KnifeSettings(bindings=listOf(KnifeItemBinding(a,"Sword A","weapon_knife_butterfly","cu_butterfly",18,.12),KnifeItemBinding(b,"Sword B","weapon_knife_karambit","am_fade",9,.01)))
 private fun source(p:String)=Path.of("src/client/$p").toFile().readText()
 @Test fun `canonical UUID accepts dashed and undashed case insensitively`() {assertEquals("0123456789abcdef0123456789abcdef",KnifeBindings.canonical(a.uppercase()));assertEquals(KnifeBindings.canonical(a),KnifeBindings.canonical(a.replace("-","")))}
 @Test fun `malformed identity never resolves`() {for(v in listOf(null,"","Sword A","1-1-1-1-1","../uuid",a+" ",a.drop(1)))assertNull(KnifeBindings.canonical(v))}
 @Test fun `two items of same type can select different knife models`() {val s=bound();assertEquals("weapon_knife_butterfly",KnifeBindings.choice(s,a).knife);assertEquals("weapon_knife_karambit",KnifeBindings.choice(s,b).knife)}
 @Test fun `bindings retain skin wear and seed together`() {val c=KnifeBindings.choice(bound(),a);assertEquals("cu_butterfly",c.finish);assertEquals(18,c.seed);assertEquals(.12,c.wear)}
 @Test fun `unbound or missing UUID selects global defaults`() {val s=bound();val global=KnifeChoice(s.knife,s.finish,s.seed,s.wear);assertEquals(global,KnifeBindings.choice(s,null));assertEquals(global,KnifeBindings.choice(s,"ffffffffffffffffffffffffffffffff"))}
 @Test fun `disabled binding falls back without deleting saved choice`() {val s=bound().let {it.copy(bindings=it.bindings.map {b->b.copy(enabled=false)})};assertEquals(s.knife,KnifeBindings.choice(s,a).knife);assertEquals(2,s.bindings.size)}
 @Test fun `duplicate UUID aliases rejected`() {assertFails {bound().let {it.copy(bindings=it.bindings+it.bindings[0].copy(uuid=a.replace("-","").uppercase()))}.validate()}}
 @Test fun `invalid binding fields are rejected before saving`() {for(binding in listOf(KnifeItemBinding("bad"),KnifeItemBinding(a,label=""),KnifeItemBinding(a,knife="../bad"),KnifeItemBinding(a,seed=1001),KnifeItemBinding(a,wear=Double.NaN),KnifeItemBinding(a,wear=1.1)))assertFails {Cs2KnifeSettings(bindings=listOf(binding)).validate()}}
 @Test fun `binding count is bounded`() {assertFails {Cs2KnifeSettings(bindings=(0..128).map {KnifeItemBinding("%032x".format(it))}).validate()}}
 @Test fun `legacy config loads with no bindings and no arm shader`() {val s=FeatureCodec.decode(JsonParser.parseString("""{"cs2Knife":{"knife":"weapon_knife_butterfly"},"heldItem":{"style":"smoke"}}""").asJsonObject);assertTrue(s.cs2Knife.bindings.isEmpty());assertFalse(s.heldItem.arms)}
 @Test fun `all binding fields round trip through feature codec`() {val f=FeatureSettings(cs2Knife=bound(),heldItem=HeldItemSettings(arms=true));assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))}
 @Test fun `binding array has strict outer type and bounded size`() {for(value in listOf("null","{}","\"text\"","[null]"))assertFails {FeatureCodec.decode(JsonParser.parseString("""{"cs2Knife":{"bindings":$value}}""").asJsonObject)}}
 @Test fun `binding config survives save and reload without enabling modules`() {val dir=Files.createTempDirectory("knife-bindings");try {val file=dir.resolve("config.json");val s=ConfigStore(file){_,_->};val g=GeneralSettings(features=FeatureSettings(cs2Knife=bound(),heldItem=HeldItemSettings(arms=true)));assertTrue(s.save(mapOf("cosmetic.cs2_knife" to false),general=g));val read=ConfigStore(file){_,_->};read.load();assertEquals(g,read.general());assertFalse(read.enabled("cosmetic.cs2_knife"))}finally {dir.toFile().deleteRecursively()}}
 @Test fun `choice key matches Java rounding used for composed skin cache`() {val c=KnifeChoice("weapon_knife_karambit","",8,.12345);assertEquals("weapon_knife_karambit||8|1235",c.key())}
 @Test fun `hand shader defaults off and respects both side gates`() {val s=HeldItemSettings();assertFalse(HeldItemPolicy.arm(true,s));val a=s.copy(arms=true,rightHand=false);assertFalse(HeldItemPolicy.arm(true,a));assertTrue(HeldItemPolicy.arm(false,a));assertFalse(HeldItemPolicy.arm(false,a.copy(leftHand=false)))}
 @Test fun `browser is embedded searchable and contains item bindings tab`() {val s=source("kotlin/dev/solyanka/client/ui/KnifeBrowserPanel.kt");for(token in listOf(":EmbeddedPanel","Search knives","Search finishes","Item Bindings","Bind selection to held item","Use item UUID".replace("Use item","Uses item")))assertTrue(token in s,token);assertFalse("setScreen(" in s);assertFalse("renderBackground(" in s)}
 @Test fun `native hand hook is narrowly scoped and uses original player skin`() {val s=source("java/dev/solyanka/client/mixin/HeldItemArmMixin.java");assertTrue("renderHand(" in s);assertTrue("entityTranslucent" in s);assertTrue("wrapArm(original,skin,part==model.rightArm)" in s);assertFalse("extractRenderState" in s)}
 @Test fun `wrong previous knife cannot render while new UUID model loads`() {val s=source("java/dev/solyanka/client/cs2/Cs2Knives.java");assertTrue("current.key.equals(choice(stack).key()) && matches(stack)" in s);assertTrue("choice(mc.player.getMainHandItem())" in s);assertTrue("b.key.equals(requestedKey)" in s)}
 @Test fun `preview work is bounded serialized and does not replace live knife`() {val s=source("kotlin/dev/solyanka/client/ui/KnifePreviewCache.kt");assertTrue("jobs.size<2" in s);assertTrue("textures.size>40" in s);assertTrue("textureManager.release" in s);assertTrue("job.epoch!=generation" in s);val b=source("java/dev/solyanka/client/cs2/Cs2Knives.java").substringAfter("CompletableFuture<PreviewImage> preview(").substringBefore("private static Cs2Library openSource");assertTrue("},WORKER)" in b);assertFalse("current=" in b);assertFalse("upload(" in b)}
 @Test fun `preview uses real asset photos and original software renderer`() {val s=source("java/dev/solyanka/client/cs2/Cs2Knives.java");assertTrue("lib.preview(knifeId,finish)" in s);assertTrue("SoftwareRender.render(model,skin,384,256,180f)" in s);val a=Path.of("audit/stray-knife-menu-source/SoftwareRender.java").toFile().readText();val p=source("java/dev/solyanka/client/cs2/render/SoftwareRender.java");assertEquals(a.substringAfter("public final class SoftwareRender"),p.substringAfter("public final class SoftwareRender"))}
}
