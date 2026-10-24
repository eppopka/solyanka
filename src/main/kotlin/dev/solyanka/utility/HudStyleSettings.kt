/* SPDX-License-Identifier: GPL-3.0-only; Stray ScoreboardHudRenderer/ChatChrome CC0 adaptations. */
package dev.solyanka.utility
import kotlin.math.*
data class ScoreboardHudSettings(val hud:HudSpec=HudSpec(x=300,y=40),val backgroundColor:Int=0xd018202b.toInt(),val borderColor:Int=0x665f869f,val accentColor:Int=0xff76c9ec.toInt(),val background:Boolean=true,val header:Boolean=true,val scores:Boolean=true,val shadow:Boolean=false,val maxWidth:Int=220) {
 fun validate() {hud.validate();require(maxWidth in 100..400)}
}
data class ThemedChatSettings(val backgroundColor:Int=0x9918202b.toInt(),val borderColor:Int=0x8865a9c9.toInt(),val inputColor:Int=0xcc18202b.toInt(),val rounded:Boolean=true,val input:Boolean=true,val opacity:Double=1.0) {fun validate() {require(opacity.isFinite() && opacity in .1..1.0)}}
object VisualChrome {
 fun alpha(color:Int,factor:Double):Int {require(factor.isFinite());return (color and 0xffffff) or (((color ushr 24)*factor.coerceIn(0.0,1.0)).roundToInt() shl 24)}
 fun distanceScale(distance:Double):Double {require(distance.isFinite() && distance>=0);return (24.0/(12.0+distance)).coerceIn(.52,1.35)}
 fun inset(row:Int,height:Int,radius:Int):Int {
  val r=radius.coerceIn(0,height/2);if(r==0) return 0
  val dy=when {row<r->r-row-.5;row>=height-r->row-(height-r)+.5;else->0.0}
  return if(dy==0.0) 0 else ceil(r-sqrt((r.toDouble()*r-dy*dy).coerceAtLeast(0.0))).toInt()
 }
 fun halo(color:Int,width:Double,radius:Double,strength:Double):List<Pair<Float,Int>> = listOf(1.0 to .09,.7 to .15,.4 to .23).map {(r,a)->(width+radius*r*2).toFloat() to alpha(color,a*strength)}
}
object ScoreboardLayout {
 fun height(rows:Int,header:Boolean)=14+(if(header) 14 else 0)+rows.coerceIn(1,15)*10-2
 fun width(content:Int,maxWidth:Int)=content.plus(24).coerceIn(100,maxWidth.coerceIn(100,400))
}
data class ChatPane(val x:Int,val y:Int,val width:Int,val height:Int)
object ChatPaneLayout {
 fun visible(ages:List<Int>,focused:Boolean,page:Int,scroll:Int):Int=ages.drop(scroll.coerceAtLeast(0)).take(page.coerceIn(1,1000)).count {focused || it<200}
 fun pane(screenHeight:Int,chatWidth:Int,scale:Double,lineHeight:Int,rows:Int):ChatPane {
  require(scale.isFinite() && scale>0)
  val h=(rows.coerceIn(1,1000)*lineHeight.coerceIn(1,100)*scale).roundToInt()
  return ChatPane((4*scale-6).roundToInt().coerceAtLeast(0),(screenHeight-40-h-6).coerceAtLeast(0),(ceil(chatWidth/scale)*scale+12).roundToInt().coerceAtLeast(16),h+12)
 }
}
