/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.DungeonWaypoints
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.NumberRange
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft

object WaypointUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  var newName="";var transferName="";var actionError:String?=null
  val rename=mutableMapOf<String,String>()
  lateinit var panel:SettingsPanel
  fun refresh()=panel.mount(host.width,host.height)
  fun act(action:()->Unit) {try {action();actionError=null} catch(e:Exception) {actionError=e.message ?: "Waypoint operation failed"};panel.refreshStatus()}
  fun update(block:(WaypointSettings)->WaypointSettings):Boolean {
   val old=model.general
   val ok=model.updateGeneral {it.copy(features=it.features.copy(waypoints=block(it.features.waypoints)))}
   if(!ok) {val message=model.error;model.updateGeneral {old};actionError=message ?: "Could not save settings"} else actionError=null
   panel.refreshStatus();return ok
  }
  fun route(block:(RouteSettings)->RouteSettings) {
   val old=model.general;val ok=model.updateGeneral {it.copy(features=it.features.copy(route=block(it.features.route)))}
   if(!ok) {val message=model.error;model.updateGeneral {old};actionError=message ?: "Could not save settings"} else actionError=null
   panel.refreshStatus()
  }
  fun both(block:(FeatureSettings)->FeatureSettings) {
   val old=model.general;val ok=model.updateGeneral {it.copy(features=block(it.features))}
   if(!ok) {val message=model.error;model.updateGeneral {old};actionError=message ?: "Could not save settings"} else actionError=null
   panel.refreshStatus()
  }
  fun active()=model.general.features.waypoints.selectedPacks.split(',').map(String::trim)
  panel=SettingsPanel("Waypoint Pack Config",{host.closePanel()},{actionError ?: model.error ?: DungeonWaypoints.error},centeredTitle=true) {p,w->buildList {
   add(GuiKit.description(w,"Packs — choose which packs are visible and which one to edit."))
   val names=runCatching {DungeonWaypoints.names()}.getOrDefault(emptyList())
   names.forEach {name->
    val enabled=name in active();val editing=model.general.features.waypoints.editPack==name
    val count=runCatching {DungeonWaypoints.count(name).toString()}.getOrDefault("unavailable")
    add(GuiKit.textRow(w,"$name — $count waypoints",rename.getOrPut(name) {name}) {rename[name]=it})
    add(GuiKit.row(w).apply {
     child(GuiKit.button(if(enabled) "Visible" else "Hidden",65) {act {val next=if(enabled) active()-name else active()+name;require(next.isNotEmpty()) {"Keep at least one visible pack"};check(update {it.copy(selectedPacks=next.joinToString(","))}) {actionError ?: "Could not save"}};refresh()})
     child(GuiKit.button(if(editing) "Editing" else "Edit",62) {act {check(update {it.copy(editPack=name)}) {actionError ?: "Could not save"}};refresh()})
     child(GuiKit.button("Rename",65) {act {
      val target=rename[name]?.trim().orEmpty();val old=model.general
      DungeonWaypoints.rename(name,target)
      try {check(update {it.copy(editPack=if(it.editPack==name) target else it.editPack,selectedPacks=active().map {v->if(v==name) target else v}.joinToString(","))}) {actionError ?: "Could not save"}}
      catch(e:Exception) {val rollback=runCatching {DungeonWaypoints.rename(target,name)};model.updateGeneral {old};if(rollback.isFailure || model.error!=null) error("Rename failed; rollback incomplete. Check packs and settings before retrying.");throw e}
      rename.remove(name);DungeonWaypoints.reload()
     };refresh()})
     child(GuiKit.button("Delete",60) {act {
      val other=names.firstOrNull {it!=name} ?: error("Create another pack before deleting the last one")
      val old=model.general;val remaining=active().filter {it!=name}.ifEmpty {listOf(other)}
      check(update {it.copy(editPack=if(it.editPack==name) other else it.editPack,selectedPacks=remaining.joinToString(","))}) {actionError ?: "Could not save"}
      try {DungeonWaypoints.delete(name)} catch(e:Exception) {model.updateGeneral {old};DungeonWaypoints.reload();if(model.error!=null) error("Delete failed; settings rollback incomplete. Check packs and settings before retrying.");throw e}
     };refresh()})
    })
   }
   add(GuiKit.textRow(w,"New pack name",newName) {newName=it})
   add(GuiKit.button("Create pack",100) {act {DungeonWaypoints.create(newName)};refresh()})
   add(GuiKit.description(w,"Viewer settings"))
   val s=model.general.features.waypoints;val r=model.general.features.route
   add(GuiKit.sliderRow(w,"Text Size",s.titleScale,NumberRange(.1,4.0,.05)) {v->update {it.copy(titleScale=v)}})
   val fonts=listOf("default","uniform","alt")
   add(GuiKit.choiceRow(w,"Text Font",fonts,fonts.indexOf(s.textFont).coerceAtLeast(0)) {i->update {it.copy(textFont=fonts[i])}})
   add(GuiKit.toggleRow(w,"Show Labels",s.titles) {v->update {it.copy(titles=v)}})
   add(GuiKit.toggleRow(w,"Global Depth (through blocks)",s.globalEsp) {v->update {it.copy(globalEsp=v)}})
   add(GuiKit.toggleRow(w,"Show Authored Effects",s.styles,"Disabled keeps each waypoint's base color.") {v->update {it.copy(styles=v)}})
   add(GuiKit.toggleRow(w,"Override Text Color",s.useTextColor) {v->update {it.copy(useTextColor=v)};refresh()})
   if(s.useTextColor) add(GuiKit.colorRow(w,"Text Color",s.textColor) {v->update {it.copy(textColor=v)}})
   add(GuiKit.toggleRow(w,"Waypoint Lines",s.lines || r.lines) {v->both {it.copy(waypoints=it.waypoints.copy(lines=v),route=it.route.copy(lines=v))};refresh()})
   if(s.lines || r.lines) {
    add(GuiKit.sliderRow(w,"Line Size",if(s.lines) s.lineSize else r.width,NumberRange(1.0,6.0,.5)) {v->both {it.copy(waypoints=it.waypoints.copy(lineSize=v),route=it.route.copy(width=v))}})
    add(GuiKit.colorRow(w,"Line Color",r.color) {v->route {it.copy(color=v)}})
    add(GuiKit.colorRow(w,"Start Color",r.startColor) {v->route {it.copy(startColor=v)};DungeonWaypoints.resetRoute()})
    add(GuiKit.sliderRow(w,"Activation Radius",r.radius,NumberRange(1.0,15.0,.5)) {v->route {it.copy(radius=v)}})
    add(GuiKit.toggleRow(w,"Follow Progress",r.follow) {v->route {it.copy(follow=v)}})
    add(GuiKit.sliderRow(w,"Segments Ahead",r.ahead.toDouble(),NumberRange(1.0,20.0,1.0)) {v->route {it.copy(ahead=v.toInt())}})
    add(GuiKit.toggleRow(w,"Lines Through Walls",r.throughWalls) {v->route {it.copy(throughWalls=v)}})
    add(GuiKit.toggleRow(w,"Skip Collected",r.skipCollected) {v->route {it.copy(skipCollected=v)}})
    add(GuiKit.toggleRow(w,"Rainbow Lines",r.rainbow) {v->route {it.copy(rainbow=v)}})
    add(GuiKit.toggleRow(w,"Route Progress HUD",r.progress) {v->route {it.copy(progress=v)}})
    add(HudNavigation.button(w))
   }
   add(GuiKit.description(w,"Editor settings — right click to add/remove; Shift + right click to name a waypoint."))
   add(GuiKit.toggleRow(w,"Editor Mode",s.allowEdits) {v->update {it.copy(allowEdits=v)}})
   add(p.inputRow(w,s.editorKey.device,s.editorKey.key) {device,key->update {it.copy(editorKey=CopyBinding(device,key))}})
   add(GuiKit.colorRow(w,"Waypoint Color",s.color) {v->update {it.copy(color=v)}})
   add(GuiKit.description(w,"Color presets: left click applies; right click saves the current color."))
   add(GuiKit.row(w).apply {for(i in 1..4) child(GuiKit.presetButton("$i",45,when(i) {1->s.preset1;2->s.preset2;3->s.preset3;else->s.preset4}) {direction->
    if(direction<0) update {v->when(i) {1->v.copy(preset1=v.color);2->v.copy(preset2=v.color);3->v.copy(preset3=v.color);else->v.copy(preset4=v.color)}}
    else update {v->v.copy(color=when(i) {1->v.preset1;2->v.preset2;3->v.preset3;else->v.preset4})}
    refresh()
   })})
   add(GuiKit.toggleRow(w,"Filled",s.filled) {v->update {it.copy(filled=v)}})
   add(GuiKit.toggleRow(w,"Depth Test",s.depth) {v->update {it.copy(depth=v)}})
   add(GuiKit.toggleRow(w,"Use Block Size",s.blockSize) {v->update {it.copy(blockSize=v)};refresh()})
   if(!s.blockSize) {
    add(GuiKit.sliderRow(w,"Size X",s.sizeX,NumberRange(.1,5.0,.05)) {v->update {it.copy(sizeX=v)}})
    add(GuiKit.sliderRow(w,"Size Y",s.sizeY,NumberRange(.1,5.0,.05)) {v->update {it.copy(sizeY=v)}})
    add(GuiKit.sliderRow(w,"Size Z",s.sizeZ,NumberRange(.1,5.0,.05)) {v->update {it.copy(sizeZ=v)}})
   }
   val modes=listOf("static","gradient","pulse","fog")
   add(GuiKit.choiceRow(w,"Effect",modes,s.styleMode.let {modes.indexOf(it).coerceAtLeast(0)}) {i->update {it.copy(styleMode=modes[i])};refresh()})
   if(s.styleMode!="static") {
    add(GuiKit.colorRow(w,"Effect Color",s.styleColor) {v->update {it.copy(styleColor=v)}})
    add(GuiKit.sliderRow(w,"Effect Speed",s.styleSpeed,NumberRange(0.0,5.0,.1)) {v->update {it.copy(styleSpeed=v)}})
    add(GuiKit.sliderRow(w,"Effect Intensity",s.styleIntensity,NumberRange(0.0,1.0,.05)) {v->update {it.copy(styleIntensity=v)}})
   }
   add(GuiKit.description(w,"Effects animate interpolated vertex colors, not the held-item pixel shader. Existing pack waypoints keep their original style."))
   add(GuiKit.textRow(w,"Import / export pack name",transferName) {transferName=it})
   add(GuiKit.row(w).apply {child(GuiKit.button("Import clipboard",125) {act {DungeonWaypoints.import(transferName,Minecraft.getInstance().keyboardHandler.clipboard)};refresh()});child(GuiKit.button("Export",65) {act {Minecraft.getInstance().keyboardHandler.clipboard=DungeonWaypoints.export(transferName)}})})
   add(GuiKit.row(w).apply {child(GuiKit.button("Reload packs",100) {act {DungeonWaypoints.reload()};refresh()});child(GuiKit.button("Reset collected",115) {act {DungeonWaypoints.resetClicked()}})})
   add(GuiKit.description(w,"Current area: ${FeatureRuntime.context.area.ifBlank {"not recognized"}}"))
   runCatching {DungeonWaypoints.roomRows()}.getOrDefault(emptyList()).forEachIndexed {i,way->add(GuiKit.row(w).apply {child(GuiKit.label("${i+1}: ${way.blockPos} ${way.title}").apply {horizontalSizing(io.wispforest.owo.ui.core.Sizing.fixed(w-100))});child(GuiKit.button("Delete",60) {act {DungeonWaypoints.remove(i)};refresh()})})}
  }}
  host.showPanel(panel)
 }
}
