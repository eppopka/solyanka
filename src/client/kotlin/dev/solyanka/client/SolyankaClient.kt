/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import dev.solyanka.config.ConfigStore
import dev.solyanka.core.*
import dev.solyanka.utility.*
import dev.solyanka.client.ui.ConfigUi
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import com.mojang.brigadier.arguments.StringArgumentType
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object SolyankaClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("solyanka")
    private val modules = ModuleRegistry { id, error -> logger.error("Module failure: $id", error) }
    private val config by lazy { ConfigStore(FabricLoader.getInstance().configDir.resolve("solyanka.json")) {
        message, error -> logger.error(message, error)
    } }
    private var initialized = false
    private var openSettingsRequested = false
    private var previousWorld: Any? = null
    private var profile = CommandProfile()
    private var bypassExpansion = false
    private val ownedInputs = hashSetOf<Pair<InputDevice,Int>>()
    private lateinit var settingsKey: KeyMapping
    const val KEYBINDS = "utility.command_keybinds"
    const val SHORTCUTS = "utility.command_shortcuts"

    fun currentProfile() = profile
    fun isEnabled(id: String) = modules.get(id)?.enabled == true

    override fun onInitializeClient() {
        modules.register(object : Module(ModuleInfo("core.diagnostics", "Lifecycle diagnostics", Category.CORE)) {
            private var ticks = 0L
            override fun onEnable() { logger.info("Diagnostics enabled") }
            override fun onDisable() { ticks=0; logger.info("Diagnostics disabled") }
            override fun onTick() { ticks++ }
            override fun onReset(reason: ResetReason) { ticks=0; logger.debug("Diagnostics reset: {}",reason) }
        })
        modules.register(object : Module(ModuleInfo(KEYBINDS, "Command keybinds", Category.UTILITY)) {
            override fun onReset(reason: ResetReason) { ownedInputs.clear() }
        })
        modules.register(object : Module(ModuleInfo(SHORTCUTS, "Command shortcuts", Category.UTILITY)) {})
        config.load()
        profile = config.profile()
        modules.all().forEach { module ->
            try { module.setEnabled(config.enabled(module.info.id)) }
            catch (e: Exception) { logger.error("Cannot enable ${module.info.id}", e) }
        }
        settingsKey = KeyMappingHelper.registerKeyMapping(KeyMapping("key.solyanka.settings", GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("solyanka", "general"))))
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val world = client.level
            if (world !== previousWorld) {
                modules.reset(if (world == null) ResetReason.DISCONNECTED else ResetReason.WORLD_CHANGED)
                ownedInputs.clear()
                previousWorld = world
            }
            while (settingsKey.consumeClick()) {
                if (client.gui.screen() == null && client.isWindowActive) openSettingsRequested = true
            }
            if (openSettingsRequested) {
                openSettingsRequested = false
                client.setScreenAndShow(ConfigUi.create(null))
            }
            if (world != null && client.player != null) modules.tick()
        }
        ClientSendMessageEvents.ALLOW_COMMAND.register { command ->
            if (bypassExpansion || !isEnabled(SHORTCUTS)) true
            else {
                val effective = routingProfile()
                if (!CommandResolver.isAliasInvocation("/$command",effective)) true
                else when (val result = CommandResolver.resolve("/$command", effective)) {
                    is Resolution.Error -> { error(result.reason); false }
                    is Resolution.Message -> if (!result.expanded) true else { transmit(result.text); false }
                }
            }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            config.save(states(), profile)
            modules.shutdown(); ownedInputs.clear(); previousWorld = null; initialized = false
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("solyanka")
                .executes {
                    // Open next tick, after vanilla has finished closing the chat screen.
                    openSettingsRequested = true; 1
                }
                .then(literal("config").executes {
                    // Defer opening until after chat submission has finished closing ChatScreen.
                    openSettingsRequested = true; 1
                })
                .then(literal("status").executes { ctx ->
                    modules.all().forEach { ctx.source.sendFeedback(Component.literal("${it.info.id}: ${if (it.enabled) "on" else "off"}")) }
                    ctx.source.sendFeedback(Component.literal("${profile.binds.size} binds, ${profile.shortcuts.size} shortcuts. Other ports: pending.")); 1
                })
                .then(literal("toggle").then(argument("module", StringArgumentType.word()).executes { ctx ->
                    val id = StringArgumentType.getString(ctx, "module")
                    if (modules.get(id) == null) {
                        ctx.source.sendError(Component.literal("Unknown or unimplemented module: $id")); 0
                    } else {
                        val changed = states().toMutableMap().also { it[id] = !isEnabled(id) }
                        val failure = applySettings(profile, changed)
                        if (failure != null) { ctx.source.sendError(Component.literal(failure)); 0 }
                        else { ctx.source.sendFeedback(Component.literal("$id: ${if (isEnabled(id)) "on" else "off"}")); 1 }
                    }
                })))
        }
        initialized = true
        logger.info("solyanka 0.2.1 initialized for Minecraft 26.2")
    }

    /** Validate and persist first; do not activate an invalid or unwritable UI draft. */
    fun applySettings(next: CommandProfile, enabled: Map<String,Boolean>): String? {
        try {
            CommandRules.validate(next)
            if (!config.save(enabled, next)) return "Settings not saved; check config permissions/schema and latest.log."
            profile = next
            modules.all().forEach { it.setEnabled(enabled[it.info.id] ?: it.enabled) }
            return null
        } catch (e: Exception) {
            logger.error("Cannot apply settings",e)
            return e.message ?: "Cannot apply settings"
        }
    }
    fun states(): Map<String,Boolean> = modules.all().associate { it.info.id to it.enabled }

    @JvmStatic fun handleInput(window: Long, action: Int, device: InputDevice, key: Int, modifiers: Int): Boolean {
        if (!initialized) return false
        val client = Minecraft.getInstance()
        if (client.window.handle() != window) return false
        val token = device to key
        if (action == GLFW.GLFW_RELEASE) return ownedInputs.remove(token)
        if (action == GLFW.GLFW_REPEAT) return token in ownedInputs
        if (action != GLFW.GLFW_PRESS || !isEnabled(KEYBINDS)) return false
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
            val bind = profile.binds.firstOrNull { CommandRules.matches(it,profile,device,key,modifiers,ctx) } ?: return false
            ownedInputs.add(token)
            when (val result = CommandResolver.resolve(bind.command,routingProfile(),isEnabled(SHORTCUTS))) {
                is Resolution.Error -> error(result.reason)
                is Resolution.Message -> transmit(result.text)
            }
            return true
        } catch (e: Exception) {
            logger.error("Command keybind failure; module disabled",e)
            modules.get(KEYBINDS)?.setEnabled(false)
            return token in ownedInputs
        }
    }
    private fun textFocused(node: GuiEventListener): Boolean =
        (node is EditBox && node.isFocused) || (node is ContainerEventHandler && node.children().any { textFocused(it) })

    private fun routingProfile(): CommandProfile {
        val root = ClientCommands.getActiveDispatcher()?.root
        return profile.copy(shortcuts=profile.shortcuts.filter { root?.getChild(CommandRules.alias(it.alias)) == null })
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
