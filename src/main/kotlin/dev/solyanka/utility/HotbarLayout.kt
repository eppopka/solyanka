/* SPDX-License-Identifier: GPL-3.0-only; impl.zip MoveHotbar appearance/geometry adaptation. */
package dev.solyanka.utility
import kotlin.math.*
data class MoveHotbarSettings(val customPosition:Boolean=false,val hud:HudSpec=HudSpec(x=389,y=518),val moveItemName:Boolean=true,val moveInfoBar:Boolean=false,val moveXpLevel:Boolean=false,
 val style:String="vanilla",val vertical:Boolean=false,val slotGap:Int=2,val padding:Int=3,val cornerRadius:Double=6.0,val borderWidth:Double=1.0,
 val backgroundColor:Int=0xff0c1119.toInt(),val backgroundOpacity:Double=.7,val gradient:Boolean=true,val gradientColor:Int=0xff1a2230.toInt(),val gradientAcross:Boolean=false,
 val accentColor:Int=0xff5e9fe8.toInt(),val selectionOpacity:Double=.22,val selectionRadius:Double=4.0,val smoothSelection:Boolean=true,val slideSpeed:Double=16.0,
 val shadow:Boolean=true,val shadowStrength:Double=.65,val glow:Boolean=true,val glowStrength:Double=.45,val effectRadius:Int=6,val shimmer:Boolean=false,val rainbowAccent:Boolean=false,val effectSpeed:Double=1.0,
 val itemScale:Double=1.0,val pickupAnimation:Boolean=true,val showOffhand:Boolean=true,val keepOnScreen:Boolean=true) {
 fun validate(){hud.validate();require(style in listOf("vanilla","minimal","glass","neon"));require(slotGap in 0..10 && padding in 0..12 && effectRadius in 1..12)
  fun n(v:Double,a:Double,b:Double){require(v.isFinite()&&v in a..b)}
  n(cornerRadius,0.0,16.0);n(borderWidth,0.0,3.0);n(backgroundOpacity,0.0,1.0);n(selectionOpacity,0.0,1.0);n(selectionRadius,0.0,12.0);n(slideSpeed,4.0,40.0);n(shadowStrength,0.0,1.0);n(glowStrength,0.0,1.0);n(effectSpeed,.15,3.0);n(itemScale,.5,1.5)
 }
}
data class HotbarCell(val x:Int,val y:Int,val width:Int,val height:Int){val centerX get()=x+width/2f;val centerY get()=y+height/2f}
data class StyledHotbarLayout(val width:Int,val height:Int,val main:HotbarCell,val slots:List<HotbarCell>,val offhand:HotbarCell?,val attack:HotbarCell?,val step:Int,val vertical:Boolean){
 fun selection(index:Double):HotbarCell {val i=index.coerceIn(0.0,8.0);return slots.first().let {it.copy(x=it.x+if(vertical)0 else (i*step).roundToInt(),y=it.y+if(vertical)(i*step).roundToInt()else 0)}}
}
class HotbarSelection {
 private var value=Double.NaN
 fun update(slot:Int,elapsed:Double,speed:Double,smooth:Boolean):Double {val target=slot.coerceIn(0,8).toDouble();if(!value.isFinite()||!smooth||!elapsed.isFinite()||elapsed<0||elapsed>.5)value=target else value+=(target-value)*(1-exp(-elapsed*speed.coerceIn(4.0,40.0)));return value}
}
object HotbarLayout {
 const val WIDTH=182;const val HEIGHT=22
 fun geometry(s:MoveHotbarSettings,offhand:Boolean=false,offhandBefore:Boolean=true,attack:Boolean=false):StyledHotbarLayout {
  val slot=if(s.style=="vanilla")20 else 22;val pad=if(s.style=="vanilla")1 else s.padding;val gap=if(s.style=="vanilla")0 else s.slotGap;val step=slot+gap;val across=slot+pad*2;val length=slot*9+gap*8+pad*2
  val extra=across+maxOf(4,gap);val before=(if(offhand&&offhandBefore)extra else 0)+(if(attack&&!offhandBefore)extra else 0);val total=length+(if(offhand)extra else 0)+(if(attack)extra else 0)
  fun box(along:Int,size:Int=across)=if(s.vertical)HotbarCell(0,along,across,size)else HotbarCell(along,0,size,across)
  val main=box(before,length)
  val slots=(0..8).map {i->if(s.vertical)HotbarCell(pad,before+pad+i*step,slot,slot)else HotbarCell(before+pad+i*step,pad,slot,slot)}
  val off=if(offhand)box(if(offhandBefore)0 else before+length+maxOf(4,gap))else null
  val atk=if(attack)box(if(!offhandBefore)0 else before+length+maxOf(4,gap))else null
  return StyledHotbarLayout(if(s.vertical)across else total,if(s.vertical)total else across,main,slots,off,atk,step,s.vertical)
 }
 fun anchor(s:MoveHotbarSettings,w:Int,h:Int,g:StyledHotbarLayout):HudSpec {
  val scale=s.hud.scale;val bw=(g.width*scale).roundToInt();val bh=(g.height*scale).roundToInt()
  val at=if(s.customPosition)s.hud else s.hud.copy(x=((w-bw)/2).coerceAtLeast(0),y=(h-bh).coerceAtLeast(0))
  return if(s.keepOnScreen)at.copy(x=at.x.coerceIn(0,(w-bw).coerceAtLeast(0)),y=at.y.coerceIn(0,(h-bh).coerceAtLeast(0)))else at
 }
 fun preset(s:MoveHotbarSettings,style:String):MoveHotbarSettings=when(style) {
  "minimal"->s.copy(style=style,gradient=false,backgroundOpacity=.45,cornerRadius=2.0,borderWidth=0.0,shadow=false,glow=false)
  "glass"->s.copy(style=style,gradient=true,backgroundOpacity=.7,cornerRadius=6.0,borderWidth=1.0,shadow=true,glow=false)
  "neon"->s.copy(style=style,gradient=false,backgroundOpacity=.85,cornerRadius=4.0,borderWidth=1.5,shadow=true,glow=true)
  "vanilla"->s.copy(style=style)
  else->error("Unknown hotbar style")
 }
 fun editorHud(s:MoveHotbarSettings,w:Int,h:Int)=anchor(s,w,h,geometry(s))
 data class Transform(val x:Double,val y:Double,val scale:Double)
 fun transform(s:MoveHotbarSettings,w:Int,h:Int):Transform {
  val at=editorHud(s,w,h);val scale=s.hud.scale
  val x=at.x.coerceIn(0,(w-WIDTH*scale).toInt().coerceAtLeast(0));val y=at.y.coerceIn(0,(h-HEIGHT*scale).toInt().coerceAtLeast(0))
  return Transform(x-(w/2-91)*scale,y-(h-22)*scale,scale)
 }
}
