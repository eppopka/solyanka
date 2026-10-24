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
 private var selection=dev.solyanka.utility.archivehotbar.HotbarSelection();private var lastFrame=0L;private var lastStyle="";private val effectEpoch=System.nanoTime()
 private fun geometry(s:MoveHotbarSettings):StyledHotbarLayout {
  val c=Minecraft.getInstance();val p=c.cameraEntity as? Player
  return HotbarLayout.geometry(s,s.showOffhand && p?.offhandItem?.isEmpty==false,p?.mainArm!=HumanoidArm.LEFT,c.options.attackIndicator().get()==AttackIndicatorStatus.HOTBAR)
 }
 fun register(){listOf(VanillaHudElements.HOTBAR,VanillaHudElements.HELD_ITEM_TOOLTIP,VanillaHudElements.INFO_BAR,VanillaHudElements.EXPERIENCE_LEVEL).forEach {id->
  HudElementRegistry.replaceElement(id){original->HudElement {g,d->
   val c=Minecraft.getInstance();val s=S.currentGeneral().features.moveHotbar;val p=c.cameraEntity as? Player
   val follower=when(id){VanillaHudElements.HOTBAR->true;VanillaHudElements.HELD_ITEM_TOOLTIP->s.moveItemName;VanillaHudElements.INFO_BAR->s.moveInfoBar;else->s.moveXpLevel}
   if(!S.isEnabled(S.MOVE_HOTBAR) || c.level==null || p==null || p.isSpectator || !follower || !s.hud.enabled){if(id==VanillaHudElements.HOTBAR)lastFrame=0;original.extractRenderState(g,d)}
   else if(s.hud.enabled && !c.gui.hud.isHidden && c.gui.screen() !is HudEditorScreen){
    if(id==VanillaHudElements.HOTBAR) {
     val geo=geometry(s);val at=HotbarLayout.anchor(s,c.window.guiScaledWidth,c.window.guiScaledHeight,geo)
     g.pose().pushMatrix();try{g.pose().translate(at.x.toFloat(),at.y.toFloat());g.pose().scale(at.scale.toFloat(),at.scale.toFloat());draw(g,s,geo,d,false)}finally{g.pose().popMatrix()}
    } else {
     val t=run {
      val geo=geometry(s);val at=HotbarLayout.anchor(s,c.window.guiScaledWidth,c.window.guiScaledHeight,geo);val scale=at.scale
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
  val geo=geometry(s);return entry.copy(hud=HotbarLayout.anchor(s,w,h,geo),baseWidth=geo.width,baseHeight=geo.height)
 }
 fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
  if(key!="archive.hotbar")return false
  val s=S.currentGeneral().features.moveHotbar;g.pose().pushMatrix()
  try{val geo=geometry(s);val scale=HotbarLayout.anchor(s,Minecraft.getInstance().window.guiScaledWidth,Minecraft.getInstance().window.guiScaledHeight,geo).scale;g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale.toFloat(),scale.toFloat());draw(g,s,geo,DeltaTracker.ZERO,true)}finally{g.pose().popMatrix()};return true
 }
 private fun draw(g:GuiGraphicsExtractor,s:MoveHotbarSettings,geo:StyledHotbarLayout,d:DeltaTracker,sample:Boolean) {
  val c=Minecraft.getInstance();val player=c.cameraEntity as? Player;val now=System.nanoTime();val elapsed=if(lastFrame==0L)0f else ((now-lastFrame)*1e-9).toFloat()
  val styleKey="${s.style}/${s.vertical}"
  if(lastStyle!=styleKey || elapsed>.5f){selection.reset();lastStyle=styleKey};lastFrame=now
  val seconds=((now-effectEpoch)*1e-9).toFloat();val accent=java.awt.Color(if(s.rainbowAccent)java.awt.Color.HSBtoRGB(((seconds*.08*s.effectSpeed)%1).toFloat(),.65f,1f)else s.accentColor,true)
  val offhand=geo.offhand!=null;val before=geo.offhand?.let {if(geo.vertical)it.y<geo.main.y+s.padding else it.x<geo.main.x+s.padding} ?: (player?.mainArm!=HumanoidArm.LEFT)
  val layout=HotbarLayout.originalGeometry(s,offhand,before,geo.attack!=null)
  val look=dev.solyanka.client.feature.hotbar.HotbarPainter.Appearance(
   style=dev.solyanka.client.feature.hotbar.HotbarPainter.Style.valueOf(s.style.uppercase(java.util.Locale.ROOT)),background=java.awt.Color(s.backgroundColor,true),secondary=java.awt.Color(s.gradientColor,true),accent=accent,
   opacity=s.backgroundOpacity.toFloat(),radius=if(s.style=="vanilla")1f else s.cornerRadius.toFloat(),borderWidth=if(s.style=="vanilla")1f else s.borderWidth.toFloat(),gradient=s.gradient,gradientVertical=if(s.gradientAcross)!s.vertical else s.vertical,
   shadow=s.shadow,shadowStrength=s.shadowStrength.toFloat(),glow=s.glow,glowStrength=s.glowStrength.toFloat(),effectRadius=s.effectRadius.toFloat(),shimmer=s.shimmer,shimmerSpeed=s.effectSpeed.toFloat()*.15f,selectionOpacity=s.selectionOpacity.toFloat(),selectionRadius=s.selectionRadius.toFloat())
  dev.solyanka.client.feature.hotbar.HotbarPainter.background(g,layout,look,seconds)
  val slot=if(sample)0 else player?.inventory?.selectedSlot ?: 0
  val selected=if(sample)slot.toFloat()else selection.update(slot,elapsed,s.slideSpeed.toFloat(),s.smoothSelection)
  dev.solyanka.client.feature.hotbar.HotbarPainter.selection(g,layout.slot(selected),look)
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
   val height=cell.height-6;g.fill(cell.centerX.toInt()-2,cell.y+3,cell.centerX.toInt()+2,cell.y+3+height,0x28ffffff);g.fill(cell.centerX.toInt()-2,cell.y+3+height-(height*strength).roundToInt(),cell.centerX.toInt()+2,cell.y+3+height,accent.rgb)
  }
 }
}
