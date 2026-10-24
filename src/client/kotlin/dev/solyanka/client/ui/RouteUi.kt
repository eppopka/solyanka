/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object RouteUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Waypoint Route",{WaypointUi.open(host,model)},{model.error}) {p,w->buildList {
   val s=model.general.features.route
   fun update(block:(RouteSettings)->RouteSettings){model.updateGeneral {it.copy(features=it.features.copy(route=block(it.features.route)))};p.refreshStatus()}
   add(GuiKit.description(w,"Extends Dungeon Waypoints; uses imported numbered titles: 1, 1 item, 2 / 4. Untitled opaque red (#FF5555) is START. Repeated positions are separate steps; the cursor advances sequentially, not to a future repeat. World, room and pack changes reset progress. No route pack is bundled."))
   add(GuiKit.toggleRow(w,"Add Lines",s.lines){v->update{it.copy(lines=v)}});add(GuiKit.colorRow(w,"Line Color",s.color){v->update{it.copy(color=v)}})
   add(GuiKit.sliderRow(w,"Line Width",s.width,NumberRange(1.0,6.0,.5)){v->update{it.copy(width=v)}});add(GuiKit.toggleRow(w,"Rainbow Line",s.rainbow){v->update{it.copy(rainbow=v)}})
   add(GuiKit.toggleRow(w,"Through Walls",s.throughWalls){v->update{it.copy(throughWalls=v)}});add(GuiKit.toggleRow(w,"Skip Collected",s.skipCollected){v->update{it.copy(skipCollected=v)}});add(GuiKit.toggleRow(w,"Follow Progress",s.follow){v->update{it.copy(follow=v)}})
   add(GuiKit.sliderRow(w,"Step Radius",s.radius,NumberRange(1.0,15.0,.5)){v->update{it.copy(radius=v)}});add(GuiKit.sliderRow(w,"Segments Ahead",s.ahead.toDouble(),NumberRange(1.0,20.0,1.0)){v->update{it.copy(ahead=v.toInt())}});add(GuiKit.toggleRow(w,"Route Progress HUD",s.progress){v->update{it.copy(progress=v)}})
   add(HudNavigation.button(w))
  }})
 }
}
