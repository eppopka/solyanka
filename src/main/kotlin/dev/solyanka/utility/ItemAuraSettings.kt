/* SPDX-License-Identifier: GPL-3.0-only; user impl.zip ItemAura adaptation. */
package dev.solyanka.utility

data class ItemAuraSettings(val style:Int=1,val shape:Int=3,val count:Int=20,val radius:Double=.32,val size:Double=.04,val speed:Double=1.0,val trail:Int=6,val ribbon:Boolean=false,val glow:Boolean=false,val quality:Int=12,val rainbow:Boolean=false,val hueSpread:Double=.35,val auraColor:Int=0xb478dcff.toInt(),val depth:Boolean=false,val offsetX:Double=.06,val offsetY:Double=.22,val offsetZ:Double=.06,val rotX:Double=0.0,val rotY:Double=0.0,val rotZ:Double=0.0,val stretch:Double=1.0,val spin:Double=0.0,val offHand:Boolean=false,val onlyWithItem:Boolean=true) {
 fun validate() {
  require(style in 1..5 && shape in 1..3 && count in 4..64 && trail in 0..16 && quality in 5..24)
  fun range(v:Double,a:Double,b:Double) {require(v.isFinite() && v in a..b)}
  range(radius,.05,1.5);range(size,.004,.2);range(speed,.1,4.0);range(hueSpread,0.0,1.0)
  listOf(offsetX,offsetY,offsetZ).forEach {range(it,-.6,.6)}
  listOf(rotX,rotY,rotZ,spin).forEach {range(it,-180.0,180.0)};range(stretch,.2,3.0)
 }
}

data class Cs2KnifeSettings(val source:String="auto",val path:String="",val knife:String="weapon_knife_karambit",val finish:String="",val seed:Int=0,val wear:Double=.05,val applyTo:String="weapons",val animations:Boolean=true,val inspectKey:Int=70,val thirdPerson:Boolean=true,val offsetX:Double=0.0,val offsetY:Double=0.0,val distance:Double=0.0,val bindings:List<KnifeItemBinding> = emptyList(),val presets:List<KnifePreset> = emptyList()) {
 fun validate() {KnifePresets.validate(presets);KnifeBindings.validate(bindings);require(source in listOf("auto","install","pack"));require(path.length<=4096 && knife.matches(Regex("[a-zA-Z0-9_]{1,128}")) && finish.matches(Regex("[a-zA-Z0-9_]{0,128}")))
  require(seed in 0..1000 && wear.isFinite() && wear in 0.0..1.0 && applyTo in listOf("weapons","held","empty") && inspectKey in -1..348)
  listOf(offsetX,offsetY,distance).forEach {require(it.isFinite() && it in -2.0..2.0)}
 }
}
