/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills ExperimentSolver; see NOTICE. */
package dev.solyanka.utility

data class ExperimentCard(val item:String="minecraft:air",val name:String="",val count:Int=0,val texture:String="",val lore:List<String> = emptyList(),val dye:Boolean=false) {
 val empty get()=count<=0 || item=="minecraft:air"
 val glass get()=item.endsWith("stained_glass") || item.endsWith("stained_glass_pane")
 val status get()=item in setOf("minecraft:clock","minecraft:bookshelf","minecraft:cauldron") || (item=="minecraft:glowstone" && name!="Enchanted Book")
 val powerup get()=lore.any { it.contains("powerup",true) }
 fun matches(other:ExperimentCard)=item==other.item && name==other.name && count==other.count && texture==other.texture
}
enum class ExperimentType { NONE,CHRONO,ULTRA,SUPER }
class ExperimentLogic {
 var type=ExperimentType.NONE;private set
 var remember=true;private set
 private val chrono=mutableListOf<MutableSet<Int>>()
 private val ultra=mutableListOf<Pair<Int,Int>>()
 val rewards=linkedMapOf<Int,ExperimentCard>()
 val colors=mutableMapOf<Int,Int>()
 private var previous:Int?=null
 fun reset(next:ExperimentType=ExperimentType.NONE) { type=next;remember=true;chrono.clear();ultra.clear();rewards.clear();colors.clear();previous=null }
 fun update(slot:Int,card:ExperimentCard,snapshot:Map<Int,ExperimentCard>) {
  if(type==ExperimentType.NONE) return
  val was=remember
  if(card.item=="minecraft:glowstone" && card.status) remember=true
  if(card.item=="minecraft:clock") remember=false
  when(type) {
   ExperimentType.CHRONO -> if(remember) {
    if(!was) chrono.clear()
    if(card.item.endsWith("terracotta")) { if(chrono.isEmpty()) chrono.add(linkedSetOf());chrono.last().add(slot) }
    else if(card.item.endsWith("stained_glass") && chrono.lastOrNull()?.contains(slot)==true) chrono.add(linkedSetOf())
   }
   ExperimentType.ULTRA -> if(remember) {
    // Capture all visible numbered dyes before they disappear; preserve slots as updates arrive.
    if(!was) ultra.clear()
    snapshot.filterValues { it.dye && !it.empty }.forEach { (id,c) -> ultra.removeAll { it.first==id };ultra.add(id to c.count) }
   } else ultra.sortBy { it.second }
   ExperimentType.SUPER -> if(!card.empty && !card.status && !card.glass) {
    if(card.powerup) colors[slot]=0x70ff00ff
    else {
     rewards.forEach { (id,known) -> if(id!=slot && known.matches(card)) { val color=if(previous==id && snapshot[id]?.matches(card)==true) 0x7000ff00 else 0x70ffff00;colors[id]=color;colors[slot]=color } }
     rewards[slot]=card;previous=slot
    }
   }
   else -> Unit
  }
 }
 fun next():Set<Int> = when(type) { ExperimentType.CHRONO -> chrono.firstOrNull { it.isNotEmpty() } ?: emptySet();ExperimentType.ULTRA->ultra.firstOrNull()?.let { setOf(it.first) } ?: emptySet();else->emptySet() }
 fun second():Set<Int> = when(type) { ExperimentType.CHRONO->chrono.filter { it.isNotEmpty() }.getOrNull(1)?.minus(next()) ?: emptySet();ExperimentType.ULTRA->ultra.getOrNull(1)?.let { setOf(it.first) } ?: emptySet();else->emptySet() }
 fun blocked(slot:Int)=type in setOf(ExperimentType.CHRONO,ExperimentType.ULTRA) && (remember || slot !in next())
 fun click(slot:Int) { if(remember || slot !in next()) return;when(type) { ExperimentType.CHRONO->{ while(chrono.firstOrNull()?.isEmpty()==true) chrono.removeAt(0);if(chrono.isNotEmpty()) chrono.removeAt(0) };ExperimentType.ULTRA->if(ultra.isNotEmpty()) ultra.removeAt(0);else->Unit } }
 companion object { fun type(title:String,privateIsland:Boolean)=if(!privateIsland) ExperimentType.NONE else when { title.startsWith("Chronomatron (")->ExperimentType.CHRONO;title.startsWith("Ultrasequencer (")->ExperimentType.ULTRA;title.startsWith("Superpairs (")->ExperimentType.SUPER;else->ExperimentType.NONE } }
}
