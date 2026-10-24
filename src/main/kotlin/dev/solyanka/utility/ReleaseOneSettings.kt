/* SPDX-License-Identifier: GPL-3.0-only
 * Stray CC0 accumulation formulas and user-supplied eppopkaaddons Timer/DVD logic adapters. */
package dev.solyanka.utility
import kotlin.math.*
import java.util.Locale

data class MotionBlurSettings(val mode:String="frame_blend",val strength:Double=1.0,val pauseInMenus:Boolean=true) {
 fun validate() {require(mode in BlurPolicy.modes);require(strength.isFinite() && strength in 0.0..2.0)}
}
object BlurPolicy {
 val modes=listOf("frame_blend","accum_mix","accum_max");val labels=listOf("Frame Blending (Stray)","Accumulation Mix","Accumulation Max")
 const val MAX_PIXELS=8_388_608L
 fun factor(s:MotionBlurSettings):Float {s.validate();return (1-.5.pow(s.strength*(if(s.mode=="accum_max") 7 else 5)/3)).toFloat()}
 fun sizeAllowed(w:Int,h:Int)=w>0 && h>0 && w.toLong()*h<=MAX_PIXELS
 fun channel(current:Double,previous:Double,s:MotionBlurSettings):Double {val f=factor(s).toDouble();return if(s.mode=="accum_max") max(current,previous*f) else sqrt(current*current*(1-f)+previous*previous*f)}
}
data class StopwatchSettings(val device:String="KEYBOARD",val key:Int=-1,val hud:HudSpec=HudSpec(x=20,y=20)) {
 fun validate() {require(device in setOf("KEYBOARD","MOUSE"));require(key == -1 || if(device=="MOUSE") key in 0..7 else key in 32..348);hud.validate()}
 fun matches(d:InputDevice,k:Int)=key>=0 && device==d.name && key==k
}
/** TimerClock from the user archive: monotonic, three presses, zero-time stop still reports. */
class StopwatchClock(private val now:()->Long=System::nanoTime) {
 enum class State { IDLE,RUNNING,STOPPED }
 var state=State.IDLE;private set
 private var startedAt=0L;private var stoppedElapsed=0L
 fun press():Long? = when(state) {
  State.IDLE->{startedAt=now();stoppedElapsed=0;state=State.RUNNING;null}
  State.RUNNING->{stoppedElapsed=(now()-startedAt).coerceAtLeast(0);state=State.STOPPED;stoppedElapsed}
  State.STOPPED->{reset();null}
 }
 fun elapsedNanos()=when(state) {State.IDLE->0L;State.RUNNING->(now()-startedAt).coerceAtLeast(0);State.STOPPED->stoppedElapsed}
 fun reset() {state=State.IDLE;startedAt=0;stoppedElapsed=0}
 companion object {
  fun formatNanos(nanos:Long):String {val m=nanos.coerceAtLeast(0)/1_000_000;return String.format(Locale.ROOT,"%02d:%02d:%02d.%03d",m/3_600_000,m/60_000%60,m/1_000%60,m%1_000)}
 }
}
data class DvdSettings(val text:String="SOLYANKA",val width:Int=92,val height:Int=38,val radius:Int=9,val speed:Double=92.0,val counter:Boolean=true,val sound:Boolean=true,val flash:Boolean=true) {
 fun validate() {require(text.length<=64 && text.none {it.code<32 || it.code==127});require(width in 48..240 && height in 24..120 && radius in 0..40);require(speed.isFinite() && speed in 20.0..320.0)}
}
class DvdMotion {
 var x=24.0;private set;var y=24.0;private set
 private var dx=1.0;private var dy=.78
 var colorIndex=0;private set;var cornerHits=0;private set
 var flashUntil=0L;private set;private var lastFrame:Long?=null
 fun pause(now:Long) {lastFrame=now}
 fun reset() {x=24.0;y=24.0;dx=1.0;dy=.78;colorIndex=0;cornerHits=0;flashUntil=0;lastFrame=null}
 /** Source frame-clamped movement. Degenerate/tiny axes never manufacture corner hits. */
 fun step(now:Long,screenW:Int,screenH:Int,s:DvdSettings):Boolean {
  val old=lastFrame;lastFrame=now;val dt=if(old==null) 0.0 else ((now-old)/1e9).coerceIn(0.0,.05)
  val maxX=(screenW-s.width).coerceAtLeast(0).toDouble();val maxY=(screenH-s.height).coerceAtLeast(0).toDouble()
  x=x.coerceIn(0.0,maxX);y=y.coerceIn(0.0,maxY);x+=dx*s.speed*dt;y+=dy*s.speed*dt
  var bx=false;var by=false
  if(maxX==0.0) x=0.0 else if(dt>0 && (x<=0 || x>=maxX)) {x=x.coerceIn(0.0,maxX);dx=if(x<=0) 1.0 else -1.0;bx=true}
  if(maxY==0.0) y=0.0 else if(dt>0 && (y<=0 || y>=maxY)) {y=y.coerceIn(0.0,maxY);dy=if(y<=0) 1.0 else -1.0;by=true}
  if(bx||by) colorIndex=(colorIndex+1)%6
  if(bx&&by) {cornerHits=(cornerHits.toLong()+1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt();flashUntil=now+420_000_000;return true}
  return false
 }
 fun flashAlpha(now:Long)=if(cornerHits==0) 0 else (((flashUntil-now)/420_000_000.0).coerceIn(0.0,1.0)*42).toInt()
}
