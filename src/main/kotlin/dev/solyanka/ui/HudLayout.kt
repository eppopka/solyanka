/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import dev.solyanka.utility.*
import kotlin.math.roundToInt

data class HudEntry(val key:String,val name:String,val hud:HudSpec,val lines:List<String>,val resizable:Boolean=true,val baseWidth:Int?=null,val baseHeight:Int?=null,val paddingX:Int=4)
data class HudBounds(val x:Int,val y:Int,val width:Int,val height:Int) {
    fun contains(mx:Double,my:Double)=mx>=x && my>=y && mx<x+width && my<y+height
}
/** The editor uses the same stored positions and scales as live renderers; sample data is editor-only. */
object HudLayout {
    fun entries(f:FeatureSettings):List<HudEntry> = buildList {
        val names=mapOf("goldor" to "Goldor","pad" to "Storm Pad","lightning" to "Storm Lightning","py" to "Storm PY","storm" to "Storm Phase","freeze" to "M3 Fire Freeze")
        f.timers.huds().forEach { (id,h) ->add(HudEntry("timer.$id",names.getValue(id),h,listOf("${names.getValue(id)}: 1.75s"))) }
        val samples=mapOf("health" to "3000/4000 HP","mana" to "2000/20000 Mana","overflow" to "333 Overflow","defense" to "1000 Defense","vitality" to "100 Vitality","ehp" to "33000 EHP","speed" to "400 Speed")
        f.player.huds().forEach { (id,h) -> add(HudEntry("player.$id",id.replaceFirstChar {it.uppercase()},h,listOf(samples.getValue(id)))) }
        val inv=InventoryHudLayout.metrics(f.inventoryHud)
        add(HudEntry("extra.inventory","Inventory HUD",f.inventoryHud.hud,listOf("Inventory sample"),baseWidth=inv.width,baseHeight=inv.height))
        add(HudEntry("extra.item_name","Item Name HUD",f.itemNameHud.hud,listOf("Sample held item"),baseHeight=18,paddingX=16))
        val skill=(if(f.skillHud.showGain) "+9.2 " else "")+"Farming"+(if(f.skillHud.showLevel) " 14" else "")+" 12,345/20,000"
        add(HudEntry("extra.skill","Skill Progress HUD",f.skillHud.hud,listOf(skill),baseHeight=26,paddingX=10+if(f.skillHud.icon) 20 else 0))
        add(HudEntry("extra.scoreboard","Scoreboard",f.scoreboardHud.hud,listOf("Scoreboard sample"),baseWidth=f.scoreboardHud.maxWidth,baseHeight=ScoreboardLayout.height(5,f.scoreboardHud.header)))
        add(HudEntry("map","Map Info",HudSpec(x=f.map.x,y=f.map.y),listOf("Secrets: 25/30 | Score~: 295","Deaths: 0 | Crypts: 5","Sample room | 05:00"),false))
    }
    fun update(f:FeatureSettings,key:String,change:(HudSpec)->HudSpec):FeatureSettings {
        val entry=entries(f).firstOrNull {it.key==key} ?: error("Unknown HUD: $key")
        val h=change(entry.hud);h.validate()
        return when {
            key.startsWith("timer.")->f.copy(timers=f.timers.withHud(key.substringAfter('.'),h))
            key.startsWith("player.")->f.copy(player=f.player.withHud(key.substringAfter('.'),h))
            key=="extra.inventory"->f.copy(inventoryHud=f.inventoryHud.copy(hud=h))
            key=="extra.item_name"->f.copy(itemNameHud=f.itemNameHud.copy(hud=h))
            key=="extra.scoreboard"->f.copy(scoreboardHud=f.scoreboardHud.copy(hud=h))
            key=="extra.skill"->f.copy(skillHud=f.skillHud.copy(hud=h))
            key=="map"->f.copy(map=f.map.copy(x=h.x,y=h.y))
            else->error("Unknown HUD: $key")
        }
    }
    fun bounds(entry:HudEntry,textWidth:Int,width:Int,height:Int)=pixels(entry.hud,entry.baseWidth ?: textWidth+entry.paddingX,entry.baseHeight ?: (entry.lines.size*10+2),width,height)
    fun bounds(h:HudSpec,textWidth:Int,lineCount:Int,width:Int,height:Int)=pixels(h,textWidth+4,lineCount*10+2,width,height)
    private fun pixels(h:HudSpec,baseWidth:Int,baseHeight:Int,width:Int,height:Int):HudBounds {
        val w=(baseWidth*h.scale).roundToInt().coerceIn(1,width.coerceAtLeast(1))
        val tall=(baseHeight*h.scale).roundToInt().coerceIn(1,height.coerceAtLeast(1))
        return HudBounds(h.x.coerceIn(0,(width-w).coerceAtLeast(0)),h.y.coerceIn(0,(height-tall).coerceAtLeast(0)),w,tall)
    }
    fun drag(h:HudSpec,mx:Double,my:Double,offsetX:Double,offsetY:Double,bounds:HudBounds,width:Int,height:Int):HudSpec = h.copy(
        x=(mx-offsetX).roundToInt().coerceIn(0,minOf(10000,(width-bounds.width).coerceAtLeast(0))),
        y=(my-offsetY).roundToInt().coerceIn(0,minOf(10000,(height-bounds.height).coerceAtLeast(0))))
}
