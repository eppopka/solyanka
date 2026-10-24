/* SPDX-License-Identifier: GPL-3.0-only; impl.zip MoveHotbar native-item/appearance adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.ui.HudEntry
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.AttackIndicatorStatus
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import kotlin.math.*
object VanillaHotbar {
 private var selection=HotbarSelection();private var lastFrame=0L;private var lastStyle=""
 private fun geometry(s:MoveHotbarSettings):StyledHotbarLayout {
  val c=Minecraft.getInstance();val p=c.cameraEntity as? Player
  return HotbarLayout.geometry(s,s.showOffhand && p?.offhandItem?.isEmpty==false,p?.mainArm!=HumanoidArm.LEFT,c.options.attackIndicator().get()==AttackIndicatorStatus.HOTBAR)
 }
 private fun native(s:MoveHotbarSettings)=s.style=="vanilla" && !s.vertical
 fun register(){listOf(VanillaHudElements.HOTBAR,VanillaHudElements.HELD_ITEM_TOOLTIP,VanillaHudElements.INFO_BAR,VanillaHudElements.EXPERIENCE_LEVEL).forEach {id->
  HudElementRegistry.replaceElement(id){original->HudElement {g,d->
   val c=Minecraft.getInstance();val s=S.currentGeneral().features.moveHotbar;val p=c.cameraEntity as? Player
   val follower=when(id){VanillaHudElements.HOTBAR->true;VanillaHudElements.HELD_ITEM_TOOLTIP->s.moveItemName;VanillaHudElements.INFO_BAR->s.moveInfoBar;else->s.moveXpLevel}
   if(!S.isEnabled(S.MOVE_HOTBAR) || c.level==null || p==null || p.isSpectator || !follower){if(id==VanillaHudElements.HOTBAR)lastFrame=0;original.extractRenderState(g,d)}
   else if(s.hud.enabled && !c.gui.hud.isHidden && c.gui.screen() !is HudEditorScreen){
    if(id==VanillaHudElements.HOTBAR && !native(s)) {
     val geo=geometry(s);val at=HotbarLayout.anchor(s,c.window.guiScaledWidth,c.window.guiScaledHeight,geo)
     g.pose().pushMatrix();try{g.pose().translate(at.x.toFloat(),at.y.toFloat());g.pose().scale(s.hud.scale.toFloat(),s.hud.scale.toFloat());draw(g,s,geo,d,false)}finally{g.pose().popMatrix()}
    } else {
     val t=if(native(s))HotbarLayout.transform(s,c.window.guiScaledWidth,c.window.guiScaledHeight) else {
      val geo=geometry(s);val at=HotbarLayout.anchor(s,c.window.guiScaledWidth,c.window.guiScaledHeight,geo);val scale=s.hud.scale
      // Followers remain horizontal, centered on the main track, above its top edge.
      val desiredCenter=at.x+geo.main.centerX*scale
      val half=91*scale;val center=desiredCenter.coerceIn(minOf(half,c.window.guiScaledWidth/2.0),maxOf(half,c.window.guiScaledWidth-half))
      HotbarLayout.Transform(center-c.window.guiScaledWidth/2*scale,at.y-(c.window.guiScaledHeight-22)*scale,scale)
     }
     g.pose().pushMatrix();try{g.pose().translate(t.x.toFloat(),t.y.toFloat());g.pose().scale(t.scale.toFloat(),t.scale.toFloat());original.extractRenderState(g,d)}finally{g.pose().popMatrix()}
    }
   }
  }}
 }}
 fun editorEntry(entry:HudEntry,w:Int,h:Int):HudEntry {
  if(entry.key!="archive.hotbar")return entry
  val s=S.currentGeneral().features.moveHotbar
  if(native(s))return entry.copy(baseWidth=HotbarLayout.WIDTH,baseHeight=HotbarLayout.HEIGHT)
  val geo=geometry(s);return entry.copy(hud=HotbarLayout.anchor(s,w,h,geo),baseWidth=geo.width,baseHeight=geo.height)
 }
 fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
  if(key!="archive.hotbar")return false
  val s=S.currentGeneral().features.moveHotbar;g.pose().pushMatrix()
  try{g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(s.hud.scale.toFloat(),s.hud.scale.toFloat());draw(g,s,if(native(s))HotbarLayout.geometry(s)else geometry(s),DeltaTracker.ZERO,true)}finally{g.pose().popMatrix()};return true
 }
 private fun mix(a:Int,b:Int,t:Double):Int {val f=t.coerceIn(0.0,1.0)
  fun ch(s:Int)=(((a ushr s)and 255)*(1-f)+((b ushr s)and 255)*f).roundToInt()
  return (ch(24) shl 24)or(ch(16)shl 16)or(ch(8)shl 8)or ch(0)}
 private fun alpha(rgb:Int,a:Double)=(rgb and 0xffffff)or((255*a.coerceIn(0.0,1.0)).roundToInt()shl 24)
 private fun draw(g:GuiGraphicsExtractor,s:MoveHotbarSettings,geo:StyledHotbarLayout,d:DeltaTracker,sample:Boolean) {
  val c=Minecraft.getInstance();val player=c.cameraEntity as? Player;val now=System.nanoTime();val elapsed=if(lastFrame==0L)1.0 else (now-lastFrame)*1e-9
  val styleKey="${s.style}/${s.vertical}"
  if(lastStyle!=styleKey){selection=HotbarSelection();lastStyle=styleKey};lastFrame=now
  val seconds=now*1e-9;val accent=if(s.rainbowAccent)java.awt.Color.HSBtoRGB(((seconds*.08*s.effectSpeed)%1).toFloat(),.65f,1f)else s.accentColor
  fun panel(b:HotbarCell) {
   val r=if(s.style=="vanilla")1 else s.cornerRadius.roundToInt()
   if(s.shadow)for(i in 3 downTo 1)ChromeDraw.panel(g,b.x-i,b.y-i+2,b.width+i*2,b.height+i*2,alpha(0,.035*s.shadowStrength),0,r+i)
   val border=alpha(accent,if(s.style=="neon") .8 else .27)
   val base=alpha(s.backgroundColor,s.backgroundOpacity)
   ChromeDraw.panel(g,b.x,b.y,b.width,b.height,base,if(s.borderWidth>0)border else base,r)
   if(s.gradient || s.shimmer) {
    val vertical=if(s.gradientAcross)!geo.vertical else geo.vertical
    // Bounded 24 bands, rounded-edge insets; no per-pixel UI tessellation.
    val size=if(vertical)b.height else b.width;val bands=minOf(24,size)
    for(i in 0 until bands){val low=i*size/bands;val high=(i+1)*size/bands;val t=(i+.5)/bands
     val color=if(s.gradient)mix(base,alpha(s.gradientColor,s.backgroundOpacity),t)else base
     val wave=if(s.shimmer)exp(-((t-((seconds*s.effectSpeed*.15)%1))*8).pow(2.0))*.12 else 0.0
     val fill=if(wave>0)mix(color,alpha(0xffffff,s.backgroundOpacity),wave)else color
     if(vertical){val inset=maxOf(1,VisualChrome.inset((low+high)/2,b.height,r));if(b.width>inset*2)g.fill(b.x+inset,b.y+low,b.x+b.width-inset,b.y+high,fill)}
     else {val inset=maxOf(1,VisualChrome.inset((low+high)/2,b.width,r));if(b.height>inset*2)g.fill(b.x+low,b.y+inset,b.x+high,b.y+b.height-inset,fill)}
    }
   }
   if(s.borderWidth>=1.5)for(i in 1 until s.borderWidth.roundToInt())ChromeDraw.panel(g,b.x+i,b.y+i,b.width-i*2,b.height-i*2,0,border,(r-i).coerceAtLeast(0))
  }
  panel(geo.main);geo.offhand?.let {panel(it)};geo.attack?.let {panel(it)}
  val slot=if(sample)0 else player?.inventory?.selectedSlot ?: 0
  val box=geo.selection(if(sample)slot.toDouble()else selection.update(slot,elapsed,s.slideSpeed,s.smoothSelection))
  if(s.glow || s.style=="neon")for(i in 3 downTo 1){val n=maxOf(1,s.effectRadius*i/3);ChromeDraw.panel(g,box.x-n,box.y-n,box.width+n*2,box.height+n*2,alpha(accent,s.glowStrength*.045),0,s.selectionRadius.roundToInt()+n)}
  ChromeDraw.panel(g,box.x,box.y,box.width,box.height,alpha(accent,s.selectionOpacity),alpha(accent,.8),s.selectionRadius.roundToInt())
  fun item(stack:ItemStack,cell:HotbarCell,seed:Int) {
   if(stack.isEmpty)return
   val size=minOf(16*s.itemScale,cell.width-4.0).toFloat().coerceAtLeast(1f)
   g.pose().pushMatrix();try{
    g.pose().translate(cell.centerX-size/2,cell.centerY-size/2);g.pose().scale(size/16,size/16)
    g.pose().pushMatrix();try{
     val pop=if(!sample && s.pickupAnimation)stack.popTime-d.getGameTimeDeltaPartialTick(false) else 0f
     if(pop>0){val f=1+pop/5;g.pose().translate(8f,12f);g.pose().scale(1/f,(f+1)/2);g.pose().translate(-8f,-12f)}
     if(player==null)g.item(stack,0,0,seed)else g.item(player,stack,0,0,seed)
    }finally{g.pose().popMatrix()}
    g.itemDecorations(c.font,stack,0,0)
   }finally{g.pose().popMatrix()}
  }
  val preview=listOf(Items.DIAMOND_SWORD,Items.BOW,Items.DIAMOND_PICKAXE,Items.GOLDEN_APPLE,Items.ENDER_PEARL,Items.TORCH,Items.POTION,Items.COOKED_BEEF,Items.NETHER_STAR)
  for(i in 0..8)item(if(sample)ItemStack(preview[i])else player?.inventory?.getItem(i) ?: ItemStack.EMPTY,geo.slots[i],i+1)
  geo.offhand?.let {item(if(sample)ItemStack(Items.SHIELD)else player?.offhandItem ?: ItemStack.EMPTY,it,10)}
  geo.attack?.let {cell->val strength=if(sample).7f else player?.getAttackStrengthScale(0f)?.coerceIn(0f,1f) ?: 0f
   val height=cell.height-6;g.fill(cell.centerX.toInt()-2,cell.y+3,cell.centerX.toInt()+2,cell.y+3+height,0x28ffffff);g.fill(cell.centerX.toInt()-2,cell.y+3+height-(height*strength).roundToInt(),cell.centerX.toInt()+2,cell.y+3+height,accent)
  }
 }
}
