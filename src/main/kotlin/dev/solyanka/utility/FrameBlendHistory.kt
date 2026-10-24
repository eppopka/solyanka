/* SPDX-License-Identifier: GPL-3.0-only
 * Stray FrameBlendingManager CC0 weighted exposure/compaction adaptation, pinned in audit. */
package dev.solyanka.utility
import kotlin.math.*

data class WeightedFrame(val index:Int,val weight:Float)
/** Clock-only ring; GPU storage uses the returned write slot. Never blend previous *output*. */
class FrameBlendHistory(val capacity:Int) {
 init {require(capacity in 1..24)}
 private val times=DoubleArray(capacity)
 private var writeIndex=0
 private var filled=0
 private var fps=0.0
 private var last=Double.NaN
 fun reset() {times.fill(0.0);writeIndex=0;filled=0;fps=0.0;last=Double.NaN}
 fun push(now:Double):Int {
  require(now.isFinite() && now>=0)
  if(last.isFinite()) {
   val elapsed=now-last
   if(elapsed<=0.0 || elapsed>.5) reset()
   else {val current=1.0/elapsed;fps=if(fps<=0) current else fps*.85+current*.15}
  }
  val index=writeIndex;times[index]=now;writeIndex=(writeIndex+1)%capacity;filled=min(capacity,filled+1);last=now
  return index
 }
 fun samples(now:Double,refreshRate:Int,strength:Double):List<WeightedFrame> {
  require(now.isFinite() && strength.isFinite())
  if(filled==0 || refreshRate<=0 || strength<=0) return emptyList()
  val reference=if(fps>0) min(fps,refreshRate.toDouble()) else refreshRate.toDouble()
  val start=now-strength.coerceAtMost(2.0)/reference
  val frameTime=if(fps>0) 1.0/fps else 1.0/refreshRate
  val first=(writeIndex-filled+capacity)%capacity
  val weighted=ArrayList<WeightedFrame>(filled)
  for(i in 0 until filled) {
   val index=(first+i)%capacity;val end=times[index]
   val previous=if(i>0) times[(first+i-1)%capacity] else end-frameTime
   val begin=if(previous<end) previous else end-frameTime
   val overlap=min(end,now)-max(begin,start)
   if(overlap>1e-7) weighted.add(WeightedFrame(index,overlap.toFloat()))
  }
  // Preserve Stray's 12-sampler compaction and total exposure weight.
  if(weighted.size<=12) return weighted
  val indices=IntArray(12) {out->weighted[(out*(weighted.size-1)/11.0).roundToInt()].index}
  val weights=FloatArray(12)
  weighted.forEachIndexed {source,value->weights[(source*11.0/(weighted.size-1)).roundToInt().coerceIn(0,11)]+=value.weight}
  return List(12) {WeightedFrame(indices[it],weights[it])}
 }
 companion object {
  const val BYTE_BUDGET=96L*1024*1024
  /** Includes output texture; history is lazy and depthless, unlike source's 24 eager targets. */
  fun capacity(width:Int,height:Int,bytesPerPixel:Int):Int {
   if(!BlurPolicy.sizeAllowed(width,height) || bytesPerPixel !in listOf(4,8)) return 0
   return (BYTE_BUDGET/(width.toLong()*height*bytesPerPixel)-1).coerceIn(0,24).toInt()
  }
 }
}
