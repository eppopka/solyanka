/* SPDX-License-Identifier: GPL-3.0-only
 * Stray MiningAreas/MiningTracker/TitaniumTracker CC0 adaptation. Marker coordinates
 * reference SkyHanni graph nodes in upstream comments; see NOTICE and docs/M6.md. */
package dev.solyanka.utility

import java.util.Locale

data class MiningCommission(val name:String,val fraction:Double) {val done get()=fraction>=.999}
enum class TitaniumRegion(val label:String,val token:String) {
    LAVA_SPRINGS("Lava Springs","lava springs"),CLIFFSIDE_VEINS("Cliffside Veins","cliffside"),RAMPARTS_QUARRY("Rampart's Quarry","rampart"),UPPER_MINES("Upper Mines","upper mines"),ROYAL_MINES("Royal Mines","royal mines"),FAR_RESERVE("Far Reserve","far reserve"),GOBLIN_BURROWS("Goblin Burrows","goblin burrow"),GREAT_ICE_WALL("Great Ice Wall","ice wall"),DWARVEN_VILLAGE("Dwarven Village","dwarven village"),THE_MIST("The Mist","mist"),FORGE_BASIN("Forge Basin","forge basin")
}
data class TitaniumMarker(val region:TitaniumRegion?,val x:Int,val y:Int,val z:Int)
data class TitaniumFilter(val active:Boolean=false,val all:Boolean=false,val regions:Set<TitaniumRegion> = emptySet()) {
    fun allows(region:TitaniumRegion?)=active && (all || region!=null && region in regions)
}
object TitaniumLogic {
    private fun clean(raw:String)=ChatLogic.plain(raw.take(512)).replace('\u00a0',' ').trim().replace(Regex("^[\\s•·▪▸►\\-–—★☆✔✅⏣]+"),"").replace(Regex("[:\\-–—]+$"),"").replace(Regex("\\s+")," ").trim()
    private val inline=Regex("^(.+?)(?:\\s*[:\\-–]\\s*|\\s+)(\\d+(?:\\.\\d+)?\\s*%|\\d+\\s*/\\s*\\d+|done|complete|✔)$",RegexOption.IGNORE_CASE)
    private val widget=Regex("^(players?\\b|info|area\\b|server\\b|gems\\b|profile\\b|sb level|bank\\b|skills\\b|stats\\b|event\\b|pet\\b|powders?\\b|crystals?\\b|pickaxe ability|unclaimed|forges?\\b|timers\\b|party\\b|slayer\\b|active effects|bestiary|essence|collection|fire sales|election|north stars|guests\\b|coop\\b|island\\b|minions?\\b|account info|dungeon stats|player stats|puzzles\\b|opened rooms).*",RegexOption.IGNORE_CASE)
    fun progress(raw:String):Double? {
        val text=raw.trim()
        if(text.lowercase(Locale.ROOT) in setOf("done","complete","✔")) return 1.0
        Regex("^(\\d+(?:\\.\\d+)?)\\s*%$").matchEntire(text)?.let {
            val n=it.groupValues[1].toDoubleOrNull() ?: return null
            return n.takeIf {it.isFinite() && it in 0.0..100.0}?.div(100.0)
        }
        Regex("^(\\d+)\\s*/\\s*(\\d+)$").matchEntire(text)?.let {
            val have=it.groupValues[1].toLongOrNull() ?: return null;val needed=it.groupValues[2].toLongOrNull() ?: return null
            return if(needed>0 && have in 0..needed) have.toDouble()/needed else null
        }
        return null
    }
    fun commissions(tab:List<String>):List<MiningCommission> = buildList {
        var section=false;var pending:String?=null
        for(raw in tab.take(80)) {
            val line=if(ChatLogic.plain(raw).trim()=="✔") "✔" else clean(raw);if(line.isEmpty()) continue
            if(line.equals("commissions",true) || line.equals("commission",true)) {section=true;pending=null;continue}
            if(!section) continue
            if(widget.matches(line)) break
            val only=progress(line)
            if(only!=null && pending!=null) {add(MiningCommission(pending,only));pending=null;continue}
            val match=inline.matchEntire(line)
            val value=match?.let {progress(it.groupValues[2])}
            if(match!=null && value!=null) {add(MiningCommission(clean(match.groupValues[1]),value));pending=null} else pending=line
            if(size>=16) break
        }
    }
    fun filter(commissions:List<MiningCommission>,showAll:Boolean=false):TitaniumFilter {
        if(showAll) return TitaniumFilter(true,true)
        val regions=linkedSetOf<TitaniumRegion>()
        for(c in commissions) {
            if(c.done) continue
            val name=c.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+")," ").trim()
            if(!name.contains("titanium")) continue
            if(name=="titanium" || name.contains("titanium miner")) return TitaniumFilter(true,true)
            TitaniumRegion.entries.filter {name.contains(it.token)}.maxByOrNull {it.token.length}?.let(regions::add)
        }
        return TitaniumFilter(regions.isNotEmpty(),false,regions)
    }
    fun region(markers:List<TitaniumMarker>,x:Int,y:Int,z:Int):TitaniumRegion? {
        var best:TitaniumMarker?=null;var distance=Long.MAX_VALUE
        for(m in markers) {
            val dx=x.toLong()-m.x;val dy=y.toLong()-m.y;val dz=z.toLong()-m.z
            val d=dx*dx+dy*dy+dz*dz
            if(d<distance) {best=m;distance=d}
        }
        return if(distance<=32L*32L) best?.region else null
    }
    fun miningArea(skyblock:Boolean,area:String):Boolean {
        if(!skyblock) return false
        val name=area.lowercase(Locale.ROOT)
        return listOf("dwarven","crystal hollow","glacite","mines of divan","magma field","precursor","goblin holdout","mithril deposit","khazad","forge","base camp","mineshaft","quarry","rampart","upper mines","royal mines","cliffside veins","lava springs","divan","deep cavern","far reserve","goblin burrow","great ice wall","the mist").any {name.contains(it)}
    }
    fun within(x:Int,y:Int,z:Int,cx:Int,cy:Int,cz:Int,radius:Int):Boolean {
        val dx=x.toLong()-cx;val dy=y.toLong()-cy;val dz=z.toLong()-cz
        return dx*dx+dy*dy+dz*dz<=radius.toLong()*radius
    }
    /** Chunk section coordinates in a bounded cube; client checks loaded status before reading. */
    fun sections(x:Int,y:Int,z:Int,radius:Int,minY:Int,maxY:Int):List<GridPos> {
        require(radius in 16..96)
        val low=maxOf(y-radius,minY);val high=minOf(y+radius,maxY)
        if(low>high) return emptyList()
        return buildList {for(cx in (x-radius shr 4)..(x+radius shr 4)) for(cz in (z-radius shr 4)..(z+radius shr 4)) for(sy in (low shr 4)..(high shr 4)) add(GridPos(cx,sy,cz))}.sortedBy {
            val dx=it.x*16+8L-x;val dy=it.y*16+8L-y;val dz=it.z*16+8L-z;dx*dx+dy*dy+dz*dz
        }
    }
}
