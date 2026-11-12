/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.feature.ChatTweaks
import dev.solyanka.client.feature.DungeonWaypoints
import dev.solyanka.client.feature.ArchiveHuds
import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

/** Input ownership and alias expansion, isolated from client bootstrap. */
object InputRouter {
 private val logger=LoggerFactory.getLogger("solyanka/input")
 private var bypassExpansion=false
 private val ownedInputs=hashSetOf<Pair<InputDevice,Int>>()
 fun clear() {ownedInputs.clear()}
 fun register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register { command ->
            if (bypassExpansion || !S.isEnabled(ModuleIds.SHORTCUTS)) true
            else {
                val effective = routingProfile()
                if (!CommandResolver.isAliasInvocation("/$command",effective)) true
                else when (val result = CommandResolver.resolve("/$command", effective)) {
                    is Resolution.Error -> { error(result.reason); false }
                    is Resolution.Message -> if (!result.expanded) true else { transmit(result.text); false }
                }
            }
        }
 }
    fun handleInput(window: Long, action: Int, device: InputDevice, key: Int, modifiers: Int): Boolean {
        if (!S.isInitialized()) return false
        val client = Minecraft.getInstance()
        if (client.window.handle() != window) return false
        val token = device to key
        if (action == GLFW.GLFW_RELEASE) return ownedInputs.remove(token)
        if (action == GLFW.GLFW_REPEAT) return token in ownedInputs
        if (action != GLFW.GLFW_PRESS) return false
        try {
            if(ChatTweaks.copy(client,device,key)) { ownedInputs.add(token);return true }
        } catch(e:Exception) { logger.error("Chat copy failure",e) }
        if (DungeonWaypoints.editorInput(device,key)) {ownedInputs.add(token);return true}
        if (ArchiveHuds.input(device,key)) {ownedInputs.add(token);return true}
        if (!S.isEnabled(ModuleIds.KEYBINDS)) return false
        try {
            val screen = client.gui.screen()
            val kind = when (screen) {
                null -> ScreenKind.WORLD
                is AnvilScreen -> ScreenKind.ANVIL
                is AbstractContainerScreen<*> -> ScreenKind.CONTAINER
                else -> ScreenKind.OTHER
            }
            val lines = client.connection?.listedOnlinePlayers?.mapNotNull { it.tabListDisplayName?.string } ?: emptyList()
            val tab = TabContextParser.parse(lines,client.player?.name?.string ?: "")
            val ctx = InputContext(client.player != null && client.level != null, client.isWindowActive, kind,
                screen?.let { textFocused(it) } ?: false,tab.island,tab.dungeonClass)
            val bind = S.currentProfile().binds.firstOrNull { CommandRules.matches(it,S.currentProfile(),device,key,modifiers,ctx) } ?: return false
            ownedInputs.add(token)
            when (val result = CommandResolver.resolve(bind.command,routingProfile(),S.isEnabled(ModuleIds.SHORTCUTS))) {
                is Resolution.Error -> error(result.reason)
                is Resolution.Message -> transmit(result.text)
            }
            return true
        } catch (e: Exception) {
            logger.error("Command keybind failure; module disabled",e)
            S.disableFailedKeybind()
            return token in ownedInputs
        }
    }
    private fun textFocused(node: GuiEventListener): Boolean =
        (node is EditBox && node.isFocused) || (node is ContainerEventHandler && node.children().any { textFocused(it) })

    private fun routingProfile(): CommandProfile {
        val root = ClientCommands.getActiveDispatcher()?.root
        return S.currentProfile().copy(shortcuts=S.currentProfile().shortcuts.filter { root?.getChild(CommandRules.alias(it.alias)) == null })
    }
    private fun transmit(text: String) {
        val connection = Minecraft.getInstance().player?.connection ?: return
        if (bypassExpansion) return
        bypassExpansion = true
        try {
            if (text.startsWith("/")) connection.sendCommand(text.substring(1)) else connection.sendChat(text)
        } finally { bypassExpansion = false }
    }
    private fun error(message: String) {
        logger.warn("Command rejected: {}",message)
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[solyanka] $message"))
    }
}
