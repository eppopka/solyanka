/* SPDX-License-Identifier: GPL-3.0-only; impl.zip numbered waypoint route adaptation. */
package dev.solyanka.utility

data class RouteSettings(val lines:Boolean=false,val color:Int=0xc8ffffff.toInt(),val width:Double=2.0,val rainbow:Boolean=false,val throughWalls:Boolean=true,val skipCollected:Boolean=true,val follow:Boolean=true,val radius:Double=4.0,val ahead:Int=1,val progress:Boolean=true,val hud:HudSpec=HudSpec(x=12,y=295)) {
 fun validate() {require(width.isFinite() && width in 1.0..6.0 && radius.isFinite() && radius in 1.0..15.0 && ahead in 1..20);hud.validate()}
}
object RouteLogic {
 data class Entry(val waypoint:Int,val number:Int)
 fun numbers(title:String):List<Int> {
  val prefix=Regex("^\\s*(\\d+(?:\\s*/\\s*\\d+)*)").find(title)?.groupValues?.get(1) ?: return emptyList()
  return prefix.split('/').mapNotNull {it.trim().toIntOrNull()?.takeIf {n->n in 1..10000}}.distinct().take(32)
 }
 fun entries(ways:List<Waypoint>)=ways.flatMapIndexed {index,w->
  if(w.title.isBlank() && w.color==0xffff5555.toInt())listOf(Entry(index,0)) else numbers(w.title).map {Entry(index,it)}
 }.sortedWith(compareBy<Entry> {it.number}.thenBy {it.waypoint})
 /** Progress cannot jump to a later repeat of the same physical block. */
 fun advance(entries:List<Entry>,cursor:Int,near:Set<Int>):Int {
  if(entries.isEmpty())return -1
  val safe=cursor.coerceIn(-1,entries.lastIndex)
  if(safe<0)return entries.indices.firstOrNull {entries[it].waypoint in near} ?: -1
  if(safe<entries.lastIndex && entries[safe+1].waypoint in near && entries[safe+1].waypoint!=entries[safe].waypoint)return safe+1
  if(entries[safe].waypoint !in near && entries.first().number==0 && entries.first().waypoint in near)return 0
  return safe
 }
 fun segments(entries:List<Entry>,cursor:Int,s:RouteSettings):List<Pair<Entry,Entry>> {
  val pairs=entries.zipWithNext()
  if(!s.follow)return pairs
  val first=cursor.coerceAtLeast(0)
  return pairs.drop(first).take(s.ahead)
 }
}
