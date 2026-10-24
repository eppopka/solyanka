/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import org.junit.jupiter.api.Test
import kotlin.test.*
import com.google.gson.JsonParser
class FrameBlendHistoryTest {
 @Test fun `first frame is a single raw sample`() {val h=FrameBlendHistory(24);assertEquals(0,h.push(1.0));assertEquals(1,h.samples(1.0,60,1.0).size)}
 @Test fun `exposure tracks actual frame times and refresh`() {
  val h=FrameBlendHistory(24);for(i in 0..20) h.push(1+i/144.0)
  val samples=h.samples(1+20/144.0,60,1.0)
  assertEquals(3,samples.size);assertEquals(1.0/60,samples.sumOf {it.weight.toDouble()},1e-7)
 }
 @Test fun `below refresh one frame strength has no temporal tail`() {
  val h=FrameBlendHistory(24);for(i in 0..20) h.push(1+i/30.0)
  assertEquals(1,h.samples(1+20/30.0,144,1.0).size)
 }
 @Test fun `long gap clears history instead of blending stale world`() {
  val h=FrameBlendHistory(24);h.push(1.0);h.push(1.01);assertEquals(0,h.push(2.0));assertEquals(1,h.samples(2.0,60,2.0).size)
 }
 @Test fun `nonmonotonic clocks reseed`() {
  val h=FrameBlendHistory(24);h.push(2.0);h.push(2.01);assertEquals(0,h.push(2.01));assertEquals(1,h.samples(2.01,60,2.0).size)
 }
 @Test fun `compaction preserves total weight with twelve samplers`() {
  val h=FrameBlendHistory(24);for(i in 0 until 24) h.push(1+i*.001)
  val samples=h.samples(1.023,60,2.0)
  assertEquals(12,samples.size);assertEquals(24*.001,samples.sumOf {it.weight.toDouble()},1e-7)
  assertEquals(0,samples.first().index);assertEquals(23,samples.last().index)
 }
 @Test fun `ring overwrite never references unavailable slots`() {
  val h=FrameBlendHistory(3);for(i in 0..100) assertEquals(i%3,h.push(1+i*.005))
  val samples=h.samples(1.5,60,2.0);assertEquals(3,samples.size);assertEquals(3,samples.map {it.index}.distinct().size)
 }
 @Test fun `zero strength and invalid refresh skip the pass`() {
  val h=FrameBlendHistory(3);h.push(1.0);assertTrue(h.samples(1.0,60,0.0).isEmpty());assertTrue(h.samples(1.0,0,1.0).isEmpty())
 }
 @Test fun `reset frees clock state`() {val h=FrameBlendHistory(3);h.push(1.0);h.reset();assertTrue(h.samples(1.0,60,1.0).isEmpty());assertEquals(0,h.push(2.0))}
 @Test fun `history targets fit the byte budget for all supported sizes`() {
  for((w,h) in listOf(1 to 1,1920 to 1080,2560 to 1440,3840 to 2160)) for(bytes in listOf(4,8)) {
   val n=FrameBlendHistory.capacity(w,h,bytes);assertTrue(n in 0..24)
   if(n>0) assertTrue((n+1L)*w*h*bytes<=FrameBlendHistory.BYTE_BUDGET)
  }
  assertEquals(0,FrameBlendHistory.capacity(0,1080,4));assertEquals(0,FrameBlendHistory.capacity(7680,4320,4))
 }
 @Test fun `new default is frame blending but stored accumulation settings survive`() {
  assertEquals("frame_blend",MotionBlurSettings().mode)
  val old=FeatureCodec.decode(JsonParser.parseString("""{"motionBlur":{"mode":"accum_mix","strength":1.5}}""").asJsonObject)
  assertEquals("accum_mix",old.motionBlur.mode);assertEquals(1.5,old.motionBlur.strength)
 }
 @Test fun `bad clock and capacity inputs fail before mutating history`() {
  assertFails {FrameBlendHistory(0)};assertFails {FrameBlendHistory(25)}
  val h=FrameBlendHistory(2);assertFails {h.push(Double.NaN)};assertFails {h.push(-1.0)};assertEquals(0,h.push(1.0))
 }
}
