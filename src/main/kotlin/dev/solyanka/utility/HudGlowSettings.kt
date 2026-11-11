/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Independent, opt-in HUD-category effect settings. Existing configs default to off. */
data class HudGlowSettings(
 val hotbar:Boolean=false,
 val pictureInPicture:Boolean=false,
 val pet:Boolean=false,
 val hearts:Boolean=false,
 val stopwatch:Boolean=false,
 val dvd:Boolean=false,
 val scoreboard:Boolean=false,
 val themedChat:Boolean=false,
 val inventory:Boolean=false,
 val itemName:Boolean=false,
 val skill:Boolean=false,
 val music:Boolean=false,
 val player:Boolean=false,
 val timers:Boolean=false,
 val simon:Boolean=false,
 val route:Boolean=false,
 val mapInfo:Boolean=false,
 val performance:Boolean=false,
 val intensity:HudGlowIntensity=HudGlowIntensity(),
 val custom:HudGlowOverrides=HudGlowOverrides(),
 val colors:HudGlowColors=HudGlowColors()
) {
 fun validate()=intensity.validate()
 fun enabled(id:String):Boolean=when(id) {
  "custom.move_hotbar"->hotbar
  "render.picture_in_picture"->pictureInPicture
  "custom.pet_display"->pet
  "custom.move_hearts"->hearts
  "custom.stopwatch"->stopwatch
  "custom.dvd_bounce"->dvd
  "hud.scoreboard"->scoreboard
  "hud.themed_chat"->themedChat
  "hud.inventory"->inventory
  "hud.item_name"->itemName
  "hud.skill_progress"->skill
  "hud.yandex_music"->music
  "hud.player_display"->player
  "dungeon.tick_timers"->timers
  "custom.simon_says"->simon
  "dungeon.waypoints"->route
  "dungeon.map_info"->mapInfo
  "hud.performance"->performance
  else->false
 }
 fun withEnabled(id:String,value:Boolean):HudGlowSettings=when(id) {
  "custom.move_hotbar"->copy(hotbar=value)
  "render.picture_in_picture"->copy(pictureInPicture=value)
  "custom.pet_display"->copy(pet=value)
  "custom.move_hearts"->copy(hearts=value)
  "custom.stopwatch"->copy(stopwatch=value)
  "custom.dvd_bounce"->copy(dvd=value)
  "hud.scoreboard"->copy(scoreboard=value)
  "hud.themed_chat"->copy(themedChat=value)
  "hud.inventory"->copy(inventory=value)
  "hud.item_name"->copy(itemName=value)
  "hud.skill_progress"->copy(skill=value)
  "hud.yandex_music"->copy(music=value)
  "hud.player_display"->copy(player=value)
  "dungeon.tick_timers"->copy(timers=value)
  "custom.simon_says"->copy(simon=value)
  "dungeon.waypoints"->copy(route=value)
  "dungeon.map_info"->copy(mapInfo=value)
  "hud.performance"->copy(performance=value)
  else->error("Unknown HUD glow feature: $id")
 }
}

