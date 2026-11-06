/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import com.google.gson.*
import dev.solyanka.client.feature.DungeonWaypoints
import dev.solyanka.client.feature.DungeonPuzzles
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
object WaypointUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  val raw=mutableMapOf<String,String>();var actionError:String?=null;val errors=linkedMapOf<String,String>();val parseErrors=linkedMapOf<String,String>();var newName="";var oldName=""
  var draft=Gson().toJsonTree(model.general.features.waypoints).asJsonObject
  if(model.general.features.route.lines && !draft["lines"].asBoolean) {draft.addProperty("lines",true);draft.addProperty("lineSize",model.general.features.route.width)}
  fun status()=parseErrors.values.firstOrNull() ?: errors.values.firstOrNull() ?: actionError ?: model.error ?: DungeonWaypoints.error
  lateinit var panel:SettingsPanel
  fun run(action:()->Unit) {try {require(errors.isEmpty() && parseErrors.isEmpty()) {"Correct invalid fields first"};action();actionError=null} catch(e:Exception) {actionError=e.message ?: "Waypoint operation failed"};panel.refreshStatus()}
  fun change(field:String,value:JsonPrimitive) {
   draft.add(field,value);parseErrors.remove(field)
   if(parseErrors.isNotEmpty()) {panel.refreshStatus();return}
   try {val root=Gson().toJsonTree(model.general.features).asJsonObject;root.add("waypoints",draft);val next=FeatureCodec.decode(root)
    val route=when(field) {"lines"->next.route.copy(lines=value.asBoolean);"lineSize"->next.route.copy(width=value.asDouble);else->next.route}
    model.updateGeneral {it.copy(features=next.copy(route=route))};errors.clear();if(model.error==null) DungeonWaypoints.reload()
   } catch(e:Exception) {errors[field]=e.message ?: "Invalid waypoint settings"}
   panel.refreshStatus()
  }
  fun remount() {panel.mount(host.width,host.height)}
  panel=SettingsPanel("Waypoint Pack Config",{host.closePanel()},{status()}) {_,w ->
   buildList {
    add(GuiKit.description(w,"Local Odin-compatible JSON or Base64+GZIP packs; clipboard import/export only, no URL fetching. No secret pack is bundled. Place/edit waypoints manually: Editor Mode + right click a block (click again removes); normal block use is blocked in edit mode. /solyanka waypoint add <title> sets a title."))
    add(GuiKit.description(w,"Packs: ${runCatching {DungeonWaypoints.names().joinToString()}.getOrDefault("unavailable")}. Selected Packs: comma-separated names. Edit Pack must exist; select it to see edits. SkyBlock area names must be visible in the player list; unknown areas and unloaded room geometry fail closed. Disable Editor Mode for normal play."))
    add(GuiKit.description(w,"Waypoint groups:"))
    runCatching {DungeonWaypoints.names()}.getOrDefault(emptyList()).forEach {name ->
     val count=runCatching {DungeonWaypoints.count(name).toString()}.getOrDefault("unavailable")
     add(GuiKit.description(w,"$name — $count waypoints"))
    }
    add(GuiKit.button("Route Lines / Progress",w){RouteUi.open(host,model)})
    draft.entrySet().forEach {(field,value) ->val label=when(field) {
     "titleScale"->"Text Size";"lines"->"Waypoint Lines";"globalEsp"->"Global Depth (through blocks)";"allowEdits"->"Editor Mode";"blockSize"->"Use Block Size"
     else->field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar {it.uppercase()}
    }
     if(field=="lineSize" && !draft["lines"].asBoolean || field in setOf("sizeX","sizeY","sizeZ") && draft["blockSize"].asBoolean) return@forEach
     if(field=="color") add(GuiKit.colorRow(w,label,value.asInt) {change(field,JsonPrimitive(it))})
     else if(field=="type") add(GuiKit.choiceRow(w,label,dev.solyanka.ui.SettingsCatalog.waypointTypes,value.asInt) {change(field,JsonPrimitive(it))})
     else if(value.asJsonPrimitive.isBoolean) add(GuiKit.toggleRow(w,label,value.asBoolean) {change(field,JsonPrimitive(it));if(field in setOf("lines","blockSize")) remount()})
     else if(value.asJsonPrimitive.isString) add(GuiKit.textRow(w,label,raw.getOrPut(field) {value.asString}) {input->raw[field]=input;change(field,JsonPrimitive(input))})
     else add(GuiKit.sliderRow(w,label,value.asDouble,dev.solyanka.ui.SettingsNumbers.range("waypoints",field,host.width,host.height,value.asDouble)) {input ->change(field,JsonPrimitive(input))})
    }
    add(GuiKit.textRow(w,"New / import / rename-to pack name",newName) {newName=it})
    add(GuiKit.textRow(w,"Existing pack name (export / delete / rename)",oldName) {oldName=it})
    add(GuiKit.row(w).apply {
     child(GuiKit.button("Create",65) {run {DungeonWaypoints.create(newName)};remount()})
     child(GuiKit.button("Import clipboard",125) {run {DungeonWaypoints.import(newName,Minecraft.getInstance().keyboardHandler.clipboard)};remount()})
     child(GuiKit.button("Export",65) {run {Minecraft.getInstance().keyboardHandler.clipboard=DungeonWaypoints.export(oldName)}})
    })
    add(GuiKit.row(w).apply {
     child(GuiKit.button("Rename inactive",125) {run {require(oldName !in model.general.features.waypoints.selectedPacks.split(',').map(String::trim) && oldName!=model.general.features.waypoints.editPack) {"Switch away from this pack before renaming"};DungeonWaypoints.rename(oldName,newName)};remount()})
     child(GuiKit.button("Delete inactive",125) {run {require(oldName !in model.general.features.waypoints.selectedPacks.split(',').map(String::trim) && oldName!=model.general.features.waypoints.editPack) {"Switch away from this pack before deleting"};DungeonWaypoints.delete(oldName)};remount()})
    })
    add(GuiKit.row(w).apply {child(GuiKit.button("Reload packs",100) {run {DungeonWaypoints.reload()};remount()});child(GuiKit.button("Reset collected",115) {run {DungeonWaypoints.resetClicked()}})})
    add(GuiKit.description(w,"Current area: ${if(FeatureRuntime.context.dungeon && !FeatureRuntime.context.boss) DungeonPuzzles.currentRoom()?.name ?: "room not recognized" else FeatureRuntime.context.area.ifBlank { "not recognized" }}. Deletion below persists immediately."))
    val rows=runCatching {DungeonWaypoints.roomRows()}.getOrDefault(emptyList())
    rows.forEachIndexed {i,way->add(GuiKit.row(w).apply {child(GuiKit.label("${i+1}: ${way.type} ${way.blockPos} ${way.title}").apply {horizontalSizing(io.wispforest.owo.ui.core.Sizing.fixed(w-100))});child(GuiKit.button("Delete",60) {run {DungeonWaypoints.remove(i)};remount()})})}
   }
  }
  host.showPanel(panel)
 }
}
