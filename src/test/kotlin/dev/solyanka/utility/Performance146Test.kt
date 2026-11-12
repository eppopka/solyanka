/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import dev.solyanka.gfx.ToggleAnimation

import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.solyanka.model.HudLayout
import dev.solyanka.model.GuiCatalog
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.*
import org.junit.jupiter.api.Test

class Performance146Test {
 @Test fun `toggle ease has frame rate independent timing and instant mode`() {
  val direct=ToggleAnimation.step(0.0,true,.2,true)
  var small=0.0;repeat(20) {small=ToggleAnimation.step(small,true,.01,true)}
  assertTrue(abs(direct-small)<1e-12)
  assertTrue(direct in 0.0..1.0);assertEquals(1.0,ToggleAnimation.step(.1,true,0.0,false))
  assertEquals(0.0,ToggleAnimation.step(.9,false,0.0,false));assertTrue(ToggleAnimation.step(.9,false,.1,true)<.9)
  assertEquals(0xff000000.toInt(),ToggleAnimation.mix(0xff000000.toInt(),-1,0.0))
  assertEquals(-1,ToggleAnimation.mix(0xff000000.toInt(),-1,1.0))
 }
 @Test fun `TPS estimates measured elapsed game ticks capped and expires`() {
  val t=ServerTpsEstimator();assertNull(t.value(0));t.observe(100,0);assertNull(t.value(1))
  t.observe(120,1_000_000_000);assertEquals(20.0,t.value(1_000_000_000))
  t.reset();t.observe(100,0);t.observe(120,2_000_000_000);assertEquals(10.0,t.value(2_000_000_000))
  t.reset();t.observe(100,0);t.observe(150,1_000_000_000);assertEquals(20.0,t.value(1_000_000_000))
  assertNull(t.value(7_000_000_001));t.observe(1,8_000_000_000);assertNull(t.value(8_000_000_000))
  t.observe(21,9_000_000_000);assertEquals(20.0,t.value(9_000_000_000));t.reset();assertNull(t.value(9_000_000_000))
 }
 @Test fun `performance formats independent metrics and handles unavailable values`() {
  val s=PerformanceSettings()
  assertEquals("FPS: 90 | Ping: Local | TPS: —",PerformanceText.format(s,90,"Local",null))
  assertEquals("FPS: 90 | Ping: 35 ms | TPS: 19.9",PerformanceText.format(s,90,"35 ms",19.85))
  assertEquals("Ping: —",PerformanceText.format(s.copy(showFps=false,showTps=false),90,"—",20.0))
  assertEquals("TPS: 20.0",PerformanceText.format(s.copy(showFps=false,showPing=false),90,"Local",25.0))
  assertEquals("",PerformanceText.format(s.copy(showFps=false,showPing=false,showTps=false),90,"Local",20.0))
 }
 @Test fun `performance and animation configuration migrate and persist`() {
  val f=FeatureCodec.decode(JsonObject());assertTrue(f.clickGui.toggleAnimations)
  assertTrue(f.performance.showFps && f.performance.showPing && f.performance.showTps)
  val changed=f.copy(performance=f.performance.copy(showFps=false,showPing=false,hud=f.performance.hud.copy(x=123)),clickGui=f.clickGui.copy(toggleAnimations=false),hudGlow=f.hudGlow.copy(custom=f.hudGlow.custom.withValue("hud.performance",true)))
  assertEquals(changed,FeatureCodec.decode(Gson().toJsonTree(changed).asJsonObject))
  assertEquals("HUD",GuiCatalog.search("Performance").values.flatten().single {it.id=="hud.performance"}.category)
  assertEquals(listOf("extra.performance"),HudLayout.visibleEntries(f,setOf("hud.performance")).map {it.key})
  assertEquals(99,HudLayout.update(f,"extra.performance") {it.copy(x=99)}.performance.hud.x)
 }
 @Test fun `packet hook and frame aligned outline use same render state`() {
  val root=Path.of("src/client/kotlin/dev/solyanka/client")
  val mixin=Files.readString(root.resolve("mixin/PerformanceTimeMixin.kt"));assertTrue(mixin.contains("handleSetTime"));assertTrue(mixin.contains("packet.gameTime()"));assertTrue(mixin.contains("At(\"TAIL\")"))
  val hud=Files.readString(root.resolve("feature/PerformanceHud.kt"));assertTrue(hud.contains("hasSingleplayerServer()"));assertTrue(hud.contains("PerformanceText.format"))
  val outline=Files.readString(root.resolve("feature/BlockSelection.kt"));assertTrue(outline.contains("state.cameraRenderState.pos"));assertTrue(outline.contains("pos.x.toDouble()-camera.x"));assertTrue(outline.contains("submitShapeOutline"));assertTrue(outline.contains("pose.popPose()"))
 }
 @Test fun `performance color thresholds and unknown metrics`() {
  assertEquals(PerformanceText.GOOD,PerformanceText.metricColor("FPS: 120"))
  assertEquals(PerformanceText.WARN,PerformanceText.metricColor("FPS: 45"))
  assertEquals(PerformanceText.BAD,PerformanceText.metricColor("TPS: 12.0"))
  assertEquals(PerformanceText.WARN,PerformanceText.metricColor("Ping: 120 ms"))
  assertNull(PerformanceText.metricColor("Ping: Local"));assertNull(PerformanceText.metricColor("TPS: —"))
 }
}
