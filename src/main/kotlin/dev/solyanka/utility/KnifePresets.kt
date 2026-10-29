/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
/** Named reusable appearances, independent of the active item UUID. */
data class KnifePreset(val name:String="Preset",val knife:String="weapon_knife_karambit",val finish:String="",val seed:Int=0,val wear:Double=.05) {
 fun choice()=KnifeChoice(knife,finish,seed,wear)
}
object KnifePresets {
 const val LIMIT=64
 fun name(value:String)=value.trim().replace(Regex("\\s+")," ")
 fun validate(values:List<KnifePreset>) {
  require(values.size<=LIMIT) {"Maximum 64 knife presets"}
  val names=hashSetOf<String>()
  values.forEach {p->require(p.name.length in 1..48 && p.name==name(p.name) && p.name.none {it.isISOControl()}) {"Preset name must be 1–48 printable characters"};require(names.add(p.name.lowercase(java.util.Locale.ROOT))) {"Duplicate preset name"};p.choice().validate()}
 }
 fun save(values:List<KnifePreset>,label:String,choice:KnifeChoice):List<KnifePreset> {
  val preset=KnifePreset(name(label),choice.knife,choice.finish,choice.seed,choice.wear)
  val index=values.indexOfFirst {it.name.equals(preset.name,true)}
  val next=if(index<0)values+preset else values.toMutableList().apply {set(index,preset)}.toList()
  validate(next);return next
 }
}
