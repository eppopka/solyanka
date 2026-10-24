/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
/** Session-local, world-independent phase. A speed edit changes velocity, never position. */
class HeldItemClock {
 private var previous:Long?=null
 private var velocity=0.0
 private var phase=0.0
 fun sample(nowNanos:Long,speed:Double,active:Boolean=true):Float {
  require(speed.isFinite() && speed in 0.0..4.0)
  previous?.let {before->
   val elapsed=nowNanos-before
   // nanoTime subtraction handles signed wrap; a genuinely backwards sample adds nothing.
   if(elapsed>0) phase+=elapsed.toDouble()/1_000_000_000.0/1200.0*velocity
  }
  previous=nowNanos;velocity=if(active) speed else 0.0
  return phase.toFloat()
 }
 /** NativeImage.setPixel takes ARGB; its RGBA bytes preserve all float bits on upload. */
 companion object {
  fun texel(phase:Float):Int {
   require(phase.isFinite() && phase>=0f)
   val bits=phase.toRawBits()
   return ((bits and 255) shl 16) or (((bits ushr 8) and 255) shl 8) or ((bits ushr 16) and 255) or ((bits ushr 24) shl 24)
  }
  fun decodeTexel(argb:Int):Float=Float.fromBits(((argb ushr 16) and 255) or (((argb ushr 8) and 255) shl 8) or ((argb and 255) shl 16) or ((argb ushr 24) shl 24))
 }
}
