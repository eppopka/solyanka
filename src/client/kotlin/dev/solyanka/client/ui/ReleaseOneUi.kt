/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object ReleaseOneUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
  val title=when(id) {S.MOTION_BLUR->"Motion Blur";S.STOPWATCH->"Stopwatch";else->"DVD Bounce"}
  host.showPanel(SettingsPanel(title,{host.closePanel()},{model.error}) {panel,w->
   fun update(block:(FeatureSettings)->FeatureSettings) {model.updateGeneral {it.copy(features=block(it.features))};panel.refreshStatus()}
   buildList {
    when(id) {
     S.MOTION_BLUR->{val s=model.general.features.motionBlur
      add(GuiKit.description(w,"Stray time-weighted Frame Blending uses a finite exposure window, monitor refresh and raw frames (not recursive output). Recommended for clean motion. Existing Mix/Max remain available and can leave trails. HUD stays sharp. History is lazy, depthless and capped at 96 MiB. Velocity/hybrid reprojection is not implemented."))
      add(GuiKit.choiceRow(w,"Algorithm",BlurPolicy.labels,BlurPolicy.modes.indexOf(s.mode),readCurrent={BlurPolicy.modes.indexOf(model.general.features.motionBlur.mode)}) {i->update {it.copy(motionBlur=it.motionBlur.copy(mode=BlurPolicy.modes[i]))}})
      add(GuiKit.sliderRow(w,"Strength",s.strength,NumberRange(0.0,2.0,.05),"0 disables blur and releases history.") {v->update {it.copy(motionBlur=it.motionBlur.copy(strength=v))}})
      add(GuiKit.toggleRow(w,"Pause in Menus",s.pauseInMenus,"Reseeds history after menus; avoids smearing settings over the world.") {v->update {it.copy(motionBlur=it.motionBlur.copy(pauseInMenus=v))}})
     }
     S.STOPWATCH->{val s=model.general.features.stopwatch
      add(GuiKit.description(w,"First press starts, second stops and reports locally in chat, third resets. World-only key/mouse binding, independent of Command Keybinds. No chat/server command sent. /solyanka gui moves/scales this HUD. Resets on disable or world change."))
      add(panel.inputRow(w,InputDevice.valueOf(s.device),s.key) {d,k->update {it.copy(stopwatch=it.stopwatch.copy(device=d.name,key=k))}})
      add(GuiKit.toggleRow(w,"HUD Visible",s.hud.enabled) {v->update {it.copy(stopwatch=it.stopwatch.copy(hud=it.stopwatch.hud.copy(enabled=v)))}})
      add(HudNavigation.button(w))
      add(GuiKit.colorRow(w,"Text Color",s.hud.color) {v->update {it.copy(stopwatch=it.stopwatch.copy(hud=it.stopwatch.hud.copy(color=v)))}})
     }
     else->{val s=model.general.features.dvd
      add(GuiKit.description(w,"Archive bouncing overlay with six-color wall bounces, simultaneous-axis corner counter, optional sound and flash. Motion pauses in blocking menus/F1. It moves itself, so it is not a static HUD-editor element. Native font/panel adaptation, not the archive custom font."))
      add(GuiKit.textRow(w,"Text",s.text) {v->update {it.copy(dvd=it.dvd.copy(text=v))}})
      add(GuiKit.sliderRow(w,"Box Width",s.width.toDouble(),NumberRange(48.0,240.0,2.0)) {v->update {it.copy(dvd=it.dvd.copy(width=v.toInt()))}})
      add(GuiKit.sliderRow(w,"Box Height",s.height.toDouble(),NumberRange(24.0,120.0,2.0)) {v->update {it.copy(dvd=it.dvd.copy(height=v.toInt()))}})
      add(GuiKit.sliderRow(w,"Corner Radius",s.radius.toDouble(),NumberRange(0.0,40.0,1.0)) {v->update {it.copy(dvd=it.dvd.copy(radius=v.toInt()))}})
      add(GuiKit.sliderRow(w,"Speed",s.speed,NumberRange(20.0,320.0,2.0)) {v->update {it.copy(dvd=it.dvd.copy(speed=v))}})
      add(GuiKit.toggleRow(w,"Corner Counter",s.counter) {v->update {it.copy(dvd=it.dvd.copy(counter=v))}})
      add(GuiKit.toggleRow(w,"Corner Sound",s.sound) {v->update {it.copy(dvd=it.dvd.copy(sound=v))}})
      add(GuiKit.toggleRow(w,"Corner Flash",s.flash) {v->update {it.copy(dvd=it.dvd.copy(flash=v))}})
     }
    }
   }
  })
 }
}
