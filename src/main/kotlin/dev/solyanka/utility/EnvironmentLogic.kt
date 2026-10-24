/* SPDX-License-Identifier: GPL-3.0-only
 * Stray CustomAmbience/AspectFov/CustomFog CC0 adaptation; see NOTICE. */
package dev.solyanka.utility
import kotlin.math.*
data class EnvironmentSettings(val clock:Int=0,val weather:Int=0,val precipitation:Boolean=false,val gradient:Double=.5,val aspectRatio:Double=1.0,val fogStart:Double=.2,val fogEnd:Double=1.0,val fogDensity:Double=.5,val fogColor:Int=0xffaabbcc.toInt()) {
 fun validate() {require(clock in 0..6 && weather in 0..4);require(gradient.isFinite() && gradient in .1..1.0);require(aspectRatio.isFinite() && aspectRatio in .3..3.0);require(listOf(fogStart,fogEnd,fogDensity).all {it.isFinite() && it in 0.0..1.0})}
}
object EnvironmentLogic {
 fun time(clock:Int):Long?=when(clock) {1->23041L;2->1000L;3->6000L;4->12610L;5->13000L;6->18000L;else->null}
 fun rain(weather:Int,original:Float):Float=when(weather) {1->0f;2,3->1f;4->.9f;else->original}
 fun thunder(weather:Int,original:Float):Float=when(weather) {3->1f;1,2,4->0f;else->original}
 data class Fog(val start:Float,val end:Float,val red:Float,val green:Float,val blue:Float,val alpha:Float)
 fun fog(s:EnvironmentSettings,chunks:Int,sodium:Boolean):Fog {
  val view=max(32f,chunks*16f);val start=view*s.fogStart.coerceIn(0.0,.95).toFloat()
  val far=max(start+4f,view*max(s.fogStart.coerceIn(0.0,.95)+.04,s.fogEnd).coerceIn(.05,1.0).toFloat());val density=s.fogDensity.toFloat()
  val end=if(!sodium && density>0) far+(start+(far-start)*.28f-far)*density else far
  return Fog(start,end,((s.fogColor shr 16) and 255)/255f,((s.fogColor shr 8) and 255)/255f,(s.fogColor and 255)/255f,if(!sodium && density>0)1f else density)
 }
}
