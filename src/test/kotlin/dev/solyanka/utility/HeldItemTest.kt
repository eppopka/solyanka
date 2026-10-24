/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import com.google.gson.*
import kotlin.math.abs
import kotlin.test.*
import org.junit.jupiter.api.Test
class HeldItemTest {
 @Test fun `all four modes validate roundtrip with old feature defaults`() {
  for(style in HeldItemPolicy.styles) {val f=FeatureSettings(heldItem=HeldItemSettings(style=style));f.validate();assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))}
  assertEquals(FeatureSettings(),FeatureCodec.decode(JsonObject()))
 }
 @Test fun `hand policy excludes inventory ground third person and missing contexts`() {
  val s=HeldItemSettings();assertTrue(HeldItemPolicy.hand("FIRST_PERSON_RIGHT_HAND",s));assertTrue(HeldItemPolicy.hand("FIRST_PERSON_LEFT_HAND",s))
  for(ctx in listOf("GUI","GROUND","FIXED","HEAD","THIRD_PERSON_RIGHT_HAND","THIRD_PERSON_LEFT_HAND","NONE","")) assertFalse(HeldItemPolicy.hand(ctx,s),ctx)
 }
 @Test fun `independent hand switches do not infer dominant arm`() {assertFalse(HeldItemPolicy.hand("FIRST_PERSON_RIGHT_HAND",HeldItemSettings(rightHand=false)));assertTrue(HeldItemPolicy.hand("FIRST_PERSON_LEFT_HAND",HeldItemSettings(rightHand=false)));assertFalse(HeldItemPolicy.hand("FIRST_PERSON_LEFT_HAND",HeldItemSettings(leftHand=false)))}
 @Test fun `invalid modes numbers and nonfinite values fail validation`() {
  for(s in listOf(HeldItemSettings(style="other"),HeldItemSettings(fill=.01),HeldItemSettings(fill=1.0),HeldItemSettings(intensity=0.0),HeldItemSettings(intensity=2.0),HeldItemSettings(speed=-1.0),HeldItemSettings(speed=5.0),HeldItemSettings(speed=Double.NaN),HeldItemSettings(fill=Double.POSITIVE_INFINITY))) assertFails {s.validate()}
 }
 @Test fun `typed codec rejects null string numbers and wrong booleans`() {
  for(raw in listOf("""{"heldItem":{"style":3}}""","""{"heldItem":{"fill":"0.5"}}""","""{"heldItem":{"color":1.25}}""","""{"heldItem":{"leftHand":null}}""","""{"heldItem":{"rightHand":1}}""")) assertFails {FeatureCodec.decode(JsonParser.parseString(raw).asJsonObject)}
 }
 @Test fun `tint RGB survives and fill controls packed alpha`() {
  val a=HeldItemPolicy.pixels(HeldItemSettings(color=0x12123456,fill=.32));assertEquals(0x123456,a[0] and 0xffffff);assertEquals(82,a[0] ushr 24);assertEquals(2,a.size)
  assertEquals(HeldItemPolicy.pixels(HeldItemSettings(color=0xff123456.toInt())).toList(),HeldItemPolicy.pixels(HeldItemSettings(color=0x00123456)).toList())
 }
 @Test fun `each shader style encodes an exact byte not a float variant`() {HeldItemPolicy.styles.forEachIndexed {i,style->assertEquals(i,(HeldItemPolicy.pixels(HeldItemSettings(style=style))[1] ushr 16) and 255)}}
 @Test fun `data texture quantization stays bounded over slider values`() {
  for(i in 0..100) {
   val s=HeldItemSettings(fill=.08+.77*i/100,intensity=.1+1.4*i/100,speed=4.0*i/100)
   val p=HeldItemPolicy.pixels(s);val fill=(p[0] ushr 24)/255.0;val amount=.1+((p[1] ushr 8) and 255)/255.0*1.4;val speed=(p[1] and 255)/255.0*4
   assertTrue(abs(s.fill-fill)<=.5/255+.000001);assertTrue(abs(s.intensity-amount)<=.7/255+.000001);assertTrue(abs(s.speed-speed)<=2.0/255+.000001)
  }
 }
 @Test fun `zero speed and intensity endpoints encode exactly`() {
  assertEquals(0,HeldItemPolicy.pixels(HeldItemSettings(speed=0.0))[1] and 255);assertEquals(255,HeldItemPolicy.pixels(HeldItemSettings(speed=4.0))[1] and 255)
  assertEquals(0,(HeldItemPolicy.pixels(HeldItemSettings(intensity=.1))[1] ushr 8) and 255);assertEquals(255,(HeldItemPolicy.pixels(HeldItemSettings(intensity=1.5))[1] ushr 8) and 255)
 }
 @Test fun `shader source reads bounded data texture and retains upstream effects`() {
  val f=java.nio.file.Path.of("src/main/resources/assets/solyanka/shaders/core/held_item.fsh").toFile().readText()
  for(token in listOf("texelFetch(Sampler5, ivec2(0, 0), 0)","texelFetch(Sampler5, ivec2(1, 0), 0)","style > 2.5","style > 1.5","style > 0.5","float smoke","tex.a < ALPHA_CUTOUT","shaderTime")) assertTrue(f.contains(token),token)
 }
 @Test fun `render hook changes only main item preparation not foil or outlines`() {
  val f=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/mixin/HeldItemFeatureMixin.kt").toFile().readText();assertTrue(f.contains("prepareMainSubmit("));assertFalse(f.contains("prepareFoilSubmit"));assertFalse(f.contains("prepareOutlineSubmit"));assertFalse(f.contains("GlRenderPass"))
 }
 @Test fun `texture lifecycle bounded cache and vanilla fallbacks are explicit`() {
  val f=java.nio.file.Path.of("src/client/kotlin/dev/solyanka/client/feature/HeldItemCosmetic.kt").toFile().readText()
  for(token in listOf("NativeImage(2,1,false)","types.size>=64","!ready","RenderSystem.isOnRenderThread()","textureManager.release(configId)","if(last==s && texture!=null)","return original")) assertTrue(f.contains(token),token)
  assertEquals(1,Regex("RenderPipelines.register").findAll(f).count())
 }
}
