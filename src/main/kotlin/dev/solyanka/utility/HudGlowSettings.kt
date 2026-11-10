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
 val intensity:HudGlowIntensity=HudGlowIntensity()
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
 val player:Double=1.0
) {
 fun validate() {listOf(hotbar,pictureInPicture,pet,hearts,stopwatch,dvd,scoreboard,themedChat,inventory,itemName,skill,music,player).forEach {
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
  else->error("Unknown HUD glow feature: $id")
 }.also {it.validate()}
}

/** Alpha ramps outwards; calculated separately from rendering for testability. */
object HudGlowProfile {
 const val RADIUS=6
 private val alphaByDistance=intArrayOf(120,82,56,36,22,12)
 fun alpha(distance:Int,intensity:Double):Int {
  require(distance in 1..RADIUS && intensity.isFinite() && intensity in .25..2.0)
  return kotlin.math.round(alphaByDistance[distance-1]*intensity).toInt().coerceIn(0,255)
 }
}
