/* SPDX-License-Identifier: GPL-3.0-only
 * Stray InventoryHudRenderer, HeldItemHudRenderer, SkillProgressHudRenderer/Tracker
 * CC0 adaptations, camberX and contributors. See NOTICE for pinned source. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.client.ui.nofrills.NoFrillsClickGui
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.*
import kotlin.math.roundToInt

object ExtraHuds {
    val skills=SkillProgress()
    private var lastLevelTick=Int.MIN_VALUE
    fun reset() {skills.reset();lastLevelTick=Int.MIN_VALUE}
    fun tick(client:Minecraft) {
        if(!SolyankaClient.isEnabled(SolyankaClient.SKILL_HUD) || !FeatureRuntime.context.skyblock) {reset();return}
        val tick=client.player?.tickCount ?: return
        if(lastLevelTick!=Int.MIN_VALUE && tick>=lastLevelTick && tick-lastLevelTick<10) return
        lastLevelTick=tick
        client.connection?.listedOnlinePlayers?.take(80)?.forEach {it.tabListDisplayName?.string?.let(skills::tab)}
        val screen=client.gui.screen() as? AbstractContainerScreen<*> ?: return
        val title=ChatLogic.plain(screen.title.string).lowercase()
        if(title !in setOf("your skills","skills") && !title.endsWith(" skills")) return
        screen.menu.slots.take((screen.menu.slots.size-36).coerceIn(0,512)).forEach {slot->
            if(!slot.item.isEmpty) skills.menuItem(buildString {
                append(slot.item.hoverName.string);append('\n')
                slot.item.get(DataComponents.LORE)?.lines()?.take(64)?.forEach {append(it.string);append('\n')}
            })
        }
    }
    private inline fun frame(g:GuiGraphicsExtractor,h:HudSpec,w:Int,tall:Int,atX:Int?=null,atY:Int?=null,draw:()->Unit) {
        val c=Minecraft.getInstance();val scale=h.scale.toFloat()
        val x=atX ?: h.x.coerceIn(0,(c.window.guiScaledWidth-w*scale).roundToInt().coerceAtLeast(0))
        val y=atY ?: h.y.coerceIn(0,(c.window.guiScaledHeight-tall*scale).roundToInt().coerceAtLeast(0))
        g.pose().pushMatrix()
        try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);draw()}
        finally {g.pose().popMatrix()}
    }
    private fun panel(g:GuiGraphicsExtractor,w:Int,h:Int,enabled:Boolean,color:Int) {if(enabled) {g.fill(0,0,w,h,color);dev.solyanka.client.ui.nofrills.GuiKit.border(g,0,0,w,h,0xff5ca0bf.toInt())}}
    private fun inventory(g:GuiGraphicsExtractor,s:InventoryHudSettings,sample:Boolean=false,x:Int?=null,y:Int?=null) {
        StrayInventoryHud.draw(g,s,sample,x,y)
    }
    private fun itemName(g:GuiGraphicsExtractor,s:ItemNameHudSettings,sample:Boolean=false,x:Int?=null,y:Int?=null) {
        val c=Minecraft.getInstance();val stack=c.player?.mainHandItem
        if(!sample && (stack==null || stack.isEmpty)) return
        val text=if(sample) Component.literal("Sample held item") else stack!!.styledHoverName.copy()
        val w=maxOf(80,c.font.width(text)+16)
        frame(g,s.hud,w,18,x,y) {panel(g,w,18,s.background,s.backgroundColor);g.text(c.font,text,8,5,s.hud.color,true)}
    }
    private fun icon(kind:SkillKind):Item = when(kind) {
        SkillKind.FARMING->Items.GOLDEN_HOE;SkillKind.MINING->Items.IRON_PICKAXE;SkillKind.COMBAT->Items.DIAMOND_SWORD;SkillKind.FORAGING->Items.GOLDEN_AXE;SkillKind.FISHING->Items.FISHING_ROD;SkillKind.ENCHANTING->Items.ENCHANTED_BOOK;SkillKind.ALCHEMY->Items.BREWING_STAND;SkillKind.TAMING->Items.BONE;SkillKind.CARPENTRY->Items.CRAFTING_TABLE;SkillKind.RUNECRAFTING->Items.MAGMA_CREAM;SkillKind.SOCIAL->Items.EMERALD;SkillKind.CATACOMBS->Items.WITHER_SKELETON_SKULL
    }
    private fun skill(g:GuiGraphicsExtractor,s:SkillHudSettings,sample:Boolean=false,x:Int?=null,y:Int?=null) {
        val snap=if(sample) SkillSnapshot(SkillKind.FARMING,12345,20000,14,15,61.725,"+9.2",0) else skills.visible(s.timeoutSeconds) ?: return
        val c=Minecraft.getInstance()
        val text=(if(s.showGain) "${snap.gained} " else "")+snap.kind.label+(if(s.showLevel && snap.level!=null) " ${snap.level}" else "")+" "+SkillProgress.progressText(snap)
        val iconWidth=if(s.icon) 20 else 0;val w=c.font.width(text)+10+iconWidth
        frame(g,s.hud,w,26,x,y) {
            panel(g,w,26,s.background,s.backgroundColor)
            if(s.icon) g.item(ItemStack(icon(snap.kind)),5,5,310)
            g.text(c.font,text,5+iconWidth,9,s.hud.color,true)
        }
    }
    fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
        val f=SolyankaClient.currentGeneral().features
        when(key) {"extra.inventory"->inventory(g,f.inventoryHud,true,x,y);"extra.item_name"->itemName(g,f.itemNameHud,true,x,y);"extra.skill"->skill(g,f.skillHud,true,x,y);else->return false}
        return true
    }
    private fun render(g:GuiGraphicsExtractor) {
        val c=Minecraft.getInstance();if(c.player==null || c.level==null || c.gui.hud.isHidden) return
        val screen=c.gui.screen()
        if(screen is HudEditorScreen || screen!=null && screen !is ChatScreen && screen !is NoFrillsClickGui) return
        val f=SolyankaClient.currentGeneral().features
        if(SolyankaClient.isEnabled(SolyankaClient.INVENTORY_HUD) && f.inventoryHud.hud.enabled) inventory(g,f.inventoryHud)
        if(SolyankaClient.isEnabled(SolyankaClient.ITEM_NAME_HUD) && f.itemNameHud.hud.enabled) itemName(g,f.itemNameHud)
        if(SolyankaClient.isEnabled(SolyankaClient.SKILL_HUD) && f.skillHud.hud.enabled && FeatureRuntime.context.skyblock) skill(g,f.skillHud)
    }
    fun register() {
        HudElementRegistry.replaceElement(VanillaHudElements.HELD_ITEM_TOOLTIP) {original->HudElement {g,d->
            val s=SolyankaClient.currentGeneral().features.itemNameHud
            if(!(SolyankaClient.isEnabled(SolyankaClient.ITEM_NAME_HUD) && s.hud.enabled && s.hideVanilla)) original.extractRenderState(g,d)
        }}
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","extra_huds")) {g,_->render(g)}
    }
}
