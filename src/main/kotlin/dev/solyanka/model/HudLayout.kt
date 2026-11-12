/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.*
import kotlin.math.roundToInt

enum class HudPreviewRenderer { YANDEX_MUSIC, VANILLA_HOTBAR, VANILLA_HEARTS, ARCHIVE_HUDS, EXTRA_HUDS, SCOREBOARD }

sealed interface HudSettingTarget {
    data class Timer(val id:String):HudSettingTarget
    data class Player(val id:String):HudSettingTarget
    data object MoveHotbar:HudSettingTarget
    data object Pip:HudSettingTarget
    data object Pet:HudSettingTarget
    data object Music:HudSettingTarget
    data object Route:HudSettingTarget
    data object Simon:HudSettingTarget
    data object MoveHearts:HudSettingTarget
    data object Stopwatch:HudSettingTarget
    data object Inventory:HudSettingTarget
    data object ItemName:HudSettingTarget
    data object Skill:HudSettingTarget
    data object Performance:HudSettingTarget
    data object Scoreboard:HudSettingTarget
    data object Map:HudSettingTarget
}

data class HudDescriptor(
    val key:String,
    val owner:String,
    val setting:HudSettingTarget,
    val preview:HudPreviewRenderer?=null,
)

data class HudEntry(val key:String,val name:String,val hud:HudSpec,val lines:List<String>,val resizable:Boolean=true,val baseWidth:Int?=null,val baseHeight:Int?=null,val paddingX:Int=4) {
    val descriptor:HudDescriptor get() = HudLayout.descriptor(key) ?: error("Unknown HUD: $key")
}
data class HudBounds(val x:Int,val y:Int,val width:Int,val height:Int) {
    fun contains(mx:Double,my:Double)=mx>=x && my>=y && mx<x+width && my<y+height
}
/** The editor uses the same stored positions and scales as live renderers; sample data is editor-only. */
object HudLayout {
    private val fixedDescriptors = listOf(
        HudDescriptor("archive.hotbar",ModuleIds.MOVE_HOTBAR,HudSettingTarget.MoveHotbar,HudPreviewRenderer.VANILLA_HOTBAR),
        HudDescriptor("extra.pip",ModuleIds.PIP,HudSettingTarget.Pip),
        HudDescriptor("archive.pet",ModuleIds.PET,HudSettingTarget.Pet),
        HudDescriptor("extra.music",ModuleIds.MUSIC,HudSettingTarget.Music,HudPreviewRenderer.YANDEX_MUSIC),
        HudDescriptor("archive.route",ModuleIds.WAYPOINTS,HudSettingTarget.Route),
        HudDescriptor("archive.simon",ModuleIds.SIMON,HudSettingTarget.Simon),
        HudDescriptor("archive.hearts",ModuleIds.MOVE_HEARTS,HudSettingTarget.MoveHearts,HudPreviewRenderer.VANILLA_HEARTS),
        HudDescriptor("archive.stopwatch",ModuleIds.STOPWATCH,HudSettingTarget.Stopwatch,HudPreviewRenderer.ARCHIVE_HUDS),
        HudDescriptor("extra.inventory",ModuleIds.INVENTORY_HUD,HudSettingTarget.Inventory,HudPreviewRenderer.EXTRA_HUDS),
        HudDescriptor("extra.item_name",ModuleIds.ITEM_NAME_HUD,HudSettingTarget.ItemName,HudPreviewRenderer.EXTRA_HUDS),
        HudDescriptor("extra.skill",ModuleIds.SKILL_HUD,HudSettingTarget.Skill,HudPreviewRenderer.EXTRA_HUDS),
        HudDescriptor("extra.performance",ModuleIds.PERFORMANCE,HudSettingTarget.Performance),
        HudDescriptor("extra.scoreboard",ModuleIds.SCOREBOARD,HudSettingTarget.Scoreboard,HudPreviewRenderer.SCOREBOARD),
        HudDescriptor("map",ModuleIds.MAP_INFO,HudSettingTarget.Map),
    ).associateBy { it.key }

    fun descriptor(key:String):HudDescriptor? = fixedDescriptors[key] ?: when {
        key.startsWith("timer.")->HudDescriptor(key,ModuleIds.TICK_TIMERS,HudSettingTarget.Timer(key.substringAfter('.')))
        key.startsWith("player.")->HudDescriptor(key,ModuleIds.PLAYER_DISPLAY,HudSettingTarget.Player(key.substringAfter('.')))
        else->null
    }
    fun previewRenderer(key:String):HudPreviewRenderer? = descriptor(key)?.preview

