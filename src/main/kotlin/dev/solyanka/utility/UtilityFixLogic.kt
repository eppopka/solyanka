/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills MiddleClickOverride/NoCursorReset, WhatYouThing and contributors. */
package dev.solyanka.utility

object MiddleClickPolicy {
    private val exactBlacklist=setOf("Attribute Fusion","Beacon","Chest","Large Chest","Anvil","Storage","Drill Anvil","Runic Pedestal","Rune Removal","Reforge Anvil","Reforge Item","Offer Pets","Exp Sharing","Convert to Dungeon Item","Upgrade Item","Salvage Items","Fishing Rod Parts","Stats Tuning","Pet Sitter","Transfer to Profile","Kuudra Armor Type Transfer","Composter","Midas Anvil","Hunting Box")
    private val exactWhitelist=setOf("Stats & Equipment","Accessory Bag Thaumaturgy","Pets","Community Shop")
    private val containBlacklist=setOf(" Minion ","Abiphone","AⒷiphone","The Hex","Enchant Item","Auction","Cosmetic","Trap","Gemstones","Heart of the ","Widgets")
    private val pagination=Regex("""^\(\d*/\d*\) """)
    fun override(title:String,button:Int,pickup:Boolean,skyblock:Boolean,empty:Boolean,skyblockId:String,lore:List<String>,loadoutEdit:Boolean):Boolean {
        if(button!=0 || !pickup || !skyblock || empty || loadoutEdit) return false
        val base=title.replace(pagination,"")
        if(base in exactBlacklist || containBlacklist.any(title::contains)) return false
        return skyblockId.isEmpty() || base in exactWhitelist || title.contains("Bits Shop") || lore.any { it=="Cost" || it=="Sell Price" || it=="Bazaar Price" }
    }
}
class CursorMemory {
    var x=-1.0;private set
    var y=-1.0;private set
    var ticks=0;private set
    fun capture(x:Double,y:Double) { if(x.isFinite() && y.isFinite() && x>=0 && y>=0) { this.x=x;this.y=y } }
    fun screenChanged(clearTicks:Int) { ticks=clearTicks }
    fun tick(inWorld:Boolean,screenOpen:Boolean) { if(inWorld && !screenOpen && ticks>0 && --ticks==0) reset() }
    fun position(width:Int,height:Int):Pair<Double,Double>? = if(x<0 || y<0 || width<=0 || height<=0) null else x.coerceIn(0.0,width-1.0) to y.coerceIn(0.0,height-1.0)
    fun reset() { x=-1.0;y=-1.0;ticks=0 }
}
object LoadingPolicy {
    fun hide(enabled:Boolean,serverOnly:Boolean,serverPresent:Boolean,lan:Boolean)=enabled && (!serverOnly || (serverPresent && !lan))
}
