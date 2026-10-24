/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class HeldItemLifecycleTest {
 private val root=Path.of("src/client/kotlin/dev/solyanka/client")
 private fun source()=root.resolve("feature/HeldItemCosmetic.kt").readText()
 @Test fun `item snippet inherits globals without duplicate startup binding`() {
  val s=source();assertTrue("RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET)" in s)
  assertFalse("RenderPipelines.ITEM_SNIPPET,RenderPipelines.GLOBALS_SNIPPET" in s)
 }
 @Test fun `fill pipeline matches Stray opaque color state`() {
  val s=source();assertTrue("withColorTargetState(ColorTargetState.DEFAULT)" in s);assertFalse("BlendFunction.TRANSLUCENT" in s)
 }
 @Test fun `pipeline metadata registers during client init not first render alone`() {
  val s=root.resolve("SolyankaClient.kt").readText();assertTrue(s.indexOf("HeldItemCosmetic.register()")<s.indexOf("modules.register("))
 }
 @Test fun `native pipeline validation precedes readiness and texture uploads`() {
  val s=source();assertTrue(s.indexOf("precompilePipeline(pipeline)")<s.indexOf("if(last==s && lastPhase==phase && texture!=null)"))
  assertTrue("if(!result.isValid) {ready=false;failed=true;" in s)
  assertTrue(s.indexOf("if(!result.isValid)")<s.indexOf("current.upload()"))
 }
 @Test fun `render head prepares resources with no tick dependency`() {
  val mixin=root.resolve("mixin/TemporalBlurMixin.kt").readText()
  assertTrue("at=[At(\"HEAD\")]" in mixin);assertTrue("HeldItemCosmetic.prepareFrame()" in mixin)
  assertFalse("HeldItemCosmetic.tick()" in root.resolve("SolyankaClient.kt").readText())
 }
 @Test fun `data texels initialized before constructor auto upload`() {
  val s=source();assertTrue(s.indexOf("image.setPixel(0,0,values[0])")<s.indexOf("DynamicTexture({"))
  assertTrue("pixels?.isClosed==true" in s);assertTrue("texture=null;last=null;" in s)
 }
 @Test fun `runtime status exposes fallback reasons without changing server state`() {
  val s=source();for(field in listOf("eligible=","replaced=","notReady=","incompatible=","ready=")) assertTrue(field in s)
  assertTrue("literal(\"helditemstatus\")" in root.resolve("SolyankaClient.kt").readText())
  assertFalse("sendCommand" in s);assertFalse("sendChat" in s)
 }
}
