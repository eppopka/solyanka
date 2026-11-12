/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.solyanka.utility.*
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Explicit whitelist of portable layout data: no commands, tokens, or other module settings. */
data class HudPlacement(val key:String,val x:Int,val y:Int,val scale:Double,val customPosition:Boolean=false,val anchorRight:Boolean=false)
data class HudLayoutSnapshot(val placements:List<HudPlacement>)
data class HudThemeColors(val scoreBackground:Int,val scoreBorder:Int,val scoreAccent:Int,val chatBackground:Int,val chatBorder:Int,val chatInput:Int,val inventoryBackground:Int,val itemBackground:Int,val skillBackground:Int)
data class HudShare(val version:Int=1,val layout:HudLayoutSnapshot,val gui:ClickGuiSettings,val lighting:HudLightingSettings,val glow:HudGlowSettings,val colors:HudThemeColors)
object HudLayouts {
 private val gson=Gson()
 fun capture(f:FeatureSettings):HudLayoutSnapshot {
  val values=mutableListOf<HudPlacement>()
  fun put(key:String,h:HudSpec,custom:Boolean=false,right:Boolean=false) {values.add(HudPlacement(key,h.x,h.y,h.scale,custom,right))}
  f.timers.huds().forEach {(k,v)->put("timer.$k",v)};f.player.huds().forEach {(k,v)->put("player.$k",v)}
  put("archive.hotbar",f.moveHotbar.hud,f.moveHotbar.customPosition)
  put("archive.hearts",f.moveHearts.hud,f.moveHearts.customPosition)
  put("extra.scoreboard",f.scoreboardHud.hud,false,f.scoreboardHud.anchorRight)
  put("extra.pip",f.pip.hud);put("archive.pet",f.pet.hud);put("extra.music",f.music.hud)
  put("archive.route",f.route.hud);put("archive.simon",f.simon.hud);put("archive.stopwatch",f.stopwatch.hud)
  put("extra.inventory",f.inventoryHud.hud);put("extra.item_name",f.itemNameHud.hud);put("extra.skill",f.skillHud.hud);put("extra.performance",f.performance.hud)
  put("map",HudSpec(x=f.map.x,y=f.map.y))
  return HudLayoutSnapshot(values)
 }
 fun apply(f:FeatureSettings,snapshot:HudLayoutSnapshot):FeatureSettings {
  val legal=capture(f).placements.map {it.key}.toSet()
  require(snapshot.placements.size<=legal.size && snapshot.placements.map {it.key}.distinct().size==snapshot.placements.size)
  require(snapshot.placements.all {it.key in legal && it.x in 0..10000 && it.y in 0..10000 && it.scale.isFinite() && it.scale in .5..4.0})
  var result=f
  for(p in snapshot.placements) {
   fun at(h:HudSpec)=h.copy(x=p.x,y=p.y,scale=p.scale)
   result=when(p.key) {
    "archive.hotbar"->result.copy(moveHotbar=result.moveHotbar.copy(hud=at(result.moveHotbar.hud),customPosition=p.customPosition))
    "archive.hearts"->result.copy(moveHearts=result.moveHearts.copy(hud=at(result.moveHearts.hud),customPosition=p.customPosition))
    "extra.scoreboard"->result.copy(scoreboardHud=result.scoreboardHud.copy(hud=at(result.scoreboardHud.hud),anchorRight=p.anchorRight))
    else->HudLayout.update(result,p.key,change=::at)
   }
  }
  result.validate();return result
 }
 fun encode(f:FeatureSettings):String {
  // Favorites are personal shortcuts, not part of a theme or layout.
  val colors=HudThemeColors(f.scoreboardHud.backgroundColor,f.scoreboardHud.borderColor,f.scoreboardHud.accentColor,f.themedChat.backgroundColor,f.themedChat.borderColor,f.themedChat.inputColor,f.inventoryHud.backgroundColor,f.itemNameHud.backgroundColor,f.skillHud.backgroundColor)
  val share=HudShare(layout=capture(f),gui=f.clickGui.copy(favorites=emptyList()),lighting=f.hudLighting,glow=f.hudGlow,colors=colors)
  return "solyanka-layout-v1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(share).toByteArray(StandardCharsets.UTF_8))
 }
 fun decodeAndApply(f:FeatureSettings,text:String):FeatureSettings {
  require(text.length<=100_000 && text.startsWith("solyanka-layout-v1:")) {"Invalid layout share string"}
  val raw=String(Base64.getUrlDecoder().decode(text.substringAfter(':')),StandardCharsets.UTF_8)
  val obj=JsonParser.parseString(raw).asJsonObject
  require(obj.get("version")?.asInt==1 && obj.get("layout")?.isJsonObject==true && obj.getAsJsonObject("layout").get("placements")?.isJsonArray==true)
  // Decode through validated feature settings so malformed theme and lighting are rejected.
  val overlay=gson.toJsonTree(f).asJsonObject
  for((k,v) in listOf("clickGui" to "gui","hudLighting" to "lighting","hudGlow" to "glow")) {
   require(obj.get(v)?.isJsonObject==true);overlay.add(k,obj.get(v))
  }
  val imported=FeatureCodec.decode(overlay)
  val layout=gson.fromJson(obj.get("layout"),HudLayoutSnapshot::class.java)
  val colorJson=obj.get("colors")?.takeIf {it.isJsonObject}?.asJsonObject ?: error("Missing colors")
  fun color(k:String):Int {val v=colorJson.get(k);require(v!=null && v.isJsonPrimitive && v.asJsonPrimitive.isNumber);return v.asBigDecimal.intValueExact()}
  val colored=imported.copy(clickGui=imported.clickGui.copy(favorites=f.clickGui.favorites),
   scoreboardHud=imported.scoreboardHud.copy(backgroundColor=color("scoreBackground"),borderColor=color("scoreBorder"),accentColor=color("scoreAccent")),
   themedChat=imported.themedChat.copy(backgroundColor=color("chatBackground"),borderColor=color("chatBorder"),inputColor=color("chatInput")),
   inventoryHud=imported.inventoryHud.copy(backgroundColor=color("inventoryBackground")),
   itemNameHud=imported.itemNameHud.copy(backgroundColor=color("itemBackground")),skillHud=imported.skillHud.copy(backgroundColor=color("skillBackground")))
  return apply(colored,layout)
 }
}