    fun entries(f:FeatureSettings,width:Int=960,height:Int=540):List<HudEntry> = buildList {
        val names=mapOf("goldor" to "Goldor","pad" to "Storm Pad","lightning" to "Storm Lightning","py" to "Storm PY","storm" to "Storm Phase","freeze" to "M3 Fire Freeze")
        f.timers.huds().forEach { (id,h) ->add(HudEntry("timer.$id",names.getValue(id),h,listOf("${names.getValue(id)}: 1.75s"))) }
        val samples=mapOf("health" to "3000/4000 HP","mana" to "2000/20000 Mana","overflow" to "333 Overflow","defense" to "1000 Defense","vitality" to "100 Vitality","ehp" to "33000 EHP","speed" to "400 Speed")
        f.player.huds().forEach { (id,h) -> add(HudEntry("player.$id",id.replaceFirstChar {it.uppercase()},h,listOf(samples.getValue(id)))) }
        add(HudEntry("archive.hotbar","Vanilla Hotbar",HotbarLayout.editorHud(f.moveHotbar,width,height),listOf("Native Hotbar"),baseWidth=HotbarLayout.geometry(f.moveHotbar).width,baseHeight=HotbarLayout.geometry(f.moveHotbar).height,paddingX=0))
        add(HudEntry("extra.pip","Picture in Picture",f.pip.hud,listOf("Explicit window capture"),baseWidth=f.pip.width+6,baseHeight=f.pip.width*9/16+6))
        add(HudEntry("archive.pet","Pet Display",f.pet.hud,listOf("[Lvl 100] Sample Pet","Total XP: server snapshot","XP/h: changed snapshots","Sample only"),baseWidth=220,baseHeight=57))
        add(HudEntry("extra.music","Yandex Music",f.music.hud,listOf("Artist — Song"),baseWidth=208,baseHeight=if(f.music.showLyrics) 92 else 66))
        add(HudEntry("archive.route","Waypoint Route",f.route.hud,listOf("Route: 3/12")))
        add(HudEntry("archive.simon","Simon Says",f.simon.hud,listOf("SS: 3/5")))
        add(HudEntry("archive.hearts","Vanilla Hearts",HeartLayout.editorHud(f.moveHearts,width,height),listOf("Vanilla Hearts"),baseWidth=HeartLayout.WIDTH,baseHeight=HeartLayout.HEIGHT,paddingX=0))
        add(HudEntry("archive.stopwatch","Stopwatch",f.stopwatch.hud,listOf("Timer - Running","00:00:05.123"),baseWidth=160,baseHeight=34))
        val inv=InventoryHudLayout.metrics(f.inventoryHud)
        add(HudEntry("extra.inventory","Inventory HUD",f.inventoryHud.hud,listOf("Inventory sample"),baseWidth=inv.width,baseHeight=inv.height))
        add(HudEntry("extra.item_name","Item Name HUD",f.itemNameHud.hud,listOf("Sample held item"),baseHeight=18,paddingX=16))
        val skill=(if(f.skillHud.showGain) "+9.2 " else "")+"Farming"+(if(f.skillHud.showLevel) " 14" else "")+" 12,345/20,000"
        add(HudEntry("extra.skill","Skill Progress HUD",f.skillHud.hud,listOf(skill),baseHeight=26,paddingX=10+if(f.skillHud.icon) 20 else 0))
        add(HudEntry("extra.performance","Performance",f.performance.hud,listOf("FPS: 120 | Ping: 42 ms | TPS: 20.0")))
        add(HudEntry("extra.scoreboard","Scoreboard",f.scoreboardHud.hud,listOf("Scoreboard sample"),baseWidth=f.scoreboardHud.maxWidth,baseHeight=ScoreboardLayout.height(5,f.scoreboardHud.header)))
        add(HudEntry("map","Map Info",HudSpec(x=f.map.x,y=f.map.y),listOf("Secrets: 25/30 | Score~: 295","Deaths: 0 | Crypts: 5","Sample room | 05:00"),false))
    }
    fun owner(key:String):String=descriptor(key)?.owner ?: throw NoSuchElementException(key)
    fun visibleEntries(f:FeatureSettings,enabled:Set<String>,width:Int=960,height:Int=540)=entries(f,width,height).filter {
        it.descriptor.owner in enabled && it.hud.enabled && (it.descriptor.setting!=HudSettingTarget.Route || f.route.progress)
    }
    fun update(f:FeatureSettings,key:String,width:Int=960,height:Int=540,change:(HudSpec)->HudSpec):FeatureSettings {
        val entry=entries(f,width,height).firstOrNull {it.key==key} ?: error("Unknown HUD: $key")
        val target=entry.descriptor.setting
        val changed=change(entry.hud)
        val h=if(target==HudSettingTarget.MoveHotbar && f.moveHotbar.referenceSizing)changed.copy(scale=if(changed.scale==entry.hud.scale)f.moveHotbar.hud.scale else (changed.scale/HotbarLayout.referenceScale(f.moveHotbar,width,height)).coerceIn(.5,4.0))else changed
        h.validate()
        return when(target) {
            is HudSettingTarget.Timer->f.copy(timers=f.timers.withHud(target.id,h))
            is HudSettingTarget.Player->f.copy(player=f.player.withHud(target.id,h))
            HudSettingTarget.MoveHotbar->f.copy(moveHotbar=f.moveHotbar.copy(customPosition=true,hud=h))
            HudSettingTarget.Music->f.copy(music=f.music.copy(hud=h))
            HudSettingTarget.Pip->f.copy(pip=f.pip.copy(hud=h))
            HudSettingTarget.Pet->f.copy(pet=f.pet.copy(hud=h))
            HudSettingTarget.Route->f.copy(route=f.route.copy(hud=h))
            HudSettingTarget.Simon->f.copy(simon=f.simon.copy(hud=h))
            HudSettingTarget.MoveHearts->f.copy(moveHearts=f.moveHearts.copy(customPosition=true,hud=h))
            HudSettingTarget.Stopwatch->f.copy(stopwatch=f.stopwatch.copy(hud=h))
            HudSettingTarget.Inventory->f.copy(inventoryHud=f.inventoryHud.copy(hud=h))
            HudSettingTarget.ItemName->f.copy(itemNameHud=f.itemNameHud.copy(hud=h))
            HudSettingTarget.Performance->f.copy(performance=f.performance.copy(hud=h))
            HudSettingTarget.Scoreboard->f.copy(scoreboardHud=f.scoreboardHud.copy(hud=h))
            HudSettingTarget.Skill->f.copy(skillHud=f.skillHud.copy(hud=h))
            HudSettingTarget.Map->f.copy(map=f.map.copy(x=h.x,y=h.y))
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
