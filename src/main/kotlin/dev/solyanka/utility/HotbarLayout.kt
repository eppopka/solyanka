/* SPDX-License-Identifier: GPL-3.0-only; native-preserving MoveHotbar adaptation. */
package dev.solyanka.utility

data class MoveHotbarSettings(val customPosition:Boolean=false,val hud:HudSpec=HudSpec(x=389,y=518),val moveItemName:Boolean=true,val moveInfoBar:Boolean=false,val moveXpLevel:Boolean=false) {fun validate()=hud.validate()}
object HotbarLayout {
 const val WIDTH=182;const val HEIGHT=22
 fun editorHud(s:MoveHotbarSettings,w:Int,h:Int):HudSpec {
  val width=(WIDTH*s.hud.scale).toInt();val height=(HEIGHT*s.hud.scale).toInt()
  return if(s.customPosition)s.hud else s.hud.copy(x=((w-width)/2).coerceAtLeast(0),y=(h-height).coerceAtLeast(0))
 }
 data class Transform(val x:Double,val y:Double,val scale:Double)
 fun transform(s:MoveHotbarSettings,w:Int,h:Int):Transform {
  val at=editorHud(s,w,h);val scale=s.hud.scale
  val x=at.x.coerceIn(0,(w-WIDTH*scale).toInt().coerceAtLeast(0));val y=at.y.coerceIn(0,(h-HEIGHT*scale).toInt().coerceAtLeast(0))
  return Transform(x-(w/2-91)*scale,y-(h-22)*scale,scale)
 }
}
