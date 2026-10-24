/* SPDX-License-Identifier: GPL-3.0-only; Stray HeldItemShader/StrayConfig CC0 adaptation. */
package dev.solyanka.utility
import kotlin.math.roundToInt
data class HeldItemSettings(val style:String="smoke",val color:Int=0xff4fd6ea.toInt(),val fill:Double=.32,val intensity:Double=.55,val speed:Double=1.0,val rightHand:Boolean=true,val leftHand:Boolean=true,val arms:Boolean=false) {
 fun validate() {require(style in HeldItemPolicy.styles);require(fill.isFinite() && fill in .08.. .85);require(intensity.isFinite() && intensity in .1..1.5);require(speed.isFinite() && speed in 0.0..4.0)}
}
object HeldItemPolicy {
 val styles=listOf("smoke","stars","portal","galaxy")
 val labels=listOf("Smoke","Stars","Portal","Galaxy")
 fun hand(context:String,s:HeldItemSettings)=when(context) {"FIRST_PERSON_RIGHT_HAND"->s.rightHand;"FIRST_PERSON_LEFT_HAND"->s.leftHand;else->false}
 fun arm(right:Boolean,s:HeldItemSettings)=s.arms && if(right)s.rightHand else s.leftHand
 /** Two RGBA8 texels: tint RGB + fill alpha, then style/intensity/speed. */
 fun pixels(s:HeldItemSettings):IntArray {
  s.validate();return intArrayOf((s.color and 0xffffff) or ((s.fill*255).roundToInt() shl 24),0xff000000.toInt() or (styles.indexOf(s.style) shl 16) or (((s.intensity-.1)/1.4*255).roundToInt().coerceIn(0,255) shl 8) or (s.speed/4*255).roundToInt().coerceIn(0,255))
 }
}
