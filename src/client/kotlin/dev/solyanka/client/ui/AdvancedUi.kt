/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import com.google.gson.Gson
import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.SettingsCatalog
import dev.solyanka.ui.SettingsNumbers
import dev.solyanka.utility.FeatureCodec

/** Typed settings still validate through FeatureCodec; widgets edit only an immutable copy. */
object AdvancedUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
  val spec=SettingsCatalog.panels[id] ?: error("No advanced settings for $id")
  val key=spec.section
  val title=spec.title
  val errors=linkedMapOf<String,String>()
  host.showPanel(SettingsPanel(title,{host.closePanel()},{errors.values.firstOrNull() ?: model.error}) { panel,w ->
   val objectValue=Gson().toJsonTree(model.general.features).asJsonObject
   val section=objectValue.getAsJsonObject(key)
   fun change(field:String,value:com.google.gson.JsonPrimitive) {
    try {
     val root=Gson().toJsonTree(model.general.features).asJsonObject;root.getAsJsonObject(key).add(field,value)
     val next=FeatureCodec.decode(root);model.updateGeneral { it.copy(features=next) };errors.remove(field)
    } catch(e:Exception) { errors[field]=e.message ?: "Invalid value" }
    panel.refreshStatus()
   }
   buildList {
    add(GuiKit.description(w,when(id) {
     SolyankaClient.AMBIENCE->"Local time and weather overrides. Left click: next style; right click: previous."
     SolyankaClient.ASPECT->"Horizontal projection multiplier: 1 is vanilla. Range 0.3–3; not a width:height ratio."
     SolyankaClient.FOG->"Start/end/density are fractions from 0–1. Density controls fog opacity; palette alpha is ignored. Sodium uses native fog uniforms; Iris parity is not included."
     SolyankaClient.DOORS->"Door colors follow Magical Map data and available-key chat. Optionally announce key spawns locally."
     SolyankaClient.MAP_INFO->"Estimated score from tab/sidebar; '?' means incomplete data. Paul is a manual bonus. Use HUD Editor to move the display. Portal needs a recognized Blood room rotation."
     SolyankaClient.ICE->"Loaded-chunk room matching with rotation markers. Use optimized routes and choose whether outlines respect walls."
     SolyankaClient.BLAZE->"Blaze order and priority outlines. Connecting lines and depth testing apply only to Blaze."
     SolyankaClient.BEAMS->"Recognized-room lantern pairs. Depth testing applies to pair outlines; connecting beams remain visible through walls."
     SolyankaClient.MIMIC->"Baby-zombie death heuristic on F6/F7, not proof of a mimic. Party announcements require explicit opt-in."
     SolyankaClient.MIMIC_CHEST->"Highlights trapped chests in recognized rooms; not proof that every trapped chest is a mimic."
     SolyankaClient.PRICES->"Online pricing is opt-in: public item-data requests only, no player data or API key. Missing prices stay unavailable. Purchases and recipes are stored locally."
     SolyankaClient.EXPERIMENTS->"Private Island only. Next=green, following=yellow. Superpairs remembers revealed rewards. No automatic clicks."
     else->"Independent rendering filters, all off by default. Left click: next mode; right click: previous."
    }))
    if(id==SolyankaClient.MAP_INFO) add(HudNavigation.button(w))
    spec.fields.filterNot {id==SolyankaClient.MAP_INFO && it in setOf("x","y")}.forEach { field ->
     val value=section.get(field)
     val label=field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar { it.uppercase() }
     val choices=SettingsCatalog.choices[field]
     if(field in SettingsCatalog.colors) add(GuiKit.colorRow(w,label,value.asInt) { change(field,com.google.gson.JsonPrimitive(it)) })
     else if(choices!=null) add(GuiKit.choiceRow(w,label,choices,value.asInt) { change(field,com.google.gson.JsonPrimitive(it)) })
     else if(value.asJsonPrimitive.isBoolean) add(GuiKit.toggleRow(w,label,value.asBoolean) { change(field,com.google.gson.JsonPrimitive(it)) })
     else add(GuiKit.sliderRow(w,label,value.asDouble,SettingsNumbers.range(key,field,host.width,host.height,value.asDouble)) { input ->
      val default=Gson().toJsonTree(dev.solyanka.utility.FeatureSettings()).asJsonObject.getAsJsonObject(key)[field].asJsonPrimitive
      val n:Number=if(default.asNumber is Double || default.asNumber is Float) input else input.toInt()
      change(field,com.google.gson.JsonPrimitive(n))
     })
    }
   }
  })
 }
}
