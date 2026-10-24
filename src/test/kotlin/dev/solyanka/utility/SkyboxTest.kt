/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import com.google.gson.*
import org.junit.jupiter.api.Test
import kotlin.test.*
import kotlin.math.*
import java.nio.file.Path
import javax.imageio.ImageIO
class SkyboxTest {
 @Test fun `all styles validate and roundtrip with old config defaults`() {
  for(style in SkyboxLogic.styles) {val f=FeatureSettings(skybox=SkyboxSettings(style=style));f.validate();assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))}
  assertEquals(FeatureSettings(),FeatureCodec.decode(JsonObject()))
 }
 @Test fun `invalid settings and nonfinite values fail`() {
  for(s in listOf(SkyboxSettings(style="bad"),SkyboxSettings(strength=-.1),SkyboxSettings(strength=1.1),SkyboxSettings(rotationSpeed=-1.0),SkyboxSettings(animationSpeed=4.1),SkyboxSettings(strength=Double.NaN),SkyboxSettings(rotationSpeed=Double.POSITIVE_INFINITY))) assertFails {s.validate()}
 }
 @Test fun `typed codec rejects null and mismatched skybox fields`() {
  for(raw in listOf("""{"skybox":{"style":4}}""","""{"skybox":{"tint":null}}""","""{"skybox":{"strength":".5"}}""","""{"skybox":{"skylessDimensions":1}}""","""{"skybox":{"color":1.5}}""")) assertFails {FeatureCodec.decode(JsonParser.parseString(raw).asJsonObject)}
 }
 @Test fun `disabled and dimension default never force End`() {
  for(style in SkyboxLogic.styles) assertFalse(SkyboxLogic.forceEnd(false,SkyboxSettings(style=style),false))
  for(none in listOf(false,true)) assertFalse(SkyboxLogic.forceEnd(true,SkyboxSettings(style="dimension",skylessDimensions=true),none))
 }
 @Test fun `skyless dimensions require independent opt in`() {
  for(style in listOf("end","decorated_end")) {val s=SkyboxSettings(style=style);assertTrue(SkyboxLogic.forceEnd(true,s,false));assertFalse(SkyboxLogic.forceEnd(true,s,true));assertTrue(SkyboxLogic.forceEnd(true,s.copy(skylessDimensions=true),true))}
 }
 @Test fun `decoration requires master and decorated style`() {
  for(style in SkyboxLogic.styles) {val s=SkyboxSettings(style=style);assertFalse(SkyboxLogic.decorated(false,s));assertEquals(style=="decorated_end",SkyboxLogic.decorated(true,s))}
 }
 @Test fun `tint disabled or zero strength is identity`() {
  for(c in listOf(0,0x12345678,-1,0xffaabbcc.toInt())) {assertEquals(c,SkyboxLogic.tint(c,SkyboxSettings()));assertEquals(c,SkyboxLogic.tint(c,SkyboxSettings(tint=true,strength=0.0)))}
 }
 @Test fun `tint preserves sunrise alpha including absence and ignores picker alpha`() {
  val s=SkyboxSettings(tint=true,color=0x00123456,strength=1.0)
  for(a in listOf(0,1,64,128,255)) {val from=(a shl 24) or 0xffffff;assertEquals((a shl 24) or 0x123456,SkyboxLogic.tint(from,s));assertEquals(SkyboxLogic.tint(from,s),SkyboxLogic.tint(from,s.copy(color=0xff123456.toInt())))}
 }
 @Test fun `tint round to nearest matches source channel mixing`() {assertEquals(0x7f808080,SkyboxLogic.tint(0x7f000000,SkyboxSettings(tint=true,color=-1,strength=.5)))}
 @Test fun `eight frame animation ping pongs at exact source boundaries`() {
  assertEquals(SkyFrames(0,1,0f),SkyboxLogic.frames(0.0,1.0));assertEquals(SkyFrames(1,2,0f),SkyboxLogic.frames(5.0,1.0));assertEquals(SkyFrames(7,7,0f),SkyboxLogic.frames(35.0,1.0));assertEquals(SkyFrames(6,7,0f),SkyboxLogic.frames(40.0,1.0));assertEquals(SkyboxLogic.frames(0.0,1.0),SkyboxLogic.frames(70.0,1.0));assertEquals(SkyFrames(0,1,.5f),SkyboxLogic.frames(2.5,1.0))
 }
 @Test fun `animation stays bounded at negative and large times`() {
  for(t in listOf(-140.0,-72.5,-1.0,1.0,34.999,35.001,69.999,1e10)) for(s in listOf(0.0,.05,1.0,4.0)) {val f=SkyboxLogic.frames(t,s);assertTrue(f.a in 0..7 && f.b in f.a..7 && f.blend in 0f..1f)}
  assertEquals(SkyboxLogic.frames(1.0,1.0),SkyboxLogic.frames(71.0,1.0));assertEquals(SkyboxLogic.frames(-1.0,1.0),SkyboxLogic.frames(69.0,1.0))
 }
 @Test fun `zero speed fixes rotation and black hole at source start`() {for(t in listOf(0.0,100.0,1e9)) {assertEquals(0f,SkyboxLogic.rotation(t,0.0));assertEquals(SkyFrames(0,1,0f),SkyboxLogic.frames(t,0.0))}}
 @Test fun `time inputs cannot generate NaN uniforms`() {assertFails {SkyboxLogic.frames(Double.NaN,1.0)};assertFails {SkyboxLogic.rotation(Double.POSITIVE_INFINITY,1.0)};assertFails {SkyboxLogic.frames(1.0,-1.0)};assertFails {SkyboxLogic.rotation(1.0,5.0)}}
 @Test fun `rotation matches source rate modulo full revolution`() {assertEquals(.09f,SkyboxLogic.rotation(100.0,1.0),.000001f);assertEquals(.18f,SkyboxLogic.rotation(100.0,2.0),.000001f);assertTrue(abs(SkyboxLogic.rotation(1e9,4.0))<2*PI)}
 @Test fun `source geometry has seven quads and consistent UV winding`() {
  val v=SkyboxLogic.vertices();assertEquals(28,v.size)
  for(q in v.chunked(4)) {assertEquals(listOf(0f to 0f,1f to 0f,1f to 1f,0f to 1f),q.map {it.u to it.v});assertTrue(q.all {it.position.x.isFinite() && it.position.y.isFinite() && it.position.z.isFinite()})}
 }
 @Test fun `six cube faces exactly cover source radius and opposing centers`() {
  val v=SkyboxLogic.vertices().take(24)
  for(point in v) for(c in listOf(point.position.x,point.position.y,point.position.z)) assertEquals(100.0,abs(c),.000001)
  val centers=v.chunked(4).map {q->q.map {it.position}.reduce {a,b->a+b}*.25}
  for(i in listOf(0,2,4)) {assertEquals(SkyPoint(0.0,0.0,0.0),centers[i]+centers[i+1]);assertEquals(10000.0,centers[i].dot(centers[i]),.000001)}
 }
 @Test fun `black hole patch is centered on inverse tilted source direction`() {
  val q=SkyboxLogic.vertices().takeLast(4).map {it.position};val center=q.reduce {a,b->a+b}*.25;val n=center.normalized()
  assertEquals(100.0,sqrt(center.dot(center)),.000001)
  for(v in q) assertEquals(100.0,v.dot(n),.000001)
  val c=cos(SkyboxLogic.TILT.toDouble());val s=sin(SkyboxLogic.TILT.toDouble());val world=SkyPoint(n.x,n.y*c-n.z*s,n.y*s+n.z*c);val expected=SkyPoint(.18,.86,.48).normalized()
  assertEquals(expected.x,world.x,.000001);assertEquals(expected.y,world.y,.000001);assertEquals(expected.z,world.z,.000001)
 }
 @Test fun `source textures have expected native dimensions and alpha`() {
  val root=Path.of("src/main/resources/assets/solyanka/textures/sky/end")
  for((prefix,count,size) in listOf(Triple("face",6,1024),Triple("hole",8,1536))) for(i in 0 until count) {val im=ImageIO.read(root.resolve("${prefix}_$i.png").toFile());assertNotNull(im);assertEquals(size,im.width);assertEquals(size,im.height);assertTrue(im.colorModel.hasAlpha())}
 }
 @Test fun `renderer resolves uploads before opening pass and uses source crossfade blends`() {
  val f=Path.of("src/client/kotlin/dev/solyanka/client/feature/SkyboxVisual.kt").toFile().readText()
  assertTrue(f.indexOf("val faceViews=")<f.indexOf(".createRenderPass("));assertTrue(f.indexOf("indices.getBuffer(42)")<f.indexOf(".createRenderPass("));assertFalse(f.contains("getMainRenderTarget"));assertFalse(f.contains("getModelViewStack"))
  for(t in listOf("TRANSLUCENT_PREMULTIPLIED_ALPHA","BlendFactor.ONE,BlendFactor.ONE","pass.drawIndexed(6,1,36,0,0)","vertices.slice()","owned.clear()","manager.release(it)","quads?.close()")) assertTrue(f.contains(t),t)
 }
 @Test fun `sky hook keeps native end draw and never cancels sky fog guards`() {
  val f=Path.of("src/client/kotlin/dev/solyanka/client/mixin/SkyboxMixin.kt").toFile().readText()
  assertFalse(f.contains("cancellable"));assertFalse(f.contains("ci.cancel"));assertTrue(f.contains("SkyboxVisual.render(renderTarget,solyankaSkyTime)"));assertTrue(f.contains("original.call(uniforms,matrix)"));assertTrue(f.contains("level.gameTime.toDouble()+partial"))
 }
}
