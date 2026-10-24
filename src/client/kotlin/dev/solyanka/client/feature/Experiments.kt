/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills ExperimentSolver/SlotOptions and RecipeData adapters; see NOTICE. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*

object Experiments {
 val logic=ExperimentLogic()
 private var menu:AbstractContainerMenu?=null
 private var previous=emptyMap<Int,ExperimentCard>()
 private val remembered=mutableMapOf<Int,ItemStack>()
 fun reset() { logic.reset();menu=null;previous=emptyMap();remembered.clear() }
 private fun card(s:ItemStack)=ExperimentCard(BuiltInRegistries.ITEM.getKey(s.item).toString(),ChatLogic.plain(s.hoverName.string),s.count,
  s.get(DataComponents.PROFILE)?.toString() ?: "",s.get(DataComponents.LORE)?.lines()?.map { ChatLogic.plain(it.string) } ?: emptyList(),
  s.item is DyeItem || s.item in setOf(Items.INK_SAC,Items.BONE_MEAL,Items.LAPIS_LAZULI,Items.COCOA_BEANS))
 fun update() {
  val client=Minecraft.getInstance();val screen=client.gui.screen() as? ContainerScreen
  if(screen==null) { if(menu!=null) reset();return }
  if(!SolyankaClient.isEnabled(SolyankaClient.EXPERIMENTS)) { if(menu!=null) reset();return }
  val cfg=SolyankaClient.currentGeneral().features.experiments
  val type=ExperimentLogic.type(ChatLogic.plain(screen.title.string),FeatureRuntime.context.skyblock && FeatureRuntime.context.area in setOf("Private Island","Your Island"))
  val active=when(type) { ExperimentType.CHRONO->cfg.chronomatron;ExperimentType.ULTRA->cfg.ultrasequencer;ExperimentType.SUPER->cfg.superpairs;else->false }
  if(!active) { if(menu!=null) reset();return }
  if(menu!==screen.menu || logic.type!=type) { reset();menu=screen.menu;logic.reset(type) }
  val slots=screen.menu.slots.filter { it.container!==client.player?.inventory }
  val snapshot=slots.associate { it.index to card(it.item) }
  // Process board changes before clock/glowstone to retain the last visible numbered board.
  slots.sortedBy { if(snapshot.getValue(it.index).status) 1 else 0 }.forEach { slot ->
   val current=snapshot.getValue(slot.index)
   if(previous[slot.index]!=current) {
    logic.update(slot.index,current,snapshot)
    if(logic.type==ExperimentType.SUPER && slot.index in logic.rewards && !current.empty && !current.glass && !current.status && !current.powerup) remembered[slot.index]=slot.item.copy()
   }
  }
  previous=snapshot
 }
 fun blocked(slot:Slot?,action:ContainerInput):Boolean {
  update()
  if(slot==null || menu==null || slot.container===Minecraft.getInstance().player?.inventory) return false
  return SolyankaClient.currentGeneral().features.experiments.blockWrongClicks && logic.blocked(slot.index)
 }
 fun click(slot:Slot?,action:ContainerInput) {
  if(slot==null || action !in setOf(ContainerInput.PICKUP,ContainerInput.CLONE)) return
  PriceTooltips.purchase(slot)
  if(menu!=null) logic.click(slot.index)
 }
 fun overlay(g:GuiGraphicsExtractor,slot:Slot,x:Int,y:Int) {
  if(menu==null || slot.index !in previous) return
  val id=slot.index;val first=id in logic.next();val second=id in logic.second()
  val color=logic.colors[id] ?: if(!logic.remember && first) 0x7000ff00 else if(!logic.remember && second) 0x70ffff00 else null
  if(logic.type==ExperimentType.SUPER) remembered[id]?.let { g.fill(x,y,x+16,y+16,0xff303030.toInt());g.item(it,x,y) }
  color?.let { g.fill(x,y,x+16,y+16,it) }
  if(!logic.remember && (first || second)) g.text(Minecraft.getInstance().font,if(first) "1" else "2",x,y,-1,true)
 }
 fun hideTooltip(slot:Slot?,title:String)=Suppression.on { emptyTooltips } && !title.startsWith("Ultrasequencer (") && slot!=null && ChatLogic.plain(slot.item.hoverName.string).isBlank()
}
