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
        modules.register(object : Module(ModuleInfo(KEYBINDS, "Command keybinds", Category.GENERAL)) {
            override fun onReset(reason: ResetReason) { InputRouter.clear() }
        })
        modules.register(object : Module(ModuleInfo(SHORTCUTS, "Command shortcuts", Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(CHAT,"Chat tweaks",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(TOOLTIPS,"Info tooltips",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(TICK_TIMERS,"Tick timers (Odin)",Category.DUNGEON)) { override fun onDisable() { FeatureRuntime.timers.reset() } })
        modules.register(object : Module(ModuleInfo(PLAYER_DISPLAY,"Player display (Odin)",Category.HUD)) { override fun onDisable() { FeatureRuntime.stats.reset() } })
        modules.register(object : Module(ModuleInfo(PERFORMANCE,"Performance",Category.HUD)) {})
        listOf(INVENTORY_HUD to "Inventory HUD",ITEM_NAME_HUD to "Item Name HUD",SKILL_HUD to "Skill Progress HUD").forEach {(id,name)->
            modules.register(object : Module(ModuleInfo(id,name,Category.HUD)) {override fun onDisable() {if(id==SKILL_HUD) ExtraHuds.reset()}})
        }
        modules.register(object : Module(ModuleInfo(ANIMATION,"Animation fix",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(MIDDLE_FIX,"Middle click fix",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(MIDDLE_OVERRIDE,"Middle click override",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(CURSOR,"No cursor reset",Category.GENERAL)) { override fun onDisable() { FeatureRuntime.cursor.reset() } })
        modules.register(object : Module(ModuleInfo(LOADING,"No loading screen",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(PRICES,"Price tooltips",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(SPREAD_GUESSER,"Spread Guesser",Category.GENERAL)) { override fun onDisable() { dev.solyanka.client.feature.SpreadGuesser.reset() }; override fun onReset(reason:ResetReason) { dev.solyanka.client.feature.SpreadGuesser.reset() } })
        modules.register(object : Module(ModuleInfo(EXPERIMENTS,"Experiment solver",Category.GENERAL)) { override fun onDisable() { Experiments.reset() } })
        modules.register(object : Module(ModuleInfo(SUPPRESSION,"No render",Category.VISUAL)) {})
        modules.register(object : Module(ModuleInfo(WAYPOINTS,"Waypoints",Category.VISUAL)) {override fun onDisable() {DungeonPuzzles.invalidate()}})
        modules.register(object : Module(ModuleInfo(LEAP_MESSAGE,"Leap Message+",Category.DUNGEON)) {})
        modules.register(object : Module(ModuleInfo(CHAT_COMMANDS,"Chat Commands",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(MUSIC,"Yandex Music",Category.HUD)) {override fun onDisable() {dev.solyanka.client.feature.YandexMusic.disconnect()}})
        listOf(DOORS to "Door Highlight",MAP_INFO to "Map Info",ICE to "Ice Fill",BLAZE to "Blaze",BEAMS to "Creeper Beams",MIMIC to "Mimic detection",MIMIC_CHEST to "Mimic Chest (archive)").forEach { (id,name) -> modules.register(object : Module(ModuleInfo(id,name,Category.DUNGEON)) {override fun onDisable() {DungeonPuzzles.invalidate()}}) }
        listOf(AMBIENCE to "Ambience",ASPECT to "Aspect",FOG to "Fog").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.VISUAL)) {})}
        listOf(PLAYER_GLOW to "Player Glow",NAMETAGS to "Nametags",BLOCK_OUTLINE to "Block Outline",TITANIUM to "Titanium ESP").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.VISUAL)) {override fun onDisable() {WorldVisuals.reset();if(id==TITANIUM) TitaniumScanner.reset()}})}
        listOf(SCOREBOARD to "Scoreboard",THEMED_CHAT to "Themed Chat").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.HUD)) {})}
        modules.register(object : Module(ModuleInfo(ITEM_AURA,"Item Aura",Category.VISUAL)) {override fun onDisable() {dev.solyanka.client.feature.ItemAura.clear()};override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.ItemAura.clear()}})
        modules.register(object : Module(ModuleInfo(CS2_KNIFE,"CS2 Knives",Category.VISUAL)) {override fun onEnable() {dev.solyanka.client.cs2.Cs2Knives.reload()};override fun onDisable() {dev.solyanka.client.cs2.Cs2Knives.clear()};override fun onReset(reason:ResetReason) {dev.solyanka.client.cs2.Cs2Viewmodel.reset()}})
        modules.register(object : Module(ModuleInfo(HELD_ITEM,"Held Item Shader",Category.VISUAL)) {override fun onEnable() {HeldItemCosmetic.retry()};override fun onDisable() {HeldItemCosmetic.suspend()};override fun onReset(reason:ResetReason) {HeldItemCosmetic.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(SKYBOX,"Skybox",Category.VISUAL)) {override fun onEnable() {SkyboxVisual.retry()};override fun onDisable() {SkyboxVisual.requestCleanup()};override fun onReset(reason:ResetReason) {SkyboxVisual.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(MOTION_BLUR,"Motion Blur",Category.VISUAL)) {override fun onEnable() {TemporalBlur.retry()};override fun onDisable() {TemporalBlur.requestCleanup()};override fun onReset(reason:ResetReason) {TemporalBlur.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(STOPWATCH,"Stopwatch",Category.HUD)) {override fun onDisable() {ArchiveHuds.resetTimer()};override fun onReset(reason:ResetReason) {ArchiveHuds.resetTimer()}})
        modules.register(object : Module(ModuleInfo(DVD,"DVD Bounce",Category.HUD)) {override fun onDisable() {ArchiveHuds.resetDvd()};override fun onReset(reason:ResetReason) {ArchiveHuds.resetDvd()}})
        modules.register(object : Module(ModuleInfo(AMBIENT_PARTICLES,"Ambient Particles",Category.VISUAL)) {})
        modules.register(object : Module(ModuleInfo(ROTATING_ITEM,"Rotating Item",Category.VISUAL)) {override fun onEnable() {ArchiveVisuals.resetRotation()};override fun onDisable() {ArchiveVisuals.resetRotation()};override fun onReset(reason:ResetReason) {ArchiveVisuals.resetRotation()}})
        modules.register(object : Module(ModuleInfo(MOVE_HEARTS,"Move Hearts",Category.HUD)) {})
        modules.register(object : Module(ModuleInfo(SECRET_COUNTER,"Secret Counter",Category.DUNGEON)) {override fun onEnable() {SecretCounter.reset()};override fun onDisable() {SecretCounter.reset()};override fun onReset(reason:ResetReason) {SecretCounter.reset()}})
        listOf(BESTIARY to "Bestiary ESP",INVISIBUG to "Invisibug ESP",TIKI to "Tiki ESP",CHINA_HAT to "ChinaHat",HALO to "Halo").forEach {(id,name)->
            modules.register(object:Module(ModuleInfo(id,name,if(id in setOf(CHINA_HAT,HALO)) Category.VISUAL else Category.ESP)) {
                override fun onDisable() {dev.solyanka.client.feature.HuntingFeatures.reset()}
                override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.HuntingFeatures.reset()}
            })
        }
        modules.register(object:Module(ModuleInfo(PUZZLE_SOLVERS,"Puzzle Solvers",Category.DUNGEON)) {
            override fun onDisable() {DungeonPuzzles.invalidate();dev.solyanka.client.feature.SimonSolver.reset()}
        })
        modules.register(object:Module(ModuleInfo(SIMON,"Simon Says",Category.DUNGEON)) {
            override fun onDisable() {dev.solyanka.client.feature.SimonSolver.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.SimonSolver.reset()}
        })
        modules.register(object:Module(ModuleInfo(PET,"Pet Display",Category.HUD)) {
            override fun onDisable() {dev.solyanka.client.feature.PetDisplay.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.PetDisplay.reset()}
        })
        modules.register(object:Module(ModuleInfo(PIP,"Picture in Picture",Category.HUD)) {
            override fun onDisable() {dev.solyanka.client.feature.PictureInPicture.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.PictureInPicture.reset()}
        })
        modules.register(object:Module(ModuleInfo(MOVE_HOTBAR,"Move Hotbar",Category.HUD)) {})
        modules.register(object:Module(ModuleInfo(BLOCKLIST,"ShitterList",Category.DUNGEON)) {
            override fun onDisable() {dev.solyanka.client.feature.ShitterList.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.ShitterList.reset()}
        })
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
