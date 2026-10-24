/* SPDX-License-Identifier: GPL-3.0-only; impl.zip MoveHotbar appearance/geometry adaptation. */
package dev.solyanka.utility
import kotlin.math.*
data class MoveHotbarSettings(val customPosition:Boolean=false,val hud:HudSpec=HudSpec(x=389,y=518),val moveItemName:Boolean=true,val moveInfoBar:Boolean=false,val moveXpLevel:Boolean=false,
 val style:String="vanilla",val vertical:Boolean=false,val slotGap:Int=2,val padding:Int=3,val cornerRadius:Double=6.0,val borderWidth:Double=1.0,
 val backgroundColor:Int=0xff0c1119.toInt(),val backgroundOpacity:Double=.7,val gradient:Boolean=true,val gradientColor:Int=0xff1a2230.toInt(),val gradientAcross:Boolean=false,
 val accentColor:Int=0xff5e9fe8.toInt(),val selectionOpacity:Double=.22,val selectionRadius:Double=4.0,val smoothSelection:Boolean=true,val slideSpeed:Double=16.0,
 val shadow:Boolean=true,val shadowStrength:Double=.65,val glow:Boolean=true,val glowStrength:Double=.45,val effectRadius:Int=6,val shimmer:Boolean=false,val rainbowAccent:Boolean=false,val effectSpeed:Double=1.0,
 val referenceSizing:Boolean=false,val itemScale:Double=1.0,val pickupAnimation:Boolean=true,val showOffhand:Boolean=true,val keepOnScreen:Boolean=true) {
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
  val a=originalGeometry(s,offhand,offhandBefore,attack)
  fun cell(b:dev.solyanka.utility.archivehotbar.HotbarBox)=HotbarCell(b.x.roundToInt(),b.y.roundToInt(),b.width.roundToInt(),b.height.roundToInt())
  return StyledHotbarLayout(a.width.roundToInt(),a.height.roundToInt(),cell(a.mainPanel),(0..8).map {cell(a.slot(it.toFloat()))},a.offhand?.let(::cell),a.attackIndicator?.let(::cell),(a.slotSize+a.gap).roundToInt(),s.vertical)
 }
 fun originalGeometry(s:MoveHotbarSettings,offhand:Boolean=false,offhandBefore:Boolean=true,attack:Boolean=false)=dev.solyanka.utility.archivehotbar.HotbarLayout(s.vertical,if(s.style=="vanilla")20f else 22f,if(s.style=="vanilla")0f else s.slotGap.toFloat(),if(s.style=="vanilla")1f else s.padding.toFloat(),offhand,offhandBefore,attack)
 fun referenceScale(s:MoveHotbarSettings,w:Int,h:Int)=if(s.referenceSizing)dev.solyanka.utility.archivehotbar.HotbarViewport.fromGui(w,h).scale.toDouble() else 1.0

 fun anchor(s:MoveHotbarSettings,w:Int,h:Int,g:StyledHotbarLayout):HudSpec {
  // Fit the complete layout and effect margins on ordinary Minecraft GUI viewports.
  // Requested scale stays in config; editor and gameplay share the effective scale.
  val margin=if(s.style=="vanilla" && !s.vertical && !s.referenceSizing)0 else if(s.shadow || s.glow)s.effectRadius+2 else 2
  val requested=s.hud.scale*referenceScale(s,w,h)
  val scale=if(s.keepOnScreen)minOf(requested,w.coerceAtLeast(1).toDouble()/(g.width+2*margin),h.coerceAtLeast(1).toDouble()/(g.height+2*margin)).coerceAtLeast(.001)else requested
  val bw=ceil(g.width*scale).toInt();val bh=ceil(g.height*scale).toInt();val edge=ceil(margin*scale).toInt()
  val at=if(s.customPosition)s.hud.copy(scale=scale)else s.hud.copy(scale=scale,x=((w-bw)/2).coerceAtLeast(0),y=(h-bh-edge).coerceAtLeast(0))
  return if(s.keepOnScreen)at.copy(x=at.x.coerceIn(minOf(edge,(w-bw).coerceAtLeast(0)),(w-bw-edge).coerceAtLeast(edge)),y=at.y.coerceIn(minOf(edge,(h-bh).coerceAtLeast(0)),(h-bh-edge).coerceAtLeast(edge)))else at
 }
 /** One-click recovery preserves appearance but cannot leave a HUD hidden or offscreen. */
 fun restore(s:MoveHotbarSettings)=s.copy(customPosition=false,keepOnScreen=true,hud=s.hud.copy(enabled=true,x=389,y=518,scale=1.0))
 /** Defaults explicitly declared by impl.zip MoveHotbar.kt; original painter is retained in client/feature/hotbar. */
 fun archiveDefaults(s:MoveHotbarSettings)=MoveHotbarSettings(customPosition=s.customPosition,hud=s.hud.copy(enabled=true),style="glass",moveItemName=false,referenceSizing=true)

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
  val at=editorHud(s,w,h);val scale=at.scale
  val x=at.x.coerceIn(0,(w-WIDTH*scale).toInt().coerceAtLeast(0));val y=at.y.coerceIn(0,(h-HEIGHT*scale).toInt().coerceAtLeast(0))
  return Transform(x-(w/2-91)*scale,y-(h-22)*scale,scale)
 }
}
