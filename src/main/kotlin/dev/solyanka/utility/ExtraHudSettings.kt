/* SPDX-License-Identifier: GPL-3.0-only
 * Stray InventoryHudRenderer/HeldItemHudRenderer/SkillProgressHudRenderer CC0 adaptations. */
package dev.solyanka.utility

data class InventoryHudSettings(val hud:HudSpec=HudSpec(x=12,y=200),val armor:Boolean=true,val hotbar:Boolean=true,val count:Boolean=true,val minimal:Boolean=false,val background:Boolean=true,val backgroundColor:Int=0xaa101010.toInt())
data class ItemNameHudSettings(val hud:HudSpec=HudSpec(x=220,y=160),val background:Boolean=true,val backgroundColor:Int=0xaa101010.toInt(),val hideVanilla:Boolean=true)
data class SkillHudSettings(val hud:HudSpec=HudSpec(x=220,y=180),val icon:Boolean=true,val showGain:Boolean=true,val showLevel:Boolean=true,val background:Boolean=true,val backgroundColor:Int=0xaa101010.toInt(),val timeoutSeconds:Int=0)
data class InventoryHudMetrics(val width:Int,val height:Int,val pad:Int,val armorY:Int,val mainY:Int,val hotbarY:Int)
object InventoryHudLayout {
    fun metrics(s:InventoryHudSettings):InventoryHudMetrics {
        val pad=if(s.minimal) 2 else 7
        var y=pad+if(s.minimal) 0 else 14
        val armorY=y
        if(s.armor) y+=23
        val mainY=y;y+=58
        var hotbarY=y
        if(s.hotbar) {y+=23;hotbarY=y-18}
        return InventoryHudMetrics(pad*2+178,y+pad,pad,armorY,mainY,hotbarY)
    }
    fun mainSlots()=(9..35).toList()
    fun hotbarSlots()=(0..8).toList()
}

object PlayerHudVisibility {
    fun xpBar(moduleEnabled:Boolean,skyblock:Boolean,settings:PlayerDisplaySettings)=moduleEnabled && skyblock && settings.hideXpBar
}
