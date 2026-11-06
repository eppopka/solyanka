/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.client.feature.DungeonWaypoints
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object RouteUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Waypoint Route",{WaypointUi.open(host,model)},{model.error}) {p,w->buildList {
   val s=model.general.features.route
   fun update(block:(RouteSettings)->RouteSettings){model.updateGeneral {it.copy(features=it.features.copy(route=block(it.features.route)))};p.refreshStatus()}
   add(GuiKit.description(w,"Connects numbered waypoint titles (1, 2, 3 or 2 / 4) in order. Any waypoint matching Start Color (RGB, ignoring transparency) is placed before step 1. Reaching a step within Activation Radius advances the visible route. Repeated positions are separate steps. World, room and pack changes reset progress."))
   val way=model.general.features.waypoints
   add(GuiKit.toggleRow(w,"Add Lines",s.lines || way.lines){v->model.updateGeneral {it.copy(features=it.features.copy(route=it.features.route.copy(lines=v),waypoints=it.features.waypoints.copy(lines=v)))};p.refreshStatus()});add(GuiKit.colorRow(w,"Line Color",s.color){v->update{it.copy(color=v)}})
   add(GuiKit.colorRow(w,"Start Color",s.startColor){v->update{it.copy(startColor=v)};if(model.error==null) DungeonWaypoints.resetRoute()})
   add(GuiKit.sliderRow(w,"Line Width",if(way.lines) way.lineSize else s.width,NumberRange(1.0,6.0,.5)){v->model.updateGeneral {it.copy(features=it.features.copy(route=it.features.route.copy(width=v),waypoints=it.features.waypoints.copy(lineSize=v)))};p.refreshStatus()});add(GuiKit.toggleRow(w,"Rainbow Line",s.rainbow){v->update{it.copy(rainbow=v)}})
   add(GuiKit.toggleRow(w,"Through Walls",s.throughWalls){v->update{it.copy(throughWalls=v)}});add(GuiKit.toggleRow(w,"Skip Collected",s.skipCollected){v->update{it.copy(skipCollected=v)}});add(GuiKit.toggleRow(w,"Follow Progress",s.follow){v->update{it.copy(follow=v)}})
   add(GuiKit.sliderRow(w,"Activation Radius",s.radius,NumberRange(1.0,15.0,.5)){v->update{it.copy(radius=v)}});add(GuiKit.sliderRow(w,"Segments Ahead",s.ahead.toDouble(),NumberRange(1.0,20.0,1.0)){v->update{it.copy(ahead=v.toInt())}});add(GuiKit.toggleRow(w,"Route Progress HUD",s.progress){v->update{it.copy(progress=v)}})
   add(HudNavigation.button(w))
  }})
 }
}
