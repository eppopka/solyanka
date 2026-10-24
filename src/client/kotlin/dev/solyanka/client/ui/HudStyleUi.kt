/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import com.google.gson.*
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.FeatureCodec
object HudStyleUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
  val key=if(id==S.SCOREBOARD) "scoreboardHud" else "themedChat";var error:String?=null
  host.showPanel(SettingsPanel(if(id==S.SCOREBOARD) "Scoreboard" else "Themed Chat",{host.closePanel()},{error ?: model.error}) {panel,w->
   val section=Gson().toJsonTree(model.general.features).asJsonObject.getAsJsonObject(key)
   fun change(path:String,value:JsonPrimitive) {try {
    val root=Gson().toJsonTree(model.general.features).asJsonObject;val target=root.getAsJsonObject(key)
    if(path.startsWith("hud.")) target.getAsJsonObject("hud").add(path.substringAfter('.'),value) else target.add(path,value)
    val next=FeatureCodec.decode(root);model.updateGeneral {it.copy(features=next)};error=null
   } catch(e:Exception) {error=e.message ?: "Invalid setting"};panel.refreshStatus()}
   buildList {
    add(GuiKit.description(w,if(key=="scoreboardHud") "Rounded sidebar with team-colored objective selection and original styled names/scores. Move and scale with /solyanka gui. Long rows are clipped to avoid score overlap. No invented live data." else "One rounded pane plus styled input. Vanilla text, links, hover, scroll, suggestions and message-security tags remain intact. No frost blur or slide animation; click hitboxes stay unchanged."))
    fun controls(obj:JsonObject,prefix:String="") {
     obj.entrySet().forEach {(field,value)->
      if(value.isJsonObject) return@forEach
      val path=prefix+field;val label=field.replace(Regex("([a-z])([A-Z])"),"$1 $2").replaceFirstChar {it.uppercase()}
      if(value.asJsonPrimitive.isBoolean) add(GuiKit.toggleRow(w,label,value.asBoolean) {change(path,JsonPrimitive(it))})
      else if(field.endsWith("Color") || field=="color") add(GuiKit.colorRow(w,label,value.asInt) {change(path,JsonPrimitive(it))})
      else {
       val range=when(field) {"maxWidth"->NumberRange(100.0,400.0,1.0);"opacity"->NumberRange(.1,1.0,.05);else->SettingsNumbers.range("hud",field,host.width,host.height,value.asDouble)}
       add(GuiKit.sliderRow(w,label,value.asDouble,range) {change(path,if(field in setOf("scale","opacity")) JsonPrimitive(it) else JsonPrimitive(it.toInt()))})
      }
     }
    }
    section.getAsJsonObject("hud")?.let {controls(it,"hud.")};controls(section)
   }
  })
 }
}
