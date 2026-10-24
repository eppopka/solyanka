/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object SkyboxUi {
 fun open(host:NoFrillsClickGui,model:InstantSettings) {
  host.showPanel(SettingsPanel("Skybox",{host.closePanel()},{model.error}) {panel,w->
   val s=model.general.features.skybox
   fun update(block:(SkyboxSettings)->SkyboxSettings) {model.updateGeneral {it.copy(features=it.features.copy(skybox=block(it.features.skybox)))};panel.refreshStatus()}
   buildList {
    add(GuiKit.description(w,"Stray's six-face End sky and animated black-hole patch. Visual only, not a dimension change. Dimension Default allows tint without replacing the sky. Fog/blindness/fluid sky guards remain vanilla. Custom shader packs may replace this sky. Textures can use about 96 MiB raw GPU memory when all frames are loaded."))
    add(GuiKit.choiceRow(w,"Style",SkyboxLogic.labels,SkyboxLogic.styles.indexOf(s.style),readCurrent={SkyboxLogic.styles.indexOf(model.general.features.skybox.style)}) {i->update {it.copy(style=SkyboxLogic.styles[i])}})
    add(GuiKit.toggleRow(w,"Sky Tint",s.tint) {v->update {it.copy(tint=v)}})
    add(GuiKit.colorRow(w,"Tint (RGB)",s.color) {v->update {it.copy(color=v)}})
    add(GuiKit.sliderRow(w,"Tint Strength",s.strength,NumberRange(0.0,1.0,.05)) {v->update {it.copy(strength=v)}})
    add(GuiKit.sliderRow(w,"Rotation Speed",s.rotationSpeed,NumberRange(0.0,4.0,.05),"0 fixes rotation at the starting orientation.") {v->update {it.copy(rotationSpeed=v)}})
    add(GuiKit.sliderRow(w,"Black Hole Speed",s.animationSpeed,NumberRange(0.0,4.0,.05),"0 shows the first animation frame.") {v->update {it.copy(animationSpeed=v)}})
    add(GuiKit.toggleRow(w,"Skyless Dimensions",s.skylessDimensions,"Opt in to forcing an End sky in dimensions normally without a sky.") {v->update {it.copy(skylessDimensions=v)}})
   }
  })
 }
}
