/* SPDX-License-Identifier: GPL-3.0-only
 * Odin MapScan, DoorHighlight, DungeonListener and DungeonUtils adaptation.
 * (c)2025 odtheking, BSD-3-Clause; see licenses/Odin-BSD-3-Clause.txt. */
package dev.solyanka.utility
import kotlin.math.*

data class MapDoor(val x:Int,val z:Int,val blood:Boolean)
object DungeonMapLogic {
 fun doors(colors:ByteArray):List<MapDoor>? {
  if(colors.size!=128*128 || colors[0].toInt()!=0) return null
  var layout:Triple<Int,Int,Int>?=null
  for(index in colors.indices) {
   if(colors[index].toInt()!=30) continue
   var end=index;while(end<colors.size && colors[end].toInt()==30) end++
   val length=end-index
   if(length!=16 && length!=18) continue
   val gap=length+4;layout=Triple(length,(index%128%gap).let {if(it==0)22 else it},(index/128%gap).let {if(it==0)22 else it});break
  }
  val (size,sx,sz)=layout ?: return null;val gap=size+4;val connection=size+2
  fun px(x:Int,z:Int)=if(x in 0..127 && z in 0..127) colors[z*128+x].toInt() and 255 else 0
  val result=mutableListOf<MapDoor>()
  for(z in 0..5) for(x in 0..5) {
   val ox=sx+x*gap;val oz=sz+z*gap
   if(x<5) {val c=px(ox+connection,oz+size/2);if(px(ox+connection,oz+size/2-4)==0 && c in setOf(119,18)) result.add(MapDoor(x*32-169,z*32-185,c==18))}
   if(z<5) {val c=px(ox+size/2,oz+connection);if(px(ox+size/2-4,oz+connection)==0 && c in setOf(119,18)) result.add(MapDoor(x*32-185,z*32-169,c==18))}
  }
  return result
 }
}
class DoorKeys {
 var wither=0;private set
 var blood=false;private set
 var bloodOpened=false;private set
 private val witherGet=Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Wither Key!?$")
 private val witherOpen=Regex("^(\\[[^]]*?])? ?(\\w{1,16}) opened a WITHER door!$")
 private val bloodGet=Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Blood Key!$")
 fun message(raw:String) {val s=ChatLogic.plain(raw);when {
  witherGet.matches(s) || s=="A Wither Key was picked up!" -> wither++
  witherOpen.matches(s) -> wither=(wither-1).coerceAtLeast(0)
  bloodGet.matches(s) || s=="A Blood Key was picked up!" -> blood=true
  s=="The BLOOD DOOR has been opened!" -> {blood=false;bloodOpened=true}
 } }
 fun reset() {wither=0;blood=false;bloodOpened=false}
}
data class MapSettings(val x:Int=12,val y:Int=230,val disableInBoss:Boolean=true,val alert300:Boolean=true,val highlightPortal:Boolean=true,val paul:Boolean=false,val remaining:Boolean=true,val roomSecrets:Boolean=true,val announceKeySpawn:Boolean=true) {
 fun validate() {require(x in 0..10000 && y in 0..10000)}
}
data class DungeonScore(val value:Int,val totalSecrets:Int?,val neededSecrets:Int?,val totalRooms:Int?,val completeInputs:Boolean)
class MapStats {
 var secrets:Int?=null;private set
 var percent:Double?=null;private set
 var completed:Int?=null;private set
 var cleared:Int?=null;private set
 var deaths:Int?=null;private set
 var crypts:Int?=null;private set
 var puzzleCount:Int?=null;private set
 var elapsed="?";private set
 var bloodDone=false;private set
 private var expectingBlood=false
 private val puzzleStates=mutableMapOf<String,Boolean>()
 private val number=Regex("^(Secrets Found|Completed Rooms|Team Deaths|Crypts|Puzzles): \\(?([0-9]+)\\)?$")
 private val secretPct=Regex("^Secrets Found: ([\\d.]+)%$")
 private val clearPct=Regex("^Cleared: (\\d+)% \\(\\d+\\)$")
 private val puzzle=Regex("^([\\w ?]+): \\[([✖✔✦])] ?(?:\\(\\w+\\))?$")
 fun update(tab:List<String>,sidebar:List<String>) {
  for(raw in tab+sidebar) {val s=ChatLogic.plain(raw).trim()
   number.matchEntire(s)?.let {m->val n=m.groupValues[2].toIntOrNull() ?: return@let;when(m.groupValues[1]) {
    "Secrets Found"->secrets=n;"Completed Rooms"->completed=n;"Team Deaths"->deaths=n;"Crypts"->crypts=n;"Puzzles"->puzzleCount=n
   }}
   secretPct.matchEntire(s)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf {it.isFinite() && it in 0.0..100.0}?.let {percent=it}
   clearPct.matchEntire(s)?.groupValues?.get(1)?.toIntOrNull()?.takeIf {it in 0..100}?.let {if(cleared!=it && expectingBlood) bloodDone=true;cleared=it}
   puzzle.matchEntire(s)?.let {puzzleStates[it.groupValues[1]]=it.groupValues[2]=="✔"}
   if(s.startsWith("Time: ")) elapsed=s.removePrefix("Time: ")
  }
 }
 fun message(raw:String) {if(ChatLogic.plain(raw)=="[BOSS] The Watcher: You have proven yourself. You may pass.") expectingBlood=true}
 fun score(floorNumber:Int?,master:Boolean,boss:Boolean,bonus:Int):DungeonScore {
  val totalSecrets=if((secrets ?: 0)>0 && (percent ?: 0.0)>0.0) kotlin.math.floor(100.0/percent!!*secrets!!+.5).toInt() else null
  val totalRooms=if((completed ?: 0)>0 && (cleared ?: 0)>0) kotlin.math.floor((completed!!/(cleared!!*.01).toFloat())+.4).toInt() else null
  val required=if(master)1.0 else when(floorNumber) {1->.3;2->.4;3->.5;4->.6;5->.7;6->.85;else->1.0}
  val count=(completed ?: 0)+(if(!bloodDone)1 else 0)+(if(!boss)1 else 0);val total=totalRooms ?: 36
  val secretScore=if(totalSecrets!=null) floor((secrets ?: 0)/(totalSecrets*required)*40).toInt().coerceIn(0,40) else 0
  val exploration=if(floorNumber!=null) secretScore+floor(count.toFloat()/total*60f).toInt().coerceIn(0,60) else 0
  val deathPenalty=((deaths ?: 0)*2-1).coerceAtLeast(0)
  val penalty=((puzzleCount ?: 0)-puzzleStates.values.count {it}).coerceAtLeast(0)*10
  val skill=(20+floor(count.toFloat()/total*80f).toInt().coerceIn(0,80)-penalty-deathPenalty).coerceIn(20,100)
  val need=totalSecrets?.let {ceil(it*required*(40-bonus+deathPenalty)/40.0).toInt().coerceAtLeast(0)}
  return DungeonScore(exploration+skill+bonus+100,totalSecrets,need,totalRooms,totalRooms!=null && totalSecrets!=null && deaths!=null && crypts!=null && puzzleCount!=null && floorNumber!=null)
 }
 fun reset() {secrets=null;percent=null;completed=null;cleared=null;deaths=null;crypts=null;puzzleCount=null;elapsed="?";bloodDone=false;expectingBlood=false;puzzleStates.clear()}
}