/** Each feature owns its own strength; old configs acquire the visible default. */
data class HudGlowIntensity(
 val hotbar:Double=1.0,
 val pictureInPicture:Double=1.0,
 val pet:Double=1.0,
 val hearts:Double=1.0,
 val stopwatch:Double=1.0,
 val dvd:Double=1.0,
 val scoreboard:Double=1.0,
 val themedChat:Double=1.0,
 val inventory:Double=1.0,
 val itemName:Double=1.0,
 val skill:Double=1.0,
 val music:Double=1.0,
 val player:Double=1.0,
 val timers:Double=1.0,
 val simon:Double=1.0,
 val route:Double=1.0,
 val mapInfo:Double=1.0,
 val performance:Double=1.0
) {
 fun validate() {listOf(hotbar,pictureInPicture,pet,hearts,stopwatch,dvd,scoreboard,themedChat,inventory,itemName,skill,music,player,timers,simon,route,mapInfo,performance).forEach {
  require(it.isFinite() && it in .25..2.0) {"HUD glow intensity must be 0.25–2"}
 }}
 fun value(id:String):Double=when(id) {
  "custom.move_hotbar"->hotbar
  "render.picture_in_picture"->pictureInPicture
  "custom.pet_display"->pet
  "custom.move_hearts"->hearts
  "custom.stopwatch"->stopwatch
  "custom.dvd_bounce"->dvd
  "hud.scoreboard"->scoreboard
  "hud.themed_chat"->themedChat
  "hud.inventory"->inventory
  "hud.item_name"->itemName
  "hud.skill_progress"->skill
  "hud.yandex_music"->music
  "hud.player_display"->player
  "dungeon.tick_timers"->timers
  "custom.simon_says"->simon
  "dungeon.waypoints"->route
  "dungeon.map_info"->mapInfo
  "hud.performance"->performance
  else->1.0
 }
 fun withValue(id:String,value:Double):HudGlowIntensity=when(id) {
  "custom.move_hotbar"->copy(hotbar=value)
  "render.picture_in_picture"->copy(pictureInPicture=value)
  "custom.pet_display"->copy(pet=value)
  "custom.move_hearts"->copy(hearts=value)
  "custom.stopwatch"->copy(stopwatch=value)
  "custom.dvd_bounce"->copy(dvd=value)
  "hud.scoreboard"->copy(scoreboard=value)
  "hud.themed_chat"->copy(themedChat=value)
  "hud.inventory"->copy(inventory=value)
  "hud.item_name"->copy(itemName=value)
  "hud.skill_progress"->copy(skill=value)
  "hud.yandex_music"->copy(music=value)
  "hud.player_display"->copy(player=value)
  "dungeon.tick_timers"->copy(timers=value)
  "custom.simon_says"->copy(simon=value)
  "dungeon.waypoints"->copy(route=value)
  "dungeon.map_info"->copy(mapInfo=value)
  "hud.performance"->copy(performance=value)
  else->error("Unknown HUD glow feature: $id")
 }.also {it.validate()}
}

/** Visual settings shared by all opted-in HUD modules. */
data class HudLightingSettings(val color:Int=0xff7dd3fc.toInt(),val intensity:Double=1.0,val enabled:Boolean=true) {
 fun validate() {require(intensity.isFinite() && intensity in .25..2.0)}
}
data class HudGlowStyle(val color:Int,val intensity:Double)
fun HudGlowSettings.style(id:String,global:HudLightingSettings):HudGlowStyle =
 if(custom.value(id)) HudGlowStyle(colors.value(id),intensity.value(id)) else HudGlowStyle(global.color,global.intensity)

data class HudGlowOverrides(
 val hotbar:Boolean=false,
 val pictureInPicture:Boolean=false,
 val pet:Boolean=false,
 val hearts:Boolean=false,
 val stopwatch:Boolean=false,
 val dvd:Boolean=false,
 val scoreboard:Boolean=false,
 val themedChat:Boolean=false,
 val inventory:Boolean=false,
 val itemName:Boolean=false,
 val skill:Boolean=false,
 val music:Boolean=false,
 val player:Boolean=false,
 val timers:Boolean=false,
 val simon:Boolean=false,
 val route:Boolean=false,
 val mapInfo:Boolean=false,
 val performance:Boolean=false
) {
 fun value(id:String):Boolean=when(id) {
  "custom.move_hotbar"->hotbar
  "render.picture_in_picture"->pictureInPicture
  "custom.pet_display"->pet
  "custom.move_hearts"->hearts
  "custom.stopwatch"->stopwatch
  "custom.dvd_bounce"->dvd
  "hud.scoreboard"->scoreboard
  "hud.themed_chat"->themedChat
  "hud.inventory"->inventory
  "hud.item_name"->itemName
  "hud.skill_progress"->skill
  "hud.yandex_music"->music
  "hud.player_display"->player
  "dungeon.tick_timers"->timers
  "custom.simon_says"->simon
  "dungeon.waypoints"->route
  "dungeon.map_info"->mapInfo
  "hud.performance"->performance
  else->false
 }
 fun withValue(id:String,value:Boolean):HudGlowOverrides=when(id) {
  "custom.move_hotbar"->copy(hotbar=value)
  "render.picture_in_picture"->copy(pictureInPicture=value)
  "custom.pet_display"->copy(pet=value)
  "custom.move_hearts"->copy(hearts=value)
  "custom.stopwatch"->copy(stopwatch=value)
  "custom.dvd_bounce"->copy(dvd=value)
  "hud.scoreboard"->copy(scoreboard=value)
  "hud.themed_chat"->copy(themedChat=value)
  "hud.inventory"->copy(inventory=value)
  "hud.item_name"->copy(itemName=value)
  "hud.skill_progress"->copy(skill=value)
  "hud.yandex_music"->copy(music=value)
  "hud.player_display"->copy(player=value)
  "dungeon.tick_timers"->copy(timers=value)
  "custom.simon_says"->copy(simon=value)
  "dungeon.waypoints"->copy(route=value)
  "dungeon.map_info"->copy(mapInfo=value)
  "hud.performance"->copy(performance=value)
  else->error("Unknown HUD glow feature: $id")
 }
}

