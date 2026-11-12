/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.gfx

import kotlin.math.*

/** Cached by the caller. Four subpixel samples create smooth, disjoint glow coverage. */
data class GlowBand(val x:Int,val y:Int,val width:Int,val height:Int,val level:Int)
object RoundedGlowGeometry {
 private val samples=doubleArrayOf(.25,.75)
 fun bands(w:Int,h:Int,radius:Int,spread:Int):List<GlowBand> {
  if(w<=0 || h<=0) return emptyList()
  require(w<=4096 && h<=4096 && spread in 1..12)
  val r=radius.coerceIn(0,minOf(w,h)/2).toDouble()
  fun distance(px:Double,py:Double):Double {
   val dx=abs(px-w/2.0)-(w/2.0-r);val dy=abs(py-h/2.0)-(h/2.0-r)
   return hypot(max(dx,0.0),max(dy,0.0))+min(max(dx,dy),0.0)-r
  }
  fun level(x:Int,y:Int):Int {
   // Quickly skip pixels certainly in the interior.
   if(x>=r+1 && x<w-r-1 && y>=r+1 && y<h-r-1) return 0
   var sum=0.0
   for(sy in samples) for(sx in samples) {
    val d=distance(x+sx,y+sy)
    if(d>=0 && d<spread) {val t=1-d/spread;sum+=t*t*.62}
   }
   return (sum/4*32).roundToInt().coerceIn(0,32)
  }
  data class Key(val x:Int,val width:Int,val level:Int)
  val out=mutableListOf<GlowBand>();var pending=linkedMapOf<Key,GlowBand>()
  for(y in -spread until h+spread) {
   val rows=linkedMapOf<Key,GlowBand>();var start= -spread;var prev=0
   for(x in -spread..w+spread) {
    val n=if(x==w+spread) 0 else level(x,y)
    if(n!=prev) {
     if(prev>0) {val key=Key(start,x-start,prev);rows[key]=pending.remove(key)?.let {it.copy(height=it.height+1)} ?: GlowBand(start,y,x-start,1,prev)}
     start=x;prev=n
    }
   }
   out.addAll(pending.values);pending=rows
  }
  out.addAll(pending.values);return out
 }
}
