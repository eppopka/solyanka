/* SPDX-License-Identifier: GPL-3.0-only
 * Odin TickTimers/PlayerDisplay adapters. Original (c) 2025 odtheking,
 * BSD-3-Clause, reproduced in licenses/Odin-BSD-3-Clause.txt. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.slf4j.LoggerFactory

object FeatureRuntime {
    private val logger=LoggerFactory.getLogger("solyanka/features")
    val timers=TickTimers()
    val stats=PlayerStats()
    val cursor=CursorMemory()
    var context=SkyblockContext();private set
    fun reset() { context=SkyblockContext();timers.reset();stats.reset();cursor.reset();ExtraHuds.reset() }
    private fun readContext(client:Minecraft):SkyblockContext {
        val player=client.player ?: return SkyblockContext()
        val scoreboard=client.level?.scoreboard ?: return SkyblockContext()
        val objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return SkyblockContext()
        val sidebar=scoreboard.listPlayerScores(objective).map { score ->
            PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(score.owner()),score.ownerName()).string
        }
        val tab=client.connection?.listedOnlinePlayers?.mapNotNull { it.tabListDisplayName?.string } ?: emptyList()
        return ContextParser.parse(objective.displayName.string,sidebar,tab,player.x,player.z)
    }
    fun tick(client:Minecraft) {
        val old=context;context=readContext(client)
        if(old.skyblock && !context.skyblock) stats.reset()
        if(old.dungeon && (!context.dungeon || old.floor!=context.floor || old.master!=context.master)) timers.reset()
        if(context.skyblock && SolyankaClient.isEnabled(SolyankaClient.PLAYER_DISPLAY)) {
            client.player?.let { stats.tick(it.health.toDouble(),it.maxHealth.toDouble(),it.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue ?: 0.0) }
        }
        if(SolyankaClient.isEnabled(SolyankaClient.CURSOR)) cursor.tick(client.level!=null,client.gui.screen()!=null)
    }
    fun ping(id:Int) {
        if(SolyankaClient.isEnabled(SolyankaClient.TICK_TIMERS)) {
            context=readContext(Minecraft.getInstance())
            timers.ping(id,context.boss,SolyankaClient.currentGeneral().features.timers)
        }
    }
    fun screenChanged() { if(SolyankaClient.isEnabled(SolyankaClient.CURSOR)) cursor.screenChanged(SolyankaClient.currentGeneral().features.fixes.cursorClearTicks) }
    fun cursorActive():Boolean {
        val client=Minecraft.getInstance()
        val screen=client.gui.screen()
        return SolyankaClient.isEnabled(SolyankaClient.CURSOR) && !(screen is AbstractContainerScreen<*> && screen.title.string=="Spirit Leap")
    }
    fun hideLoading():Boolean {
        val server=Minecraft.getInstance().currentServer
        return LoadingPolicy.hide(SolyankaClient.isEnabled(SolyankaClient.LOADING),SolyankaClient.currentGeneral().features.fixes.loadingServerOnly,server!=null,server?.isLan==true)
    }
    fun animationActive()=SolyankaClient.isEnabled(SolyankaClient.ANIMATION) && (!SolyankaClient.currentGeneral().features.fixes.animationSkyblockOnly || context.skyblock)
    fun middleOverride(slot:Slot?,button:Int,action:ContainerInput):Boolean {
        if(!SolyankaClient.isEnabled(SolyankaClient.MIDDLE_OVERRIDE) || slot==null) return false
        val client=Minecraft.getInstance();val screen=client.gui.screen() as? ContainerScreen ?: return false
        context=readContext(client)
        val stack=slot.item
        val result=MiddleClickPolicy.override(ChatLogic.plain(screen.title.string),button,action==ContainerInput.PICKUP,context.skyblock,stack.isEmpty,
            stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","") ?: "",
            stack.get(DataComponents.LORE)?.lines()?.map { ChatLogic.plain(it.string) } ?: emptyList(),
            screen.menu.slots.any { !it.item.isEmpty && ChatLogic.plain(it.item.hoverName.string)=="Rename Loadout" })
        if(result && SolyankaClient.currentGeneral().features.fixes.middleDebug) {
            val message="Middle Click Override: slot ${slot.index}, left PICKUP -> middle CLONE"
            logger.info(message);client.player?.sendSystemMessage(Component.literal("[solyanka] $message"))
        }
        return result
    }
    private fun draw(g:GuiGraphicsExtractor,text:Component,h:HudSpec) {
        if(!h.enabled) return
        val client=Minecraft.getInstance();val scale=h.scale.toFloat()
        val x=h.x.coerceIn(0,(client.window.guiScaledWidth-client.font.width(text)*scale).toInt().coerceAtLeast(0))
        val y=h.y.coerceIn(0,(client.window.guiScaledHeight-9*scale).toInt().coerceAtLeast(0))
        g.pose().pushMatrix()
        try { g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);g.text(client.font,text,0,0,h.color,true) }
        finally { g.pose().popMatrix() }
    }
    private fun render(g:GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        if(client.player==null || client.level==null || client.gui.hud.isHidden) return
        val screen=client.gui.screen()
        if(screen!=null && screen !is ChatScreen && screen !is dev.solyanka.client.ui.nofrills.NoFrillsClickGui) return
        val f=SolyankaClient.currentGeneral().features
        if(SolyankaClient.isEnabled(SolyankaClient.TICK_TIMERS) && context.boss) {
            val lines=timers.lines(f.timers)
            lines.forEach { (id,line) ->
                val text=Component.literal(line.prefix).withColor(line.prefixColor).append(Component.literal(line.value).withColor(line.color))
                draw(g,text,f.timers.huds().getValue(id))
            }
        }
        if(SolyankaClient.isEnabled(SolyankaClient.PLAYER_DISPLAY) && context.skyblock) {
            val data=stats
            data.lines(f.player).forEach { (id,text) -> draw(g,Component.literal(text),f.player.huds().getValue(id)) }
        }
    }
    fun register() {
        ClientReceiveMessageEvents.MODIFY_GAME.register { message,overlay ->
            if(overlay) {
                context=readContext(Minecraft.getInstance())
                if(context.skyblock && SolyankaClient.isEnabled(SolyankaClient.SKILL_HUD)) ExtraHuds.skills.overlay(message.string)
                if(context.skyblock && SolyankaClient.isEnabled(SolyankaClient.PLAYER_DISPLAY)) {
                    stats.overlay(message.string)
                    val before=ChatLogic.plain(message.string)
                    val after=PlayerStats.strip(before,SolyankaClient.currentGeneral().features.player)
                    if(after!=before) Component.literal(after) else message
                } else message
            } else {
                context=readContext(Minecraft.getInstance())
                if(context.dungeon && SolyankaClient.isEnabled(SolyankaClient.TICK_TIMERS)) timers.message(message.string)
                message
            }
        }
        fun hide(id:Identifier,predicate:()->Boolean) { HudElementRegistry.replaceElement(id) { original -> HudElement { g,d -> if(!predicate()) original.extractRenderState(g,d) } } }
        fun playerEnabled()=SolyankaClient.isEnabled(SolyankaClient.PLAYER_DISPLAY) && context.skyblock
        hide(VanillaHudElements.ARMOR_BAR) { playerEnabled() && SolyankaClient.currentGeneral().features.player.hideArmor }
        hide(VanillaHudElements.HEALTH_BAR) { playerEnabled() && SolyankaClient.currentGeneral().features.player.hideHearts }
        hide(VanillaHudElements.FOOD_BAR) { playerEnabled() && SolyankaClient.currentGeneral().features.player.hideFood }
        hide(VanillaHudElements.EXPERIENCE_LEVEL) { playerEnabled() && SolyankaClient.currentGeneral().features.player.hideXp }
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","odin_huds")) { g,_ -> render(g) }
    }
}
