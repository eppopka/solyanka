/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
/** Stable SkyBlock item identity only. Never match a display name, type or player UUID. */
data class KnifeItemBinding(val uuid:String="",val label:String="Item",val knife:String="weapon_knife_karambit",val finish:String="",val seed:Int=0,val wear:Double=.05,val enabled:Boolean=true) {
 fun choice()=KnifeChoice(knife,finish,seed,wear)
}
data class KnifeChoice(val knife:String,val finish:String,val seed:Int,val wear:Double) {
 fun key()="$knife|$finish|$seed|${java.lang.Math.round(wear*10000)}"
 fun validate() {require(knife.matches(Regex("[a-zA-Z0-9_]{1,128}")));require(finish.matches(Regex("[a-zA-Z0-9_]{0,128}")));require(seed in 0..1000 && wear.isFinite() && wear in 0.0..1.0)}
}
object KnifeBindings {
 @JvmStatic fun canonical(value:String?):String? {
  if(value==null || !(value.matches(Regex("[a-fA-F0-9]{32}")) || value.matches(Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"))))return null
  return value.replace("-","").lowercase(java.util.Locale.ROOT)
 }
 @JvmStatic fun binding(s:Cs2KnifeSettings,uuid:String?)=canonical(uuid)?.let {id->s.bindings.firstOrNull {it.enabled && canonical(it.uuid)==id}}
 @JvmStatic fun choice(s:Cs2KnifeSettings,uuid:String?):KnifeChoice=binding(s,uuid)?.choice() ?: KnifeChoice(s.knife,s.finish,s.seed,s.wear)
 fun validate(bindings:List<KnifeItemBinding>) {require(bindings.size<=128);val seen=hashSetOf<String>();bindings.forEach {b->require(seen.add(canonical(b.uuid) ?: error("Binding requires a valid item UUID"))) {"Duplicate item UUID"};require(b.label.length in 1..96);b.choice().validate()}}
}
