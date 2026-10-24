/* SPDX-License-Identifier: GPL-3.0-only
 * Stray InventoryHudRenderer/HeldItemHudRenderer/SkillProgressHudRenderer CC0 adaptations. */
package dev.solyanka.utility

data class InventoryHudSettings(val hud:HudSpec=HudSpec(x=12,y=200,color=0xff2fb5ff.toInt()),val armor:Boolean=true,val hotbar:Boolean=true,val count:Boolean=true,val minimal:Boolean=false,val background:Boolean=true,val backgroundColor:Int=0xff0b0e14.toInt())
data class ItemNameHudSettings(val hud:HudSpec=HudSpec(x=220,y=160),val background:Boolean=true,val backgroundColor:Int=0xaa101010.toInt(),val hideVanilla:Boolean=true)
data class SkillHudSettings(val hud:HudSpec=HudSpec(x=220,y=180),val icon:Boolean=true,val showGain:Boolean=true,val showLevel:Boolean=true,val background:Boolean=true,val backgroundColor:Int=0xaa101010.toInt(),val timeoutSeconds:Int=0)
data class InventoryHudMetrics(val width:Int,val height:Int,val pad:Int,val armorY:Int,val mainY:Int,val hotbarY:Int)
object InventoryHudLayout {
    fun metrics(s:InventoryHudSettings):InventoryHudMetrics {
        val pad=if(s.minimal) 2 else 7
        var y=pad
        val armorY=y
        if(s.armor) y+=23
        val mainY=y;y+=58
        var hotbarY=y
        if(s.hotbar) {y+=23;hotbarY=y-18}
        if(s.count && !s.minimal && !s.armor) y+=11
        return InventoryHudMetrics(pad*2+178,y+pad,pad,armorY,mainY,hotbarY)
    }
    private val main=(9..35).toList()
    private val hotbar=(0..8).toList()
    fun mainSlots()=main
    fun hotbarSlots()=hotbar
}

object PlayerHudVisibility {
    fun xpBar(moduleEnabled:Boolean,skyblock:Boolean,settings:PlayerDisplaySettings)=moduleEnabled && skyblock && settings.hideXpBar
}
