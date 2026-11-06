/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.util.UUID

/** Server-supplied nameplates retained only for the current world. */
class BestiaryTagCache(private val limit:Int=4096) {
 data class Target(val id:Int,val uuid:UUID)
 data class Entry(val id:Int,val position:Point3,var name:String?=null,var target:Target?=null)
 private val entries=linkedMapOf<Int,Entry>()
 fun spawn(id:Int,pos:Point3) {
  entries.remove(id)
  if(!listOf(pos.x,pos.y,pos.z).all(Double::isFinite) || limit<=0)return
  // shortcut: oldest of 4096 stands is evicted, raise the cap if real worlds exceed it.
  if(entries.size>=limit)entries.remove(entries.keys.first())
  entries[id]=Entry(id,pos)
 }
 fun name(id:Int,name:String?) {entries[id]?.let {entry->entry.name=name?.takeIf {it.length<=2048};if(entry.name==null)entry.target=null}}
 fun remove(id:Int) {entries.remove(id)}
 fun clear() {entries.clear()}
 fun get(id:Int)=entries[id]
 fun all()=entries.values.toList()
}
