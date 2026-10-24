/* SPDX-License-Identifier: GPL-3.0-only; impl.zip PetDisplay server snapshot adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component

object PetDisplay {
 private data class Snapshot(val key:String,val reading:PetLogic.Reading,val stack:ItemStack,val time:Long,val stable:Boolean)
 private val cache=linkedMapOf<String,Snapshot>();private val history=PetHistory()
 private var active:Snapshot?=null;private var cached=false;private var pending="";private var menu:Any?=null
 private var selectedSlot=-1;private var lastSignature="";private var titleTicks=0;private var title=""
 private fun now()=System.nanoTime()/1_000_000L
 fun reset(){cache.clear();history.clear();active=null;cached=false;pending="";menu=null;selectedSlot=-1;lastSignature="";titleTicks=0;title=""}
 private fun clearActive(){active=null;history.clear();selectedSlot=-1;cached=false}
 private fun select(value:String){
  val plain=ChatLogic.plain(value).trim().removeSuffix("!");val m=Regex("^\\[Lvl (\\d+)] (.+)$").matchEntire(plain)
  val name=m?.groupValues?.get(2) ?: plain
  val found=cache.values.filter {it.reading.name.equals(name,true) && (m==null || it.reading.level==m.groupValues[1].toIntOrNull())}
  history.clear();active=found.singleOrNull();cached=active!=null;pending=name
 }
 private fun message(text:String,overlay:Boolean){
  if(overlay || !S.isEnabled(S.PET) || !FeatureRuntime.context.skyblock)return
  val t=ChatLogic.plain(text)
  if(t.startsWith("You despawned your ")){clearActive();pending="";return}
  val auto=Regex("^Autopet equipped your (.+?)(?:!|$)").find(t)?.groupValues?.get(1)
  val selected=Regex("^You summoned your (.+?)!$").find(t)?.groupValues?.get(1) ?: auto ?: return
  select(selected)
  val s=S.currentGeneral().features.pet
  if(auto!=null && s.autoTitle && (!s.titleDungeonOnly || FeatureRuntime.context.dungeon)){title=active?.reading?.name ?: pending;titleTicks=40}
 }
 fun tick(){
  val c=Minecraft.getInstance();if(!S.isEnabled(S.PET) || c.level==null || !FeatureRuntime.context.skyblock){reset();return}
  if(titleTicks>0)titleTicks--
  val screen=c.gui.screen() as? AbstractContainerScreen<*> ?: run {menu=null;selectedSlot=-1;lastSignature="";return}
  if(!Regex("^Pets(?: \\(\\d+/\\d+\\))?$").matches(ChatLogic.plain(screen.title.string))) {menu=null;selectedSlot=-1;return}
  if(menu!==screen.menu){cache.entries.removeIf {!it.value.stable};if(active?.stable==false)clearActive();menu=screen.menu;lastSignature=""}
  val items=screen.menu.slots.filter {it.container!==c.player?.inventory}.take(512)
  // Record only changed server snapshots; identical client ticks cannot manufacture XP/rates.
  val signature=items.joinToString("|"){"${it.index}:${it.item}"};if(signature==lastSignature)return;lastSignature=signature
  selectedSlot=-1
  items.forEach {slot->
   val stack=slot.item;if(stack.isEmpty)return@forEach
   val data=stack.get(DataComponents.CUSTOM_DATA)?.copyTag();val attrs=data?.getCompound("ExtraAttributes")?.orElse(null) ?: data
   val json=data?.getStringOr("petInfo","")?.takeIf {it.isNotBlank()} ?: attrs?.getStringOr("petInfo","")
   val lore=stack.get(DataComponents.LORE)?.lines()?.map {it.string} ?: emptyList()
   val read=PetLogic.read(stack.hoverName.string,lore,json) ?: return@forEach
   val uuid=read.uuid ?: data?.getStringOr("uuid","")?.takeIf {it.isNotBlank()} ?: attrs?.getStringOr("uuid","")?.takeIf {it.isNotBlank()}
   val key=uuid ?: "${read.type}:${read.rarity}:window${screen.menu.containerId}:slot${slot.index}"
   val state=Snapshot(key,read,stack.copy(),now(),uuid!=null);cache[key]=state
   if(cache.size>512)cache.keys.firstOrNull {it!=active?.key}?.let(cache::remove)
   if(read.active){if(active?.key!=key)history.clear();active=state;cached=false;pending=read.name;selectedSlot=slot.index;read.xp?.let {history.record(now(),it,S.currentGeneral().features.pet.sampleMinutes)}}
   else if(active?.key==key && lore.any {ChatLogic.plain(it).equals("Click to summon!",true)})clearActive()
  }
 }
 fun overlay(g:net.minecraft.client.gui.GuiGraphicsExtractor,slot:net.minecraft.world.inventory.Slot,x:Int,y:Int){
  if(S.isEnabled(S.PET) && S.currentGeneral().features.pet.highlight && menu!=null && slot.index==selectedSlot)g.fill(x,y,x+16,y+16,S.currentGeneral().features.pet.highlightColor)
 }
 fun sync(){val c=Minecraft.getInstance();if(S.isEnabled(S.PET) && FeatureRuntime.context.skyblock)c.player?.connection?.sendCommand("pets")}
 fun register(){
  ClientReceiveMessageEvents.GAME.register {m,o->message(m.string,o)};ClientReceiveMessageEvents.GAME_CANCELED.register {m,o->message(m.string,o)}
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","pet")){g,_->
   val c=Minecraft.getInstance();val s=S.currentGeneral().features.pet;val h=s.hud
   if(!S.isEnabled(S.PET) || c.level==null || c.gui.hud.isHidden || !FeatureRuntime.context.skyblock || c.gui.screen() is dev.solyanka.client.ui.HudEditorScreen)return@attachElementBefore
   if(h.enabled && (active!=null || pending.isNotBlank())) {
    g.pose().pushMatrix();try{g.pose().translate(h.x.toFloat(),h.y.toFloat());g.pose().scale(h.scale.toFloat(),h.scale.toFloat())
     if(s.style!=0)ChromeDraw.panel(g,0,0,220,57,s.background,if(s.style==2)s.accent else 0x40555555,5)
     val a=active;val read=a?.reading
     if(a!=null)g.item(a.stack,5,5)
     g.text(c.font,if(read!=null)"[Lvl ${read.level ?: "?"}] ${read.name}" else pending,25,5,s.accent,true)
     g.text(c.font,"Total XP: "+(read?.xp?.let {"%,.0f".format(java.util.Locale.ROOT,it)} ?: "unknown — open /pets"),5,22,h.color,false)
     val rate=history.hourly(now(),s.sampleMinutes)?.let {"%,.0f XP/h".format(java.util.Locale.ROOT,it)} ?: "Rate: awaiting changed snapshots"
     g.text(c.font,rate,5,34,h.color,false)
     val age=a?.let {((now()-it.time)/1000).coerceAtLeast(0)}
     g.text(c.font,if(a==null)"Sync needed — no unique cached pet" else "${if(cached)"Cached" else "Server snapshot"} • ${age}s ago",5,46,0xffaaaaaa.toInt(),false)
    }finally{g.pose().popMatrix()}
   }
   if(titleTicks>0 && s.autoTitle)g.text(c.font,title,(c.window.guiScaledWidth-c.font.width(title))/2,c.window.guiScaledHeight/3,s.accent,true)
  }
 }
}
