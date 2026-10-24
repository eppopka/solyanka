/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import com.google.gson.*
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.FeatureCodec
object WorldVisualUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
  val key=when(id) {S.PLAYER_GLOW->"playerGlow";S.NAMETAGS->"nametags";S.BLOCK_OUTLINE->"blockOutline";else->"titanium"}
  val title=when(key) {"playerGlow"->"Player Glow";"nametags"->"Nametags";"blockOutline"->"Block Outline";else->"Titanium ESP"}
  var error:String?=null
  host.showPanel(SettingsPanel(title,{host.closePanel()},{error ?: model.error}) {panel,w->
   val section=Gson().toJsonTree(model.general.features.worldVisuals).asJsonObject.getAsJsonObject(key)
   fun change(field:String,value:JsonPrimitive) {
    try {val root=Gson().toJsonTree(model.general.features).asJsonObject;root.getAsJsonObject("worldVisuals").getAsJsonObject(key).add(field,value);val next=FeatureCodec.decode(root);model.updateGeneral {it.copy(features=next)};error=null}
    catch(e:Exception) {error=e.message ?: "Invalid setting"};panel.refreshStatus()
   }
   buildList {
    add(GuiKit.description(w,when(key) {
     "playerGlow"->"Native silhouette outlines, not the source blur shader. Real accounts filter uses UUID v4; disable for offline players. Existing server glow colors are preserved."
     "nametags"->"Rounded, bordered player plates with styled names, muted distance and source distance scaling. Optional health row. Real accounts filter skips most NPCs. Through Walls off uses visual line of sight. F1 hides custom labels."
     "blockOutline"->"Voxel contour/fill with a layered soft-edge halo. This is not the source framebuffer blur shader; use Glow off for the previous crisp contour."
     else->"Loaded blocks only, SkyBlock mining areas. Polished diorite marks titanium. All off follows unfinished commissions with approximate source regions; unknown areas fail closed. Scan: 32768 block visits/tick."
    }))
    section.entrySet().forEach {(field,value)->
     val label=if(field=="all") "Show All Titanium" else field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar {it.uppercase()}
     if(value.asJsonPrimitive.isBoolean) add(GuiKit.toggleRow(w,label,value.asBoolean) {change(field,JsonPrimitive(it))})
     else if(field.endsWith("Color") || field=="color") add(GuiKit.colorRow(w,label,value.asInt) {change(field,JsonPrimitive(it))})
     else {
      val range=when(field) {"scale"->NumberRange(.5,3.0,.05);"opacity"->NumberRange(.15,1.0,.05);"glowStrength"->NumberRange(.1,1.0,.05);"glowRadius"->NumberRange(1.0,8.0,.25);"width"->NumberRange(1.0,5.0,.25);"maxMarkers"->NumberRange(1.0,256.0,1.0);else->NumberRange(16.0,if(key=="titanium") 96.0 else 256.0,1.0)}
      add(GuiKit.sliderRow(w,label,value.asDouble,range) {change(field,if(field in setOf("scale","width","opacity","glowStrength","glowRadius")) JsonPrimitive(it) else JsonPrimitive(it.toInt()))})
     }
    }
   }
  })
 }
}
