/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import com.google.gson.*
import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*

object ExtraHudUi {
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        val key=when(id) {SolyankaClient.INVENTORY_HUD->"inventoryHud";SolyankaClient.ITEM_NAME_HUD->"itemNameHud";else->"skillHud"}
        val title=when(key) {"inventoryHud"->"Inventory HUD";"itemNameHud"->"Item Name HUD";else->"Skill Progress HUD"}
        var error:String?=null
        host.showPanel(SettingsPanel(title,{host.closePanel()},{error ?: model.error}) {panel,w->
            val section=Gson().toJsonTree(model.general.features).asJsonObject.getAsJsonObject(key)
            fun change(path:String,value:JsonPrimitive) {
                try {
                    val root=Gson().toJsonTree(model.general.features).asJsonObject
                    val target=root.getAsJsonObject(key)
                    if(path.startsWith("hud.")) target.getAsJsonObject("hud").add(path.substringAfter('.'),value) else target.add(path,value)
                    val next=FeatureCodec.decode(root);model.updateGeneral {it.copy(features=next)};error=null
                } catch(e:Exception) {error=e.message ?: "Invalid settings"}
                panel.refreshStatus()
            }
            buildList {
                add(GuiKit.description(w,when(key) {
                    "inventoryHud"->"27 inventory slots with optional hotbar, armor/offhand and filled-slot count. Display only; no inventory interaction. /solyanka gui moves/scales this HUD."
                    "itemNameHud"->"Persistent styled main-hand item name, hidden when hand is empty. Vanilla fading name can be hidden independently. /solyanka gui edits layout."
                    else->"SkyBlock action-bar skill gains: ratio, percentage, MAXED and tab/Skills-menu level hints. No API calls. Unknown absolute XP stays a percentage; no fabricated live data. Timeout 0 keeps the latest update until reset."
                }))
                val hud=section.getAsJsonObject("hud")
                add(GuiKit.toggleRow(w,"HUD Visible",hud["enabled"].asBoolean) {change("hud.enabled",JsonPrimitive(it))})
                for(field in listOf("x","y","scale")) {
                    val value=hud[field].asDouble
                    add(GuiKit.sliderRow(w,field.replaceFirstChar {it.uppercase()},value,SettingsNumbers.range("hud",field,host.width,host.height,value)) {
                        change("hud.$field",if(field=="scale") JsonPrimitive(it) else JsonPrimitive(it.toInt()))
                    })
                }
                add(GuiKit.colorRow(w,"Text color",hud["color"].asInt) {change("hud.color",JsonPrimitive(it))})
                section.entrySet().filter {it.key!="hud"}.forEach {(field,value)->
                    val label=field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar {it.uppercase()}
                    if(value.asJsonPrimitive.isBoolean) add(GuiKit.toggleRow(w,label,value.asBoolean) {change(field,JsonPrimitive(it))})
                    else if(field=="backgroundColor") add(GuiKit.colorRow(w,label,value.asInt) {change(field,JsonPrimitive(it))})
                    else add(GuiKit.sliderRow(w,label,value.asDouble,NumberRange(0.0,120.0,1.0)) {change(field,JsonPrimitive(it.toInt()))})
                }
            }
        })
    }
}
