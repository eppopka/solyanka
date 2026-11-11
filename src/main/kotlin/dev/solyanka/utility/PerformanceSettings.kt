/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

data class PerformanceSettings(val hud:HudSpec=HudSpec(x=12,y=12),val showFps:Boolean=true,val showPing:Boolean=true,val showTps:Boolean=true) {
 fun validate()=hud.validate()
}

/** Estimate server tick rate from elapsed monotonic time and world-game-time updates. */
class ServerTpsEstimator(private val maxSamples:Int=10,private val staleNanos:Long=5_000_000_000L) {
 private data class Sample(val ticks:Long,val nanos:Long)
 private val samples=ArrayDeque<Sample>()
 private var lastTime:Long?=null
 private var lastTicks:Long?=null
 fun reset() {samples.clear();lastTime=null;lastTicks=null}
 fun observe(ticks:Long,now:Long) {
  val previousTime=lastTime;val previousTicks=lastTicks
  lastTime=now;lastTicks=ticks
  if(previousTime==null || previousTicks==null) return
  val elapsed=now-previousTime;val advance=ticks-previousTicks
  if(elapsed<=0 || elapsed>30_000_000_000L || advance<0) {samples.clear();return}
  samples.addLast(Sample(advance,elapsed));while(samples.size>maxSamples.coerceAtLeast(1)) samples.removeFirst()
 }
 fun value(now:Long):Double? {
  val last=lastTime ?: return null
  if(now<last || now-last>staleNanos || samples.isEmpty()) return null
  val nanos=samples.sumOf {it.nanos};if(nanos<=0) return null
  return (samples.sumOf {it.ticks}.toDouble()*1_000_000_000.0/nanos).coerceIn(0.0,20.0)
 }
}

object PerformanceText {
 fun format(s:PerformanceSettings,fps:Int,ping:String,tps:Double?):String = buildList {
  if(s.showFps) add("FPS: $fps")
  if(s.showPing) add("Ping: $ping")
  if(s.showTps) add("TPS: "+(tps?.takeIf {it.isFinite()}?.coerceIn(0.0,20.0)?.let {String.format(java.util.Locale.ROOT,"%.1f",it)} ?: "—"))
 }.joinToString(" | ")
}