data class HudGlowColors(
 val hotbar:Int=0xff7dd3fc.toInt(),
 val pictureInPicture:Int=0xff7dd3fc.toInt(),
 val pet:Int=0xff7dd3fc.toInt(),
 val hearts:Int=0xff7dd3fc.toInt(),
 val stopwatch:Int=0xff7dd3fc.toInt(),
 val dvd:Int=0xff7dd3fc.toInt(),
 val scoreboard:Int=0xff7dd3fc.toInt(),
 val themedChat:Int=0xff7dd3fc.toInt(),
 val inventory:Int=0xff7dd3fc.toInt(),
 val itemName:Int=0xff7dd3fc.toInt(),
 val skill:Int=0xff7dd3fc.toInt(),
 val music:Int=0xff7dd3fc.toInt(),
 val player:Int=0xff7dd3fc.toInt(),
 val timers:Int=0xff7dd3fc.toInt(),
 val simon:Int=0xff7dd3fc.toInt(),
 val route:Int=0xff7dd3fc.toInt(),
 val mapInfo:Int=0xff7dd3fc.toInt(),
 val performance:Int=0xff7dd3fc.toInt()
) {
 fun value(id:String):Int=when(id) {
  "custom.move_hotbar"->hotbar
  "render.picture_in_picture"->pictureInPicture
  "custom.pet_display"->pet
  "custom.move_hearts"->hearts
  "custom.stopwatch"->stopwatch
  "custom.dvd_bounce"->dvd
  "hud.scoreboard"->scoreboard
  "hud.themed_chat"->themedChat
  "hud.inventory"->inventory
  "hud.item_name"->itemName
  "hud.skill_progress"->skill
  "hud.yandex_music"->music
  "hud.player_display"->player
  "dungeon.tick_timers"->timers
  "custom.simon_says"->simon
  "dungeon.waypoints"->route
  "dungeon.map_info"->mapInfo
  "hud.performance"->performance
  else->0xff7dd3fc.toInt()
 }
 fun withValue(id:String,value:Int):HudGlowColors=when(id) {
  "custom.move_hotbar"->copy(hotbar=value)
  "render.picture_in_picture"->copy(pictureInPicture=value)
  "custom.pet_display"->copy(pet=value)
  "custom.move_hearts"->copy(hearts=value)
  "custom.stopwatch"->copy(stopwatch=value)
  "custom.dvd_bounce"->copy(dvd=value)
  "hud.scoreboard"->copy(scoreboard=value)
  "hud.themed_chat"->copy(themedChat=value)
  "hud.inventory"->copy(inventory=value)
  "hud.item_name"->copy(itemName=value)
  "hud.skill_progress"->copy(skill=value)
  "hud.yandex_music"->copy(music=value)
  "hud.player_display"->copy(player=value)
  "dungeon.tick_timers"->copy(timers=value)
  "custom.simon_says"->copy(simon=value)
  "dungeon.waypoints"->copy(route=value)
  "dungeon.map_info"->copy(mapInfo=value)
  "hud.performance"->copy(performance=value)
  else->error("Unknown HUD glow feature: $id")
 }
}
