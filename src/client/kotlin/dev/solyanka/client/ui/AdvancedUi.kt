/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds
import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.SettingsCatalog
import dev.solyanka.model.SettingsNumbers
import dev.solyanka.utility.FeatureSettings
import dev.solyanka.model.FeatureField
import dev.solyanka.model.FeatureFields

/** Typed settings descriptors update immutable feature state without serialization. */
object AdvancedUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings,onBack:()->Unit={host.closePanel()}) {
  val spec=SettingsCatalog.panels[id] ?: error("No advanced settings for $id")
  val key=spec.section
  val title=spec.title
  val errors=linkedMapOf<String,String>()
  host.showPanel(SettingsPanel(title,onBack,{errors.values.firstOrNull() ?: model.error}) { panel,w ->
   val section=FeatureFields.fields(model.general.features,key).associateBy {it.name}
   fun change(field:String,edit:(FeatureSettings)->FeatureSettings) {
    try {val next=edit(model.general.features);next.validate();model.updateGeneral {it.copy(features=next)};errors.remove(field)}
    catch(e:Exception) {errors[field]=e.message ?: "Invalid value"}
    panel.refreshStatus()
   }
   buildList {
    add(GuiKit.description(w,when(id) {
     ModuleIds.AMBIENCE->"Local time and weather overrides. Ignore Weather Blocks lets precipitation pass through roofs, but does not enable rain or snow. Left click: next style; right click: previous."
     ModuleIds.ASPECT->"Horizontal projection multiplier: 1 is vanilla. Range 0.3–3; not a width:height ratio."
     ModuleIds.FOG->"Start/end/density are fractions from 0–1. Density controls fog opacity; palette alpha is ignored. Sodium uses native fog uniforms; Iris parity is not included."
     ModuleIds.DOORS->"Door colors follow Magical Map data and available-key chat. Optionally announce key spawns locally."
     ModuleIds.MAP_INFO->"Estimated score from tab/sidebar; '?' means incomplete data. Paul is a manual bonus. Use HUD Editor to move the display. Portal needs a recognized Blood room rotation."
     ModuleIds.ICE->"Loaded-chunk room matching with rotation markers. Use optimized routes and choose whether outlines respect walls."
     ModuleIds.BLAZE->"Blaze order and priority outlines. Connecting lines and depth testing apply only to Blaze."
     ModuleIds.BEAMS->"Recognized-room lantern pairs. Depth testing applies to pair outlines; connecting beams remain visible through walls."
     ModuleIds.MIMIC->"Baby-zombie death heuristic on F6/F7, not proof of a mimic. Party announcements require explicit opt-in."
     ModuleIds.MIMIC_CHEST->"Highlights trapped chests in recognized rooms; not proof that every trapped chest is a mimic."
     ModuleIds.PRICES->"Online pricing is opt-in: public item-data requests only, no player data or API key. Missing prices stay unavailable. Purchases and recipes are stored locally."
     ModuleIds.EXPERIMENTS->"Private Island only. Next=green, following=yellow. Superpairs remembers revealed rewards. No automatic clicks."
     else->"Independent rendering filters, all off by default. Left click: next mode; right click: previous."
    }))
    if(id==ModuleIds.MAP_INFO) add(HudNavigation.button(w))
    spec.fields.filterNot {id==ModuleIds.MAP_INFO && it in setOf("x","y")}.forEach { field ->
     val value=section.getValue(field)
     val label=field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar { it.uppercase() }
     val choices=SettingsCatalog.choices[field]
     when(value) {
      is FeatureField.Flag -> add(GuiKit.toggleRow(w,label,value.value) {v->change(field) {value.set(it,v)}})
      is FeatureField.Integer -> when {
       field in SettingsCatalog.colors -> add(GuiKit.colorRow(w,label,value.value) {v->change(field) {value.set(it,v)}})
       choices!=null -> add(GuiKit.choiceRow(w,label,choices,value.value) {v->change(field) {value.set(it,v)}})
       else -> add(GuiKit.sliderRow(w,label,value.value.toDouble(),SettingsNumbers.range(key,field,host.width,host.height,value.value.toDouble())) {v->change(field) {value.set(it,v.toInt())}})
      }
      is FeatureField.Decimal -> add(GuiKit.sliderRow(w,label,value.value,SettingsNumbers.range(key,field,host.width,host.height,value.value)) {v->change(field) {value.set(it,v)}})
     }
    }
    if(id==ModuleIds.MAP_INFO) addAll(HudGlowUi.controls(id,model,panel,w))
   }
  })
 }
}
