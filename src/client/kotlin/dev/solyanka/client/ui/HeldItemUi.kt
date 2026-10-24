/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object HeldItemUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Held Item Shader",{host.closePanel()},{model.error}) {panel,w->
   val s=model.general.features.heldItem
   fun update(block:(HeldItemSettings)->HeldItemSettings) {model.updateGeneral {it.copy(features=it.features.copy(heldItem=block(it.features.heldItem)))};panel.refreshStatus()}
   buildList {
    add(GuiKit.description(w,"Stray Smoke, Stars, Portal and Galaxy shaders for first-person item quads. Item Name HUD is separate. Vanilla glint remains. Does not replace arms, maps or special model renderers; silhouette post-outline is not ported. No server/item changes."))
    add(GuiKit.choiceRow(w,"Effect",HeldItemPolicy.labels,HeldItemPolicy.styles.indexOf(s.style),readCurrent={HeldItemPolicy.styles.indexOf(model.general.features.heldItem.style)}) {i->update {it.copy(style=HeldItemPolicy.styles[i])}})
    add(GuiKit.colorRow(w,"Tint (RGB)",s.color) {v->update {it.copy(color=v)}})
    add(GuiKit.sliderRow(w,"Fill",s.fill,NumberRange(.08,.85,.01),"Tint strength; Portal/Galaxy also use this for transparency.") {v->update {it.copy(fill=v)}})
    add(GuiKit.sliderRow(w,"Intensity",s.intensity,NumberRange(.1,1.5,.05)) {v->update {it.copy(intensity=v)}})
    add(GuiKit.sliderRow(w,"Animation Speed",s.speed,NumberRange(0.0,4.0,.05),"0 freezes shader time; visual only.") {v->update {it.copy(speed=v)}})
    add(GuiKit.toggleRow(w,"Right Hand",s.rightHand) {v->update {it.copy(rightHand=v)}})
    add(GuiKit.toggleRow(w,"Left Hand",s.leftHand) {v->update {it.copy(leftHand=v)}})
   }
  })
 }
}
