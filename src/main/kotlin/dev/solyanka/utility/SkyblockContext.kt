/* SPDX-License-Identifier: GPL-3.0-only
 * Sidebar SkyBlock check references NoFrills. Boss-coordinate predicate adapted
 * from Odin DungeonListener (c) 2025 odtheking, BSD-3-Clause; see bundled license. */
package dev.solyanka.utility

data class SkyblockContext(val skyblock:Boolean=false,val dungeon:Boolean=false,val floor:Int?=null,val master:Boolean=false,val boss:Boolean=false,val area:String="")
object ContextParser {
    private val floorRegex=Regex("""The Catacombs \(([FM])([1-7])\)$""")
    fun parse(title:String,sidebar:List<String>,tab:List<String>,x:Double,z:Double):SkyblockContext {
        val skyblock=ChatLogic.plain(title).contains("SKYBLOCK")
        if(!skyblock) return SkyblockContext()
        val clean=sidebar.map { ChatLogic.plain(it).trim() }
        val floor=clean.firstNotNullOfOrNull { floorRegex.find(it) }
        val area=tab.map { ChatLogic.plain(it).trim() }.firstOrNull { it.startsWith("Area: ") || it.startsWith("Dungeon: ") }?.substringAfter(": ") ?: ""
        val dungeon=floor!=null || area=="Catacombs" || area=="The Catacombs" || area=="Dungeon"
        val n=floor?.groupValues?.get(2)?.toIntOrNull()
        val boss=dungeon && when(n) { 1->x> -71 && z> -39;2,3,4->x> -39 && z> -39;5,6->x> -39 && z> -7;7->x> -7 && z> -7;else->false }
        return SkyblockContext(true,dungeon,n,floor?.groupValues?.get(1)=="M",boss,area)
    }
}
