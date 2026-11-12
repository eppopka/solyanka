/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model
import kotlin.test.*
import org.junit.jupiter.api.Test
class HudGuidesTest {
 @Test fun `center and other HUD edges snap with visible guide`() {
  val center=HudGuides.snap(97,40,10,10,200,100,emptyList())
  assertEquals(95,center.x);assertEquals(100,center.guideX)
  val edge=HudGuides.snap(49,10,10,10,200,100,listOf(HudBounds(50,70,15,10)))
  assertEquals(50,edge.x);assertEquals(50,edge.guideX)
 }
 @Test fun `far away and out of bounds positions do not snap unexpectedly`() {
  val result=HudGuides.snap(12,12,10,10,200,100,emptyList())
  assertNull(result.guideX);assertNull(result.guideY);assertEquals(12,result.x)
  val clamped=HudGuides.snap(400,-40,10,10,200,100,emptyList())
  assertEquals(190,clamped.x);assertEquals(0,clamped.y)
  val edge=HudGuides.snap(190,0,10,10,200,100,listOf(HudBounds(188,0,5,5)))
  assertEquals(190,edge.x);assertEquals(0,edge.y)
 }
}
