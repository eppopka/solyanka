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
    fun entries(f:FeatureSettings,width:Int=960,height:Int=540):List<HudEntry> = buildList {
        val names=mapOf("goldor" to "Goldor","pad" to "Storm Pad","lightning" to "Storm Lightning","py" to "Storm PY","storm" to "Storm Phase","freeze" to "M3 Fire Freeze")
        f.timers.huds().forEach { (id,h) ->add(HudEntry("timer.$id",names.getValue(id),h,listOf("${names.getValue(id)}: 1.75s"))) }
        val samples=mapOf("health" to "3000/4000 HP","mana" to "2000/20000 Mana","overflow" to "333 Overflow","defense" to "1000 Defense","vitality" to "100 Vitality","ehp" to "33000 EHP","speed" to "400 Speed")
        f.player.huds().forEach { (id,h) -> add(HudEntry("player.$id",id.replaceFirstChar {it.uppercase()},h,listOf(samples.getValue(id)))) }
        add(HudEntry("archive.hotbar","Vanilla Hotbar",HotbarLayout.editorHud(f.moveHotbar,width,height),listOf("Native Hotbar"),baseWidth=HotbarLayout.WIDTH,baseHeight=HotbarLayout.HEIGHT,paddingX=0))
        add(HudEntry("extra.pip","Picture in Picture",f.pip.hud,listOf("Explicit window capture"),baseWidth=f.pip.width+6,baseHeight=f.pip.width*9/16+6))
        add(HudEntry("archive.pet","Pet Display",f.pet.hud,listOf("[Lvl 100] Sample Pet","Total XP: server snapshot","XP/h: changed snapshots","Sample only"),baseWidth=220,baseHeight=57))
        add(HudEntry("archive.route","Waypoint Route",f.route.hud,listOf("Route: 3/12")))
        add(HudEntry("archive.simon","Simon Says",f.simon.hud,listOf("SS: 3/5")))
        add(HudEntry("archive.hearts","Vanilla Hearts",HeartLayout.editorHud(f.moveHearts,width,height),listOf("Vanilla Hearts"),baseWidth=HeartLayout.WIDTH,baseHeight=HeartLayout.HEIGHT,paddingX=0))
        add(HudEntry("archive.stopwatch","Stopwatch",f.stopwatch.hud,listOf("Timer - Running","00:00:05.123"),baseWidth=160,baseHeight=34))
        val inv=InventoryHudLayout.metrics(f.inventoryHud)
        add(HudEntry("extra.inventory","Inventory HUD",f.inventoryHud.hud,listOf("Inventory sample"),baseWidth=inv.width,baseHeight=inv.height))
        add(HudEntry("extra.item_name","Item Name HUD",f.itemNameHud.hud,listOf("Sample held item"),baseHeight=18,paddingX=16))
        val skill=(if(f.skillHud.showGain) "+9.2 " else "")+"Farming"+(if(f.skillHud.showLevel) " 14" else "")+" 12,345/20,000"
        add(HudEntry("extra.skill","Skill Progress HUD",f.skillHud.hud,listOf(skill),baseHeight=26,paddingX=10+if(f.skillHud.icon) 20 else 0))
        add(HudEntry("extra.scoreboard","Scoreboard",f.scoreboardHud.hud,listOf("Scoreboard sample"),baseWidth=f.scoreboardHud.maxWidth,baseHeight=ScoreboardLayout.height(5,f.scoreboardHud.header)))
        add(HudEntry("map","Map Info",HudSpec(x=f.map.x,y=f.map.y),listOf("Secrets: 25/30 | Score~: 295","Deaths: 0 | Crypts: 5","Sample room | 05:00"),false))
    }
    fun update(f:FeatureSettings,key:String,width:Int=960,height:Int=540,change:(HudSpec)->HudSpec):FeatureSettings {
        val entry=entries(f,width,height).firstOrNull {it.key==key} ?: error("Unknown HUD: $key")
        val h=change(entry.hud);h.validate()
        return when {
            key.startsWith("timer.")->f.copy(timers=f.timers.withHud(key.substringAfter('.'),h))
            key.startsWith("player.")->f.copy(player=f.player.withHud(key.substringAfter('.'),h))
            key=="archive.hotbar"->f.copy(moveHotbar=f.moveHotbar.copy(customPosition=true,hud=h))
            key=="extra.pip"->f.copy(pip=f.pip.copy(hud=h))
            key=="archive.pet"->f.copy(pet=f.pet.copy(hud=h))
            key=="archive.route"->f.copy(route=f.route.copy(hud=h))
            key=="archive.simon"->f.copy(simon=f.simon.copy(hud=h))
            key=="archive.hearts"->f.copy(moveHearts=f.moveHearts.copy(customPosition=true,hud=h))
            key=="archive.stopwatch"->f.copy(stopwatch=f.stopwatch.copy(hud=h))
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
