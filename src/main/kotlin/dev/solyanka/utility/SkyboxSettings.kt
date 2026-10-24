/* SPDX-License-Identifier: GPL-3.0-only
 * Stray WorldTint/EndSkyDecor CC0 adaptation; see NOTICE and source manifest. */
package dev.solyanka.utility
import kotlin.math.*
data class SkyboxSettings(val style:String="decorated_end",val tint:Boolean=false,val color:Int=0xff1b4f8a.toInt(),val strength:Double=.70,val rotationSpeed:Double=1.0,val animationSpeed:Double=1.0,val skylessDimensions:Boolean=false) {
 fun validate() {require(style in SkyboxLogic.styles);require(strength.isFinite() && strength in 0.0..1.0);require(rotationSpeed.isFinite() && rotationSpeed in 0.0..4.0);require(animationSpeed.isFinite() && animationSpeed in 0.0..4.0)}
}
data class SkyPoint(val x:Double,val y:Double,val z:Double) {
 operator fun plus(o:SkyPoint)=SkyPoint(x+o.x,y+o.y,z+o.z)
 operator fun times(n:Double)=SkyPoint(x*n,y*n,z*n)
 fun dot(o:SkyPoint)=x*o.x+y*o.y+z*o.z
 fun cross(o:SkyPoint)=SkyPoint(y*o.z-z*o.y,z*o.x-x*o.z,x*o.y-y*o.x)
 fun normalized():SkyPoint {val n=sqrt(dot(this));require(n>0 && n.isFinite());return this*(1/n)}
}
data class SkyVertex(val position:SkyPoint,val u:Float,val v:Float)
data class SkyFrames(val a:Int,val b:Int,val blend:Float)
object SkyboxLogic {
 const val TILT=.42f
 val styles=listOf("decorated_end","end","dimension")
 val labels=listOf("Decorated End","Vanilla End","Dimension Default")
 /** Respect naturally skyless dimensions by default; never change the actual dimension. */
 fun forceEnd(enabled:Boolean,s:SkyboxSettings,nativeSkyless:Boolean)=enabled && s.style!="dimension" && (!nativeSkyless || s.skylessDimensions)
 fun decorated(enabled:Boolean,s:SkyboxSettings)=enabled && s.style=="decorated_end"
 fun tint(from:Int,s:SkyboxSettings):Int {
  if(!s.tint || s.strength==0.0) return from
  var out=from and 0xff000000.toInt()
  for(shift in listOf(16,8,0)) {val a=(from ushr shift) and 255;val b=(s.color ushr shift) and 255;out=out or ((a+(b-a)*s.strength).roundToInt().coerceIn(0,255) shl shift)}
  return out // Preserve sunrise transparency, including zero alpha (source changed zero alpha to opaque).
 }
 /** 8 frames, 5 ticks per step, ping-pong: no duplicate bright frame at the turnaround. */
 fun frames(time:Double,speed:Double):SkyFrames {
  require(time.isFinite() && speed.isFinite() && speed in 0.0..4.0)
  val cycle=((time/5.0*speed)%14+14)%14;val pos=if(cycle<=7) cycle else 14-cycle
  val a=floor(pos).toInt().coerceIn(0,7);return SkyFrames(a,min(a+1,7),(pos-a).toFloat().coerceIn(0f,1f))
 }
 fun rotation(time:Double,speed:Double):Float {require(time.isFinite() && speed.isFinite() && speed in 0.0..4.0);return ((time*.0009*speed)%(2*PI)).toFloat()}
 /** Six source cube faces followed by the source black-hole patch, UV row 0 at +up. */
 fun vertices():List<SkyVertex> = buildList {
  fun quad(n:SkyPoint,right:SkyPoint,up:SkyPoint,w:Double=1.0,h:Double=1.0) {
   for((x,y,uv) in listOf(Triple(-w,h,0f to 0f),Triple(w,h,1f to 0f),Triple(w,-h,1f to 1f),Triple(-w,-h,0f to 1f))) add(SkyVertex((n+right*x+up*y)*100.0,uv.first,uv.second))
  }
  for((normal,up) in listOf(SkyPoint(1.0,0.0,0.0) to SkyPoint(0.0,1.0,0.0),SkyPoint(-1.0,0.0,0.0) to SkyPoint(0.0,1.0,0.0),SkyPoint(0.0,1.0,0.0) to SkyPoint(0.0,0.0,-1.0),SkyPoint(0.0,-1.0,0.0) to SkyPoint(0.0,0.0,1.0),SkyPoint(0.0,0.0,1.0) to SkyPoint(0.0,1.0,0.0),SkyPoint(0.0,0.0,-1.0) to SkyPoint(0.0,1.0,0.0))) quad(normal,normal.cross(up).normalized(),up)
  val world=SkyPoint(.18,.86,.48).normalized();val c=cos(-TILT.toDouble());val s=sin(-TILT.toDouble())
  val hole=SkyPoint(world.x,world.y*c-world.z*s,world.y*s+world.z*c).normalized()
  var up=SkyPoint(0.0,1.0,0.0);if(abs(up.dot(hole))>.94) up=SkyPoint(0.0,0.0,1.0)
  up=(up+hole*(-up.dot(hole))).normalized();val right=up.cross(hole).normalized();val actualUp=hole.cross(right);val tanCone=sqrt(1-.574*.574)/.574
  quad(hole,right,actualUp,tanCone,tanCone)
 }
}
