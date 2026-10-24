/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class GuiPerformanceTest {
 @Test fun `coalesced bands retain every pixel and translucent layering`() {
  for(w in listOf(1,2,3,17,136,400)) for(h in listOf(1,2,3,9,20,28,420)) for(r in listOf(-1,0,1,4,7,500)) {
   val old=IntArray(w*h);val new=IntArray(w*h)
   fun fill(a:IntArray,l:Int,t:Int,right:Int,b:Int,ink:Int) {for(y in t until b) for(x in l until right) a[y*w+x]=a[y*w+x]*3+ink}
   val radius=r.coerceIn(0,minOf(w,h)/2)
   for(y in 0 until h) {
    val inset=VisualChrome.inset(y,h,radius);val right=w-inset
    if(right<=inset) continue
    if(y==0 || y==h-1) fill(old,inset,y,right,y+1,2)
    else {fill(old,inset,y,right,y+1,1);fill(old,inset,y,inset+1,y+1,2);fill(old,right-1,y,right,y+1,2)}
   }
   for(b in PanelGeometry.bands(w,h,r)) {
    val l=b.inset;val right=w-l;if(right<=l) continue
    if(b.borderOnly) fill(new,l,b.top,right,b.bottom,2)
    else {fill(new,l,b.top,right,b.bottom,1);fill(new,l,b.top,l+1,b.bottom,2);fill(new,right-1,b.top,right,b.bottom,2)}
   }
   assertContentEquals(old,new,"${w}x$h radius $r")
  }
 }
 @Test fun `tall settings panel no longer generates quads for every row`() {
  val old=2+3*(420-2);val current=PanelGeometry.fillCalls(PanelGeometry.bands(400,420,7))
  assertEquals(23,current);assertTrue(current<50,"$current vs $old")
  assertTrue(current*20<old)
 }
 @Test fun `empty panels submit nothing and geometry depends on corners not height`() {
  assertEquals(emptyList(),PanelGeometry.bands(0,50,7));assertEquals(emptyList(),PanelGeometry.bands(5,0,7))
  assertEquals(PanelGeometry.fillCalls(PanelGeometry.bands(400,420,7)),PanelGeometry.fillCalls(PanelGeometry.bands(400,4200,7)))
 }
 @Test fun `feature list remains rendered behind modal settings`() {
  val s=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt").readText()
  assertTrue("super.extractRenderState(g,mx,my,delta)" in s);assertFalse("if(panel==null) super.extractRenderState" in s)
  assertTrue("current.render(g,mx,my,delta)" in s)
 }
}
