/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.math.exp
import kotlin.math.roundToInt

/** Exponential ease-out: independent of how a time interval is divided into frames. */
object ToggleAnimation {
 fun step(current:Double,active:Boolean,seconds:Double,enabled:Boolean):Double {
  val target=if(active) 1.0 else 0.0
  if(!enabled) return target
  if(!seconds.isFinite() || seconds<=0.0) return current.coerceIn(0.0,1.0)
  return (target+(current-target)*exp(-12.0*seconds)).coerceIn(0.0,1.0)
 }
 fun mix(a:Int,b:Int,amount:Double):Int {
  val t=amount.coerceIn(0.0,1.0)
  fun channel(shift:Int)=(((a ushr shift and 255)*(1.0-t)+(b ushr shift and 255)*t).roundToInt().coerceIn(0,255)) shl shift
  return channel(24) or channel(16) or channel(8) or channel(0)
 }
}
