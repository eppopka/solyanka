/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import dev.solyanka.config.ConfigStore
import dev.solyanka.core.*
import dev.solyanka.utility.*
import dev.solyanka.client.ui.ConfigUi
import dev.solyanka.client.feature.InfoTooltips
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.client.feature.TemporalBlur
import dev.solyanka.client.feature.SecretCounter
import dev.solyanka.client.feature.ArchiveVisuals
import dev.solyanka.client.feature.VanillaHearts
import dev.solyanka.client.feature.ArchiveHuds
import dev.solyanka.client.feature.SkyboxVisual
import dev.solyanka.client.feature.HeldItemCosmetic
import dev.solyanka.client.feature.ScoreboardHud
import dev.solyanka.client.feature.WorldVisuals
import dev.solyanka.client.feature.TitaniumScanner
import dev.solyanka.client.feature.ExtraHuds
import dev.solyanka.client.feature.Experiments
import dev.solyanka.client.feature.PriceTooltips
import dev.solyanka.client.feature.Suppression
import dev.solyanka.client.feature.DungeonPuzzles
import dev.solyanka.client.feature.DungeonMap
import dev.solyanka.client.feature.DungeonWaypoints
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object SolyankaClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("solyanka")
    private val modules = ModuleRegistry { id, error -> logger.error("Module failure: $id", error) }
    private val config by lazy { ConfigStore(FabricLoader.getInstance().configDir.resolve("solyanka.json")) {
        message, error -> logger.error(message, error)
    } }
    private var initialized = false
    private var openHudRequested = false
    private var openSettingsRequested = false
    private var previousWorld: Any? = null
    private var profile = CommandProfile()
    private var general = GeneralSettings()
    private lateinit var settingsKey: KeyMapping
    private lateinit var quickToggleKey: KeyMapping
    const val KEYBINDS = ModuleIds.KEYBINDS
    const val SHORTCUTS = ModuleIds.SHORTCUTS
    const val SPREAD_GUESSER = ModuleIds.SPREAD_GUESSER

    const val BLOCKLIST=ModuleIds.BLOCKLIST
    const val MOVE_HOTBAR=ModuleIds.MOVE_HOTBAR
    const val PIP=ModuleIds.PIP
    const val PET=ModuleIds.PET
    const val SIMON=ModuleIds.SIMON
    const val BESTIARY=ModuleIds.BESTIARY
    const val INVISIBUG=ModuleIds.INVISIBUG
    const val TIKI=ModuleIds.TIKI
    const val CHINA_HAT=ModuleIds.CHINA_HAT
    const val HALO=ModuleIds.HALO
    const val SECRET_COUNTER=ModuleIds.SECRET_COUNTER
    const val AMBIENT_PARTICLES=ModuleIds.AMBIENT_PARTICLES
    const val ROTATING_ITEM=ModuleIds.ROTATING_ITEM
    const val MOVE_HEARTS=ModuleIds.MOVE_HEARTS
    const val MOTION_BLUR=ModuleIds.MOTION_BLUR
    const val STOPWATCH=ModuleIds.STOPWATCH
    const val DVD=ModuleIds.DVD
    const val SKYBOX=ModuleIds.SKYBOX
    const val ITEM_AURA=ModuleIds.ITEM_AURA
    const val CS2_KNIFE=ModuleIds.CS2_KNIFE
    const val HELD_ITEM=ModuleIds.HELD_ITEM
    const val SCOREBOARD=ModuleIds.SCOREBOARD
    const val THEMED_CHAT=ModuleIds.THEMED_CHAT
    const val PLAYER_GLOW=ModuleIds.PLAYER_GLOW
    const val NAMETAGS=ModuleIds.NAMETAGS
    const val BLOCK_OUTLINE=ModuleIds.BLOCK_OUTLINE
    const val TITANIUM=ModuleIds.TITANIUM
    const val CHAT = ModuleIds.CHAT
    const val TOOLTIPS = ModuleIds.TOOLTIPS
    const val TICK_TIMERS = ModuleIds.TICK_TIMERS
    const val PLAYER_DISPLAY = ModuleIds.PLAYER_DISPLAY
    const val PERFORMANCE = ModuleIds.PERFORMANCE
    const val INVENTORY_HUD=ModuleIds.INVENTORY_HUD
    const val ITEM_NAME_HUD=ModuleIds.ITEM_NAME_HUD
    const val SKILL_HUD=ModuleIds.SKILL_HUD
    const val ANIMATION = ModuleIds.ANIMATION
    const val MIDDLE_FIX = ModuleIds.MIDDLE_FIX
    const val MIDDLE_OVERRIDE = ModuleIds.MIDDLE_OVERRIDE
    const val CURSOR = ModuleIds.CURSOR
    const val AMBIENCE=ModuleIds.AMBIENCE
    const val ASPECT=ModuleIds.ASPECT
    const val FOG=ModuleIds.FOG
    const val WAYPOINTS=ModuleIds.WAYPOINTS
    const val LEAP_MESSAGE=ModuleIds.LEAP_MESSAGE
    const val CHAT_COMMANDS=ModuleIds.CHAT_COMMANDS
    const val MUSIC=ModuleIds.MUSIC
    const val DOORS=ModuleIds.DOORS
    const val MAP_INFO=ModuleIds.MAP_INFO
    const val PUZZLE_SOLVERS=ModuleIds.PUZZLE_SOLVERS
    const val ICE=ModuleIds.ICE
    const val BLAZE=ModuleIds.BLAZE
    const val BEAMS=ModuleIds.BEAMS
    const val MIMIC=ModuleIds.MIMIC
    const val MIMIC_CHEST=ModuleIds.MIMIC_CHEST
    const val PRICES = ModuleIds.PRICES
    const val EXPERIMENTS = ModuleIds.EXPERIMENTS
    const val SUPPRESSION = ModuleIds.SUPPRESSION
    const val LOADING = ModuleIds.LOADING
    internal fun requestSettings() {openSettingsRequested=true}
    internal fun requestHud() {openHudRequested=true}
    fun currentGeneral() = general
    @JvmStatic fun chatLimit(original:Int) = if(isEnabled(CHAT) && general.chat.extraLines) general.chat.lines else original
    @JvmStatic fun keepChatHistory() = isEnabled(CHAT) && general.chat.keepHistory

    fun currentProfile() = profile
    fun isEnabled(id: String) = PuzzleSolverModules.active(id,modules.get(id)?.enabled == true,modules.get(PUZZLE_SOLVERS)?.enabled == true)

    override fun onInitializeClient() {
        dev.solyanka.client.cs2.render.KnifeRender.register()
        HeldItemCosmetic.register() // Pipeline metadata is known before the first shader resource reload.
        ClientModuleCatalog.register(modules)
        config.load()
        profile = config.profile()
        general = config.general()
        InfoTooltips.register()
        FeatureRuntime.register()
        dev.solyanka.client.feature.PictureInPicture.register();dev.solyanka.client.feature.PetDisplay.register();dev.solyanka.client.feature.ShitterList.register();SecretCounter.register();dev.solyanka.client.feature.SimonSolver.register()
        ArchiveHuds.register();ExtraHuds.register();WorldVisuals.register();ScoreboardHud.register();dev.solyanka.client.feature.PerformanceHud.register()
        PriceTooltips.register()
        Suppression.register()
        dev.solyanka.client.feature.VanillaHotbar.register();VanillaHearts.register() // Wrap existing visibility filters; never bypass their decisions.
        DungeonPuzzles.register();DungeonMap.register();DungeonWaypoints.register();dev.solyanka.client.feature.LeapMessage.register();dev.solyanka.client.feature.ChatCommands.register();dev.solyanka.client.feature.YandexMusic.register()
        modules.all().forEach { module ->
            try { module.setEnabled(if(module.info.id==PUZZLE_SOLVERS) PuzzleSolverModules.masterEnabled(config.configured(PUZZLE_SOLVERS),PuzzleSolverModules.children.any(config::enabled)) else config.enabled(module.info.id)) }
            catch (e: Exception) { logger.error("Cannot enable ${module.info.id}", e) }
        }
        val keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("solyanka", "general"))
        settingsKey = KeyMappingHelper.registerKeyMapping(KeyMapping("key.solyanka.settings", GLFW.GLFW_KEY_RIGHT_SHIFT, keyCategory))
        quickToggleKey = KeyMappingHelper.registerKeyMapping(KeyMapping("key.solyanka.quick_toggle", GLFW.GLFW_KEY_UNKNOWN, keyCategory))
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            dev.solyanka.client.cs2.Cs2Knives.tick(client);dev.solyanka.client.feature.PictureInPicture.tick();TemporalBlur.tick();ArchiveHuds.tick();SkyboxVisual.tick()
            val world = client.level
            dev.solyanka.client.feature.HuntingFeatures.bestiaryWorld(world)
            if (world !== previousWorld) {
                if(world==null) FeatureRuntime.flushHudProfile()
                modules.reset(if (world == null) ResetReason.DISCONNECTED else ResetReason.WORLD_CHANGED)
                InputRouter.clear()
                dev.solyanka.client.feature.PerformanceHud.reset();FeatureRuntime.reset();Experiments.reset();DungeonPuzzles.reset();DungeonMap.reset();DungeonWaypoints.reset();WorldVisuals.reset();TitaniumScanner.reset()
                previousWorld = world
            }
            while (quickToggleKey.consumeClick()) {
                if(client.gui.screen()==null && client.isWindowActive && client.player!=null) {
                    val x=client.mouseHandler.getScaledXPos(client.window).toInt();val y=client.mouseHandler.getScaledYPos(client.window).toInt()
                    client.setScreenAndShow(dev.solyanka.client.ui.QuickToggleScreen(quickToggleKey,x,y))
                }
            }
            while (settingsKey.consumeClick()) {
                if (client.gui.screen() == null && client.isWindowActive) openSettingsRequested = true
            }
            if (openHudRequested) {
                openHudRequested=false
                client.setScreenAndShow(dev.solyanka.client.ui.HudEditorScreen())
            }
            if (openSettingsRequested) {
                openSettingsRequested = false
                client.setScreenAndShow(ConfigUi.create(null))
            }
            if (world != null && client.player != null) { FeatureRuntime.tick(client);dev.solyanka.client.feature.SpreadGuesser.tick(client);dev.solyanka.client.feature.ShitterList.tick();dev.solyanka.client.feature.PetDisplay.tick();dev.solyanka.client.feature.SimonSolver.tick();dev.solyanka.client.feature.HuntingFeatures.tick(client);SecretCounter.tick(); ArchiveVisuals.tick(client);ExtraHuds.tick(client);WorldVisuals.reset();TitaniumScanner.tick(client); modules.tick(); InfoTooltips.tick();Experiments.update();PriceTooltips.tick();DungeonPuzzles.tick();DungeonMap.tick() }
        }
        InputRouter.register()
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            FeatureRuntime.flushHudProfile()
            config.save(states(), profile, general)
            dev.solyanka.client.feature.YandexMusic.disconnect();dev.solyanka.client.cs2.Cs2Knives.shutdown();modules.shutdown(); dev.solyanka.client.feature.ShitterList.close(); dev.solyanka.client.feature.PictureInPicture.close();TemporalBlur.close();ArchiveHuds.resetTimer();ArchiveHuds.resetDvd(); SkyboxVisual.close(); HeldItemCosmetic.close(); FeatureRuntime.reset();Experiments.reset();DungeonPuzzles.reset();DungeonMap.reset();DungeonWaypoints.reset();WorldVisuals.reset();TitaniumScanner.reset(); InputRouter.clear(); previousWorld = null; initialized = false
        }
        Commands.register()
        initialized = true
        logger.info("solyanka {} initialized for Minecraft 26.2",ModInfo.version)
    }

    /** Validate and persist first; do not activate an invalid or unwritable UI draft. */
    private val recentlyChanged=dev.solyanka.model.RecentModules()
    fun recentModules():List<String> = recentlyChanged.snapshot()
    fun applySettings(next: CommandProfile, enabled: Map<String,Boolean>, nextGeneral:GeneralSettings=general): String? {
        try {
            CommandRules.validate(next)
            nextGeneral.validate()
            if (!config.save(enabled, next, nextGeneral)) return "Settings not saved; check config permissions/schema and latest.log."
            profile = next
            general = nextGeneral
            val before=states()
            val failures=modules.applyEnabled(enabled)
            states().forEach { (id,value)->if(before[id]!=value) recentlyChanged.record(id) }
            if(failures.isNotEmpty()) {
                // Persist actual states after failed hooks, so restarts do not silently retry a broken transition.
                val reconciled=config.save(states(),profile,general)
                val detail=failures.joinToString(", ")
                return if(reconciled) "Settings saved, but module change failed: $detail. See latest.log."
                else "Settings saved, but module change failed: $detail; actual states could not be saved. See latest.log."
            }
            return null
        } catch (e: Exception) {
            logger.error("Cannot apply settings",e)
            return "Settings not saved: ${e.message ?: "Cannot apply settings"}"
        }
    }
    fun states(): Map<String,Boolean> = modules.all().associate { it.info.id to it.enabled }

    internal fun isInitialized() = initialized
    internal fun disableFailedKeybind() {modules.get(KEYBINDS)?.setEnabled(false)}
    @JvmStatic fun handleInput(window:Long,action:Int,device:InputDevice,key:Int,modifiers:Int):Boolean =
        InputRouter.handleInput(window,action,device,key,modifiers)
}
