/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from camberX/Stray CC0 InventoryHudRenderer, HudChrome, GuiDraw and Theme.
 * Pinned d91fcd4515b4eda647ee8fd7fa99408f473e5718; see audit/stray-ui-blur-source-manifest.json. */
package dev.solyanka.client.feature
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.*
import kotlin.math.roundToInt

/** Display only. Stray's 18px wells / 2px gaps / armor row / separate selected hotbar. */
object StrayInventoryHud {
 private const val ACCENT=0xff2fb5ff.toInt()
 private const val LINE=0xff1c2430.toInt()
 private const val TRACK=0xff1a222c.toInt()
 private const val HOVER=0xff171b24.toInt()
 private const val MUTED=0xff8a9aab.toInt()
 private const val OFF=0xff3d4a58.toInt()
 private val armor=arrayOf(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)
 private val marks=arrayOf("H","C","L","B")
 private val sampleArmor=arrayOf(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS)
 private val sampleMain=arrayOf(Items.DIAMOND,Items.EMERALD,Items.ENDER_PEARL)
 private var countPlayer=java.lang.ref.WeakReference<Any>(null);private var countTick=Int.MIN_VALUE;private var countLabel="0/41"
 private fun filled():String {
  val player=Minecraft.getInstance().player ?: return "0/41"
  if(countPlayer.get()===player && countTick==player.tickCount) return countLabel
  var count=0
  for(i in 0 until 36) if(!player.inventory.getItem(i).isEmpty) count++
  for(slot in armor) if(!player.getItemBySlot(slot).isEmpty) count++
  if(!player.offhandItem.isEmpty) count++
  if(countPlayer.get()!==player) countPlayer=java.lang.ref.WeakReference(player)
  countTick=player.tickCount;countLabel="$count/41"
  return countLabel
 }
 fun draw(g:GuiGraphicsExtractor,s:InventoryHudSettings,sample:Boolean=false,atX:Int?=null,atY:Int?=null) {
  val c=Minecraft.getInstance();val player=c.player;if(!sample && player==null) return
  val m=InventoryHudLayout.metrics(s);val scale=s.hud.scale.toFloat()
  val x=atX ?: s.hud.x.coerceIn(0,(c.window.guiScaledWidth-m.width*scale).roundToInt().coerceAtLeast(0))
  val y=atY ?: s.hud.y.coerceIn(0,(c.window.guiScaledHeight-m.height*scale).roundToInt().coerceAtLeast(0))
  g.pose().pushMatrix()
  try {
   g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale)
   // Stray classic chrome. No costly frost/starfield pass in solyanka's inventory overlay.
   if(s.background) ChromeDraw.panel(g,0,0,m.width,m.height,s.backgroundColor,LINE,6)
   fun small(text:String,tx:Float,ty:Float,color:Int) {
    g.pose().pushMatrix()
    try {g.pose().translate(tx,ty);g.pose().scale(.85f,.85f);g.text(c.font,text,0,0,color,false)} finally {g.pose().popMatrix()}
   }
   if(!s.minimal) {
    small("INVENTORY",(m.pad+4).toFloat(),(m.pad+1).toFloat(),s.hud.color)
    if(s.count) {
     val text=if(sample) "9/41" else filled()
     small(text,m.width-m.pad-c.font.width(text)*.85f,(m.pad+1).toFloat(),MUTED)
    }
   }
   fun slot(stack:ItemStack,sx:Int,sy:Int,seed:Int,empty:String?=null,selected:Boolean=false) {
    if(!s.minimal) {
     // GuiDraw.well: outline underlay + inset fill, just two quads per slot.
     g.fill(sx,sy,sx+18,sy+18,if(selected) ACCENT else LINE)
     g.fill(sx+1,sy+1,sx+17,sy+17,if(selected) HOVER else TRACK)
    }
    if(stack.isEmpty) {
     if(empty!=null) small(empty,sx+(18-c.font.width(empty)*.85f)*.5f,(sy+4).toFloat(),OFF)
     return
    }
    if(player!=null) g.item(player,stack,sx+1,sy+1,seed) else g.item(stack,sx+1,sy+1,seed)
    g.itemDecorations(c.font,stack,sx+1,sy+1)
   }
   if(s.armor) {
    for(i in armor.indices) slot(if(sample) ItemStack(sampleArmor[i]) else player!!.getItemBySlot(armor[i]),m.pad+i*20,m.armorY,100+i,marks[i])
    slot(if(sample) ItemStack(Items.SHIELD) else player!!.offhandItem,m.pad+100,m.armorY,104,"O")
   }
   for(row in 0 until 3) for(col in 0 until 9) {
    val i=row*9+col;val index=9+i
    val stack=if(sample && i<sampleMain.size) ItemStack(sampleMain[i]) else if(sample) ItemStack.EMPTY else player!!.inventory.getItem(index)
    slot(stack,m.pad+col*20,m.mainY+row*20,index)
   }
   if(s.hotbar) {
    g.fill(m.pad,m.hotbarY-3,m.width-m.pad,m.hotbarY-2,LINE)
    for(i in 0 until 9) slot(if(sample && i==0) ItemStack(Items.DIAMOND_SWORD) else if(sample) ItemStack.EMPTY else player!!.inventory.getItem(i),m.pad+i*20,m.hotbarY,i,selected=if(sample) i==0 else player!!.inventory.selectedSlot==i)
   }
  } finally {g.pose().popMatrix()}
 }
}
