/* SPDX-License-Identifier: GPL-3.0-only; independently implemented local Party Finder blocklist. */
package dev.solyanka.utility
import com.google.gson.*
import java.nio.file.*

data class BlocklistSettings(val network:Boolean=false,val autoKick:Boolean=false,val partyWarning:Boolean=false,val delayMs:Int=1100) {fun validate(){require(delayMs in 100..3000)}}
data class BlockEntry(val uuid:String,val nickname:String,val reason:String)
object BlocklistCodec {
 fun uuid(value:String)=value.replace("-","").lowercase().takeIf {it.matches(Regex("[0-9a-f]{32}"))}
 fun name(value:String)=value.matches(Regex("[A-Za-z0-9_]{1,16}"))
 fun entry(e:BlockEntry):BlockEntry {
  val id=uuid(e.uuid) ?: throw IllegalArgumentException("Invalid UUID")
  require(name(e.nickname) || e.nickname=="Unknown");require(e.reason.length<=256 && e.reason.none {it.isISOControl()})
  return e.copy(uuid=id)
 }
 fun decode(text:String):List<BlockEntry> {
  require(text.length<=2*1024*1024);val a=JsonParser.parseString(text);require(a.isJsonArray && a.asJsonArray.size()<=5000)
  val rows=a.asJsonArray.map {j->val o=j.asJsonObject;entry(BlockEntry(o["uuid"].asString,o["nickname"]?.asString ?: "Unknown",o["reason"]?.asString ?: ""))}
  require(rows.map{it.uuid}.distinct().size==rows.size){"Duplicate UUIDs"};return rows
 }
 fun encode(rows:List<BlockEntry>)=GsonBuilder().setPrettyPrinting().create().toJson(rows.map(::entry))
 fun joiner(raw:String):String?=Regex("^Party Finder > ([A-Za-z0-9_]{1,16}) joined the dungeon group!(?: .*)?$").matchEntire(ChatLogic.plain(raw))?.groupValues?.get(1)
}
/** Refuses writes after corrupt loading; never replaces an unreadable list with empty data. */
class BlocklistStore(private val path:Path) {
 var rows=emptyList<BlockEntry>();private set
 var error:String?=null;private set
 fun load(){try{rows=if(Files.exists(path)){require(Files.size(path)<=2*1024*1024);BlocklistCodec.decode(Files.readString(path))}else emptyList();error=null}catch(e:Exception){rows=emptyList();error="Blocklist unreadable; writes disabled: ${e.message}"}}
 fun save(next:List<BlockEntry>){check(error==null){error!!};require(next.size<=5000);val text=BlocklistCodec.encode(next);BlocklistCodec.decode(text)
  Files.createDirectories(path.parent);val temp=Files.createTempFile(path.parent,"solyanka-list-",".tmp")
  try{Files.writeString(temp,text);Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);rows=next.map(BlocklistCodec::entry)}finally{Files.deleteIfExists(temp)}
 }
}
