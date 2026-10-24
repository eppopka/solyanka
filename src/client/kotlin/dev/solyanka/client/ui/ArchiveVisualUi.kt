/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*

object ArchiveVisualUi {
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        val title=when(id) {S.AMBIENT_PARTICLES->"Ambient Particles";S.ROTATING_ITEM->"Rotating Item";else->"Move Hearts"}
        host.showPanel(SettingsPanel(title,{host.closePanel()},{model.error}) {panel,w->
            fun update(block:(FeatureSettings)->FeatureSettings) {model.updateGeneral {it.copy(features=block(it.features))};panel.refreshStatus()}
            buildList {
                when(id) {
                    S.AMBIENT_PARTICLES->{val s=model.general.features.ambientParticles
                        add(GuiKit.description(w,"Archive client-only particles around the player; 1–10 spawn attempts per tick, paused with the game. Uses native particle settings and culling, so Reduced/Minimal particles may show fewer. No network traffic; existing particles expire naturally after disabling."))
                        add(GuiKit.choiceRow(w,"Particle",AmbientParticleLogic.labels,AmbientParticleLogic.types.indexOf(s.particle),readCurrent={AmbientParticleLogic.types.indexOf(model.general.features.ambientParticles.particle)}) {i->update {it.copy(ambientParticles=it.ambientParticles.copy(particle=AmbientParticleLogic.types[i]))}})
                        add(GuiKit.sliderRow(w,"Amount per Tick",s.amount.toDouble(),NumberRange(1.0,10.0,1.0)) {v->update {it.copy(ambientParticles=it.ambientParticles.copy(amount=v.toInt()))}})
                        add(GuiKit.sliderRow(w,"Radius",s.radius.toDouble(),NumberRange(4.0,32.0,2.0)) {v->update {it.copy(ambientParticles=it.ambientParticles.copy(radius=v.toInt()))}})
                    }
                    S.ROTATING_ITEM->{val s=model.general.features.rotatingItem
                        add(GuiKit.description(w,"Archive first-person model movement: Chaotic, Clockwise, Counterclockwise, Spin and Wave. Horizontal offset mirrors for the left hand. Maps and empty hands bypass the source transform hook. Item-use animations still run; test bows, crossbows, shields and the Held Item Shader together. Phase restarts on enable/world change."))
                        add(GuiKit.choiceRow(w,"Mode",RotatingItemLogic.labels,RotatingItemLogic.modes.indexOf(s.mode),readCurrent={RotatingItemLogic.modes.indexOf(model.general.features.rotatingItem.mode)}) {i->update {it.copy(rotatingItem=it.rotatingItem.copy(mode=RotatingItemLogic.modes[i]))}})
                        add(GuiKit.sliderRow(w,"Speed",s.speed.toDouble(),NumberRange(5.0,200.0,5.0)) {v->update {it.copy(rotatingItem=it.rotatingItem.copy(speed=v.toInt()))}})
                        add(GuiKit.sliderRow(w,"Amplitude",s.amplitude.toDouble(),NumberRange(5.0,100.0,5.0),"Chaotic and Wave modes only.") {v->update {it.copy(rotatingItem=it.rotatingItem.copy(amplitude=v.toInt()))}})
                        for(axis in listOf("X","Y","Z")) {
                            val value=when(axis) {"X"->s.offsetX;"Y"->s.offsetY;else->s.offsetZ}
                            add(GuiKit.sliderRow(w,"Offset $axis",value,NumberRange(-.5,.5,.01)) {v->update {f->val a=f.rotatingItem;f.copy(rotatingItem=when(axis) {"X"->a.copy(offsetX=v);"Y"->a.copy(offsetY=v);else->a.copy(offsetZ=v)})}})
                        }
                    }
                    else->{val s=model.general.features.moveHearts;val h=HeartLayout.editorHud(s,host.width,host.height)
                        add(GuiKit.description(w,"Move/scale the VANILLA health bar, including native absorption, poison and multirow behavior. Optional armor follows the same transform. Player Display and No Render hiding still apply. /solyanka gui drags the baseline box; extra health rows/armor can extend above it. Reset uses the native screen-relative position."))
                        add(GuiKit.toggleRow(w,"Visible",s.hud.enabled) {v->update {it.copy(moveHearts=it.moveHearts.copy(hud=it.moveHearts.hud.copy(enabled=v)))}})
                        add(GuiKit.toggleRow(w,"Move Armor Bar",s.moveArmor) {v->update {it.copy(moveHearts=it.moveHearts.copy(moveArmor=v))}})
                        add(HudNavigation.button(w))
                        add(GuiKit.button("Reset Position / Scale",w) {update {it.copy(moveHearts=MoveHeartsSettings(moveArmor=it.moveHearts.moveArmor,hud=HudSpec(x=0,y=0,enabled=it.moveHearts.hud.enabled)))}})
                    }
                }
            }
        })
    }
}
