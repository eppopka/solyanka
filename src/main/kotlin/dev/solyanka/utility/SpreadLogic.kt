/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

data class SpreadGuesserSettings(val color:Int=0xffffcc33.toInt(),val filled:Boolean=false,val fillColor:Int=0x40ffcc33)

object SpreadLogic {
    private val miningGroup=setOf("minecraft:cyan_terracotta","minecraft:gray_wool","minecraft:light_blue_wool", "minecraft:prismarine","minecraft:prismarine_bricks","minecraft:dark_prismarine")
    fun equivalent(first:String,second:String)=first==second || first in miningGroup && second in miningGroup
    private val stat = Regex("(?i)\\bmining\\s+spread\\s*[:：]?[\\s\\p{Z}\\p{S}\\p{Co}\\p{M}\\p{Cf}]*([0-9][0-9,]*(?:\\.[0-9]+)?)\\b")
    fun readTab(lines:Iterable<String>):Double? {
        val values=lines.mapNotNull { line ->
            val match=stat.find(ChatLogic.plain(line)) ?: return@mapNotNull null
            match.groupValues[1].replace(",","").toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10000.0 }
        }.distinct()
        return values.singleOrNull()
    }
}
