/* SPDX-License-Identifier: GPL-3.0-only; server-snapshot policy adapted from impl.zip PetDisplay. */
package dev.solyanka.utility
import com.google.gson.JsonParser

data class PetSettings(val hud:HudSpec=HudSpec(x=18,y=82),val style:Int=1,val accent:Int=0xffffbf55.toInt(),val background:Int=0xc0181a22.toInt(),val sampleMinutes:Int=10,val highlight:Boolean=true,val highlightColor:Int=0x7055ffaa,val autoTitle:Boolean=false,val titleDungeonOnly:Boolean=true) {fun validate(){hud.validate();require(style in 0..2 && sampleMinutes in 1..30)}}
object PetLogic {
 data class Reading(val name:String,val level:Int?,val type:String?,val rarity:String?,val uuid:String?,val xp:Double?,val active:Boolean)
 fun read(header:String,lore:List<String>,json:String?):Reading? {
  val clean=ChatLogic.plain(header);val m=Regex("^\\[Lvl (\\d+)] (.+)$").matchEntire(clean) ?: return null
  val level=m.groupValues[1].toIntOrNull()?.takeIf {it in 1..1000}
  val j=if(json!=null && json.length<=32768)runCatching {JsonParser.parseString(json).asJsonObject}.getOrNull() else null
  fun str(k:String)=runCatching {j?.get(k)?.takeIf {!it.isJsonNull}?.asString?.takeIf {it.length<=256 && it.isNotBlank()}}.getOrNull()
  val xp=runCatching {j?.get("exp")?.asDouble?.takeIf {it.isFinite() && it>=0}}.getOrNull()
  val active=runCatching {j?.get("active")?.asBoolean==true}.getOrDefault(false) || lore.any {ChatLogic.plain(it).equals("Click to despawn!",true)}
  return Reading(m.groupValues[2],level,str("type"),str("tier"),str("uuid"),xp,active)
 }
}
class PetHistory {
 private val points=java.util.ArrayDeque<Pair<Long,Double>>()
 fun clear()=points.clear()
 fun record(time:Long,xp:Double,minutes:Int) {
  if(!xp.isFinite() || xp<0)return
  if(points.lastOrNull()?.let {time<it.first || xp<it.second}==true)clear()
  if(points.lastOrNull()?.second==xp)return
  points.add(time to xp)
  while(points.size>1 && (points.first.first<time-minutes*60000L || points.size>512))points.removeFirst()
 }
 fun hourly(time:Long=points.lastOrNull()?.first ?: 0L,minutes:Int=30):Double? {
  while(points.isNotEmpty() && points.first.first<time-minutes*60000L)points.removeFirst()
  if(points.size<2)return null
  val a=points.first;val b=points.last;val delta=b.first-a.first
  return if(delta<1000)null else ((b.second-a.second)*3600000.0/delta).takeIf {it.isFinite() && it>=0}
 }
}
