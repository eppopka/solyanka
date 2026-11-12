/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.*
import dev.solyanka.utility.*

object ExtraHudUi {
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        val key=when(id) {ModuleIds.INVENTORY_HUD->"inventoryHud";ModuleIds.ITEM_NAME_HUD->"itemNameHud";else->"skillHud"}
        val title=when(key) {"inventoryHud"->"Inventory HUD";"itemNameHud"->"Item Name HUD";else->"Skill Progress HUD"}
        var error:String?=null
        host.showPanel(SettingsPanel(title,{host.closePanel()},{error ?: model.error}) {panel,w->
            val section=FeatureFields.fields(model.general.features,key)
            fun change(edit:(FeatureSettings)->FeatureSettings) {
                try {val next=edit(model.general.features);next.validate();model.updateGeneral {it.copy(features=next)};error=null}
                catch(e:Exception) {error=e.message ?: "Invalid settings"}
                panel.refreshStatus()
            }
            buildList {
                add(GuiKit.description(w,when(key) {
                    "inventoryHud"->"27 inventory slots with optional hotbar, armor/offhand and filled-slot count. Display only; no inventory interaction. /solyanka gui moves/scales this HUD."
                    "itemNameHud"->"Persistent styled main-hand item name, hidden when hand is empty. Vanilla fading name can be hidden independently. /solyanka gui edits layout."
                    else->"SkyBlock action-bar skill gains: ratio, percentage, MAXED and tab/Skills-menu level hints. No API calls. Unknown absolute XP stays a percentage; no fabricated live data. Timeout 0 keeps the latest update until reset."
                }))
                val visible=section.first {it.name=="hud.enabled"} as FeatureField.Flag
                val textColor=section.first {it.name=="hud.color"} as FeatureField.Integer
                add(GuiKit.toggleRow(w,"HUD Visible",visible.value) {v->change {visible.set(it,v)}})
                add(HudNavigation.button(w))
                add(GuiKit.colorRow(w,"Text color",textColor.value) {v->change {textColor.set(it,v)}})
                section.filterNot {it.name.startsWith("hud.")}.forEach {field->
                    val label=field.name.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar {it.uppercase()}
                    when(field) {
                        is FeatureField.Flag -> add(GuiKit.toggleRow(w,label,field.value) {v->change {field.set(it,v)}})
                        is FeatureField.Integer -> if(field.name=="backgroundColor") add(GuiKit.colorRow(w,label,field.value) {v->change {field.set(it,v)}})
                            else add(GuiKit.sliderRow(w,label,field.value.toDouble(),NumberRange(0.0,120.0,1.0)) {v->change {field.set(it,v.toInt())}})
                        is FeatureField.Decimal -> error("Unexpected decimal field: ${field.name}")
                    }
                }
                addAll(HudGlowUi.controls(id,model,panel,w))
            }
        })
    }
}
