/* SPDX-License-Identifier: GPL-3.0-only; archive MoveHotbar options. */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object HotbarUi {
 fun controls(w:Int,host:NoFrillsClickGui,model:InstantSettings,panel:SettingsPanel)=buildList<io.wispforest.owo.ui.core.UIComponent> {
  val s=model.general.features.moveHotbar
  fun change(block:(MoveHotbarSettings)->MoveHotbarSettings) {model.updateGeneral {it.copy(features=it.features.copy(moveHotbar=block(it.features.moveHotbar)))};panel.refreshStatus()}
  fun toggle(name:String,v:Boolean,block:(MoveHotbarSettings,Boolean)->MoveHotbarSettings){add(GuiKit.toggleRow(w,name,v){x->change {block(it,x)}})}
  fun n(name:String,v:Double,a:Double,b:Double,step:Double,block:(MoveHotbarSettings,Double)->MoveHotbarSettings){add(GuiKit.sliderRow(w,name,v,NumberRange(a,b,step)){x->change {block(it,x)}})}
  fun color(name:String,v:Int,block:(MoveHotbarSettings,Int)->MoveHotbarSettings){add(GuiKit.colorRow(w,name,v){x->change {block(it,x)}})}
  add(GuiKit.description(w,"Style changes preserve your colors and effects. Use Original Hotbar Defaults to restore the archive’s Glass setup. Archive-styled hotbar with native item icons, counts, durability and cooldowns. Glass uses the original translucent coating and sheen, not framebuffer blur. HUD Editor handles position/overall scale. Native item-name/XP followers stay horizontal. All four styles use the original archive painter."))
  add(GuiKit.button("Restore Hotbar",w){change {HotbarLayout.restore(it)};HuntingUi.open(dev.solyanka.client.SolyankaClient.MOVE_HOTBAR,host,model)})
  add(GuiKit.button("Use Original Hotbar Defaults",w){change {HotbarLayout.archiveDefaults(it)};HuntingUi.open(dev.solyanka.client.SolyankaClient.MOVE_HOTBAR,host,model)})
  add(HudNavigation.button(w))
  add(GuiKit.choiceRow(w,"Style",listOf("Vanilla","Minimal","Glass","Neon"),listOf("vanilla","minimal","glass","neon").indexOf(s.style),readCurrent={listOf("vanilla","minimal","glass","neon").indexOf(model.general.features.moveHotbar.style)}){i->change {it.copy(style=listOf("vanilla","minimal","glass","neon")[i])};HuntingUi.open(dev.solyanka.client.SolyankaClient.MOVE_HOTBAR,host,model)})
  toggle("Archive Reference Sizing",s.referenceSizing){a,v->a.copy(referenceSizing=v)}
  toggle("Vertical layout",s.vertical){a,v->a.copy(vertical=v)}
  n("Slot Spacing",s.slotGap.toDouble(),0.0,10.0,1.0){a,v->a.copy(slotGap=v.toInt())};n("Padding",s.padding.toDouble(),0.0,12.0,1.0){a,v->a.copy(padding=v.toInt())}
  n("Corner Radius",s.cornerRadius,0.0,16.0,.5){a,v->a.copy(cornerRadius=v)};n("Border Width",s.borderWidth,0.0,3.0,.5){a,v->a.copy(borderWidth=v)}
  color("Background",s.backgroundColor){a,v->a.copy(backgroundColor=v)};n("Background Opacity",s.backgroundOpacity,0.0,1.0,.01){a,v->a.copy(backgroundOpacity=v)}
  toggle("Gradient",s.gradient){a,v->a.copy(gradient=v)};color("Gradient Color",s.gradientColor){a,v->a.copy(gradientColor=v)};toggle("Gradient Across Hotbar",s.gradientAcross){a,v->a.copy(gradientAcross=v)}
  color("Accent",s.accentColor){a,v->a.copy(accentColor=v)};n("Selection Opacity",s.selectionOpacity,0.0,1.0,.01){a,v->a.copy(selectionOpacity=v)};n("Selection Radius",s.selectionRadius,0.0,12.0,.5){a,v->a.copy(selectionRadius=v)}
  toggle("Smooth Selection",s.smoothSelection){a,v->a.copy(smoothSelection=v)};n("Slide Speed",s.slideSpeed,4.0,40.0,1.0){a,v->a.copy(slideSpeed=v)}
  toggle("Shadow",s.shadow){a,v->a.copy(shadow=v)};n("Shadow Strength",s.shadowStrength,0.0,1.0,.05){a,v->a.copy(shadowStrength=v)}
  toggle("Glow",s.glow){a,v->a.copy(glow=v)};n("Glow Strength",s.glowStrength,0.0,1.0,.05){a,v->a.copy(glowStrength=v)};n("Effect Radius",s.effectRadius.toDouble(),1.0,12.0,1.0){a,v->a.copy(effectRadius=v.toInt())}
  toggle("Animated Shimmer",s.shimmer){a,v->a.copy(shimmer=v)};toggle("Rainbow Accent",s.rainbowAccent){a,v->a.copy(rainbowAccent=v)};n("Effect Speed",s.effectSpeed,.15,3.0,.05){a,v->a.copy(effectSpeed=v)}
  n("Item Icon Size",s.itemScale,.5,1.5,.05){a,v->a.copy(itemScale=v)};toggle("Pickup Animation",s.pickupAnimation){a,v->a.copy(pickupAnimation=v)};toggle("Show Offhand",s.showOffhand){a,v->a.copy(showOffhand=v)}
  toggle("Custom Position",s.customPosition){a,v->a.copy(customPosition=v)};toggle("Keep On Screen",s.keepOnScreen){a,v->a.copy(keepOnScreen=v)}
  toggle("Item Name Follows",s.moveItemName){a,v->a.copy(moveItemName=v)};toggle("XP Bar Follows",s.moveInfoBar){a,v->a.copy(moveInfoBar=v)};toggle("XP Level Follows",s.moveXpLevel){a,v->a.copy(moveXpLevel=v)}
  add(GuiKit.button("Reset Layout",w){change {HotbarLayout.restore(it)}})
 }
}
