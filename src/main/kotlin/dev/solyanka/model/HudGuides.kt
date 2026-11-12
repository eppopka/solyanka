/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import kotlin.math.abs

/** Snap the dragged element's edges or center to the screen center and other HUD edges. */
object HudGuides {
 data class Snap(val x:Int,val y:Int,val guideX:Int?,val guideY:Int?)
 fun snap(x:Int,y:Int,w:Int,h:Int,screenWidth:Int,screenHeight:Int,other:List<HudBounds>,threshold:Int=4):Snap {
  fun axis(start:Int,size:Int,limit:Int,targets:List<Int>):Pair<Int,Int?> {
   val max=(limit-size).coerceAtLeast(0)
   if(start<=0 || start>=max) return start.coerceIn(0,max) to null // preserve flush screen-edge placement
   val offsets=listOf(0,size/2,size)
   val candidates=targets.flatMap { target->offsets.map {offset->target-offset to target} }
    .filter { (position,_)->position in 0..max && abs(position-start)<=threshold }
   val best=candidates.minByOrNull {abs(it.first-start)} ?: return start.coerceIn(0,max) to null
   return best.first to best.second
  }
  val (sx,gx)=axis(x,w,screenWidth,listOf(screenWidth/2)+other.flatMap {listOf(it.x,it.x+it.width/2,it.x+it.width)})
  val (sy,gy)=axis(y,h,screenHeight,listOf(screenHeight/2)+other.flatMap {listOf(it.y,it.y+it.height/2,it.y+it.height)})
  return Snap(sx,sy,gx,gy)
 }
}
