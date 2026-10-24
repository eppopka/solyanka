/* SPDX-License-Identifier: GPL-3.0-only
 * Stray player visuals, NametagRenderer and TitaniumTracker CC0 adaptations. */
package dev.solyanka.utility

import java.util.UUID
import kotlin.math.sqrt
import java.util.Locale

data class PlayerGlowSettings(val color:Int=0xff55ffff.toInt(),val range:Int=128,val throughWalls:Boolean=true,val realAccountsOnly:Boolean=true) {
    fun validate() {require(range in 16..256)}
}
data class NametagSettings(val color:Int=-1,val backgroundColor:Int=0xaa101010.toInt(),val range:Int=128,val scale:Double=1.0,val throughWalls:Boolean=true,val background:Boolean=true,val distance:Boolean=true,val health:Boolean=true,val self:Boolean=false,val realAccountsOnly:Boolean=true) {
    fun validate() {require(range in 16..256);require(scale.isFinite() && scale in .5..3.0)}
}
data class BlockOutlineSettings(val color:Int=0xff55ffff.toInt(),val fillColor:Int=0x4055ffff,val filled:Boolean=true,val throughWalls:Boolean=false,val width:Double=2.0) {
    fun validate() {require(width.isFinite() && width in 1.0..5.0)}
}
data class TitaniumSettings(val color:Int=0xff55ffff.toInt(),val fillColor:Int=0x3055ffff,val range:Int=48,val all:Boolean=false,val throughWalls:Boolean=true,val filled:Boolean=true,val distance:Boolean=true,val tracers:Boolean=false,val maxMarkers:Int=128) {
    fun validate() {require(range in 16..96);require(maxMarkers in 1..256)}
}
data class WorldVisualSettings(val playerGlow:PlayerGlowSettings=PlayerGlowSettings(),val nametags:NametagSettings=NametagSettings(),val blockOutline:BlockOutlineSettings=BlockOutlineSettings(),val titanium:TitaniumSettings=TitaniumSettings()) {
    fun validate() {playerGlow.validate();nametags.validate();blockOutline.validate();titanium.validate()}
}
object PlayerVisualPolicy {
    fun account(id:UUID,self:Boolean,realOnly:Boolean)=!realOnly || self || id.version()==4
    fun eligible(enabled:Boolean,self:Boolean,allowSelf:Boolean,firstPerson:Boolean,alive:Boolean,spectator:Boolean,invisible:Boolean,real:Boolean,distanceSquared:Double,range:Int):Boolean =
        enabled && alive && !spectator && !invisible && real && (!self || allowSelf && !firstPerson) && distanceSquared.isFinite() && distanceSquared>=0 && distanceSquared<=range.toDouble()*range
    fun glowColor(color:Int)=color or 0xff000000.toInt()
    fun tag(name:String,distanceSquared:Double,health:Double,maxHealth:Double,showDistance:Boolean,showHealth:Boolean):String = buildString {
        append(ChatLogic.plain(name.take(256)).replace(Regex("[\\r\\n]")," "))
        if(showHealth && health.isFinite() && maxHealth.isFinite() && maxHealth>0) append(String.format(Locale.US," %.0f/%.0f HP",health.coerceAtLeast(0.0),maxHealth))
        if(showDistance && distanceSquared.isFinite() && distanceSquared>=0) append(String.format(Locale.US," [%.0fm]",sqrt(distanceSquared)))
    }
}
