/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import dev.solyanka.client.feature.ArchiveHuds
import dev.solyanka.client.feature.ArchiveVisuals
import dev.solyanka.client.feature.DungeonPuzzles
import dev.solyanka.client.feature.Experiments
import dev.solyanka.client.feature.ExtraHuds
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.client.feature.HeldItemCosmetic
import dev.solyanka.client.feature.SecretCounter
import dev.solyanka.client.feature.SkyboxVisual
import dev.solyanka.client.feature.TemporalBlur
import dev.solyanka.client.feature.TitaniumScanner
import dev.solyanka.client.feature.WorldVisuals
import dev.solyanka.core.*

object ClientModuleCatalog {
    fun register(modules: ModuleRegistry) {
        modules.register(object : Module(ModuleInfo(ModuleIds.KEYBINDS, "Command keybinds", Category.GENERAL)) {
            override fun onReset(reason: ResetReason) { InputRouter.clear() }
        })
        modules.register(object : Module(ModuleInfo(ModuleIds.SHORTCUTS, "Command shortcuts", Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.CHAT,"Chat tweaks",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.TOOLTIPS,"Info tooltips",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.TICK_TIMERS,"Tick timers (Odin)",Category.DUNGEON)) { override fun onDisable() { FeatureRuntime.timers.reset() } })
        modules.register(object : Module(ModuleInfo(ModuleIds.PLAYER_DISPLAY,"Player display (Odin)",Category.HUD)) { override fun onDisable() { FeatureRuntime.stats.reset() } })
        modules.register(object : Module(ModuleInfo(ModuleIds.PERFORMANCE,"Performance",Category.HUD)) {})
        listOf(ModuleIds.INVENTORY_HUD to "Inventory HUD",ModuleIds.ITEM_NAME_HUD to "Item Name HUD",ModuleIds.SKILL_HUD to "Skill Progress HUD").forEach {(id,name)->
            modules.register(object : Module(ModuleInfo(id,name,Category.HUD)) {override fun onDisable() {if(id==ModuleIds.SKILL_HUD) ExtraHuds.reset()}})
        }
        modules.register(object : Module(ModuleInfo(ModuleIds.ANIMATION,"Animation fix",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.MIDDLE_FIX,"Middle click fix",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.MIDDLE_OVERRIDE,"Middle click override",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.CURSOR,"No cursor reset",Category.GENERAL)) { override fun onDisable() { FeatureRuntime.cursor.reset() } })
        modules.register(object : Module(ModuleInfo(ModuleIds.LOADING,"No loading screen",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.PRICES,"Price tooltips",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.SPREAD_GUESSER,"Spread Guesser",Category.GENERAL)) { override fun onDisable() { dev.solyanka.client.feature.SpreadGuesser.reset() }; override fun onReset(reason:ResetReason) { dev.solyanka.client.feature.SpreadGuesser.reset() } })
        modules.register(object : Module(ModuleInfo(ModuleIds.EXPERIMENTS,"Experiment solver",Category.GENERAL)) { override fun onDisable() { Experiments.reset() } })
        modules.register(object : Module(ModuleInfo(ModuleIds.SUPPRESSION,"No render",Category.VISUAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.WAYPOINTS,"Waypoints",Category.VISUAL)) {override fun onDisable() {DungeonPuzzles.invalidate()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.LEAP_MESSAGE,"Leap Message+",Category.DUNGEON)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.CHAT_COMMANDS,"Chat Commands",Category.GENERAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.MUSIC,"Yandex Music",Category.HUD)) {override fun onDisable() {dev.solyanka.client.feature.YandexMusic.disconnect()}})
        listOf(ModuleIds.DOORS to "Door Highlight",ModuleIds.MAP_INFO to "Map Info",ModuleIds.ICE to "Ice Fill",ModuleIds.BLAZE to "Blaze",ModuleIds.BEAMS to "Creeper Beams",ModuleIds.MIMIC to "Mimic detection",ModuleIds.MIMIC_CHEST to "Mimic Chest (archive)").forEach { (id,name) -> modules.register(object : Module(ModuleInfo(id,name,Category.DUNGEON)) {override fun onDisable() {DungeonPuzzles.invalidate()}}) }
        listOf(ModuleIds.AMBIENCE to "Ambience",ModuleIds.ASPECT to "Aspect",ModuleIds.FOG to "Fog").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.VISUAL)) {})}
        listOf(ModuleIds.PLAYER_GLOW to "Player Glow",ModuleIds.NAMETAGS to "Nametags",ModuleIds.BLOCK_OUTLINE to "Block Outline",ModuleIds.TITANIUM to "Titanium ESP").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.VISUAL)) {override fun onDisable() {WorldVisuals.reset();if(id==ModuleIds.TITANIUM) TitaniumScanner.reset()}})}
        listOf(ModuleIds.SCOREBOARD to "Scoreboard",ModuleIds.THEMED_CHAT to "Themed Chat").forEach {(id,name)->modules.register(object : Module(ModuleInfo(id,name,Category.HUD)) {})}
        modules.register(object : Module(ModuleInfo(ModuleIds.ITEM_AURA,"Item Aura",Category.VISUAL)) {override fun onDisable() {dev.solyanka.client.feature.ItemAura.clear()};override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.ItemAura.clear()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.CS2_KNIFE,"CS2 Knives",Category.VISUAL)) {override fun onEnable() {dev.solyanka.client.cs2.Cs2Knives.reload()};override fun onDisable() {dev.solyanka.client.cs2.Cs2Knives.clear()};override fun onReset(reason:ResetReason) {dev.solyanka.client.cs2.Cs2Viewmodel.reset()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.HELD_ITEM,"Held Item Shader",Category.VISUAL)) {override fun onEnable() {HeldItemCosmetic.retry()};override fun onDisable() {HeldItemCosmetic.suspend()};override fun onReset(reason:ResetReason) {HeldItemCosmetic.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.SKYBOX,"Skybox",Category.VISUAL)) {override fun onEnable() {SkyboxVisual.retry()};override fun onDisable() {SkyboxVisual.requestCleanup()};override fun onReset(reason:ResetReason) {SkyboxVisual.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.MOTION_BLUR,"Motion Blur",Category.VISUAL)) {override fun onEnable() {TemporalBlur.retry()};override fun onDisable() {TemporalBlur.requestCleanup()};override fun onReset(reason:ResetReason) {TemporalBlur.requestCleanup()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.STOPWATCH,"Stopwatch",Category.HUD)) {override fun onDisable() {ArchiveHuds.resetTimer()};override fun onReset(reason:ResetReason) {ArchiveHuds.resetTimer()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.DVD,"ModuleIds.DVD Bounce",Category.HUD)) {override fun onDisable() {ArchiveHuds.resetDvd()};override fun onReset(reason:ResetReason) {ArchiveHuds.resetDvd()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.AMBIENT_PARTICLES,"Ambient Particles",Category.VISUAL)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.ROTATING_ITEM,"Rotating Item",Category.VISUAL)) {override fun onEnable() {ArchiveVisuals.resetRotation()};override fun onDisable() {ArchiveVisuals.resetRotation()};override fun onReset(reason:ResetReason) {ArchiveVisuals.resetRotation()}})
        modules.register(object : Module(ModuleInfo(ModuleIds.MOVE_HEARTS,"Move Hearts",Category.HUD)) {})
        modules.register(object : Module(ModuleInfo(ModuleIds.SECRET_COUNTER,"Secret Counter",Category.DUNGEON)) {override fun onEnable() {SecretCounter.reset()};override fun onDisable() {SecretCounter.reset()};override fun onReset(reason:ResetReason) {SecretCounter.reset()}})
        listOf(ModuleIds.BESTIARY to "Bestiary ESP",ModuleIds.INVISIBUG to "Invisibug ESP",ModuleIds.TIKI to "Tiki ESP",ModuleIds.CHINA_HAT to "ChinaHat",ModuleIds.HALO to "Halo").forEach {(id,name)->
            modules.register(object:Module(ModuleInfo(id,name,if(id in setOf(ModuleIds.CHINA_HAT,ModuleIds.HALO)) Category.VISUAL else Category.ESP)) {
                override fun onDisable() {dev.solyanka.client.feature.HuntingFeatures.reset()}
                override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.HuntingFeatures.reset()}
            })
        }
        modules.register(object:Module(ModuleInfo(ModuleIds.PUZZLE_SOLVERS,"Puzzle Solvers",Category.DUNGEON)) {
            override fun onDisable() {DungeonPuzzles.invalidate();dev.solyanka.client.feature.SimonSolver.reset()}
        })
        modules.register(object:Module(ModuleInfo(ModuleIds.SIMON,"Simon Says",Category.DUNGEON)) {
            override fun onDisable() {dev.solyanka.client.feature.SimonSolver.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.SimonSolver.reset()}
        })
        modules.register(object:Module(ModuleInfo(ModuleIds.PET,"Pet Display",Category.HUD)) {
            override fun onDisable() {dev.solyanka.client.feature.PetDisplay.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.PetDisplay.reset()}
        })
        modules.register(object:Module(ModuleInfo(ModuleIds.PIP,"Picture in Picture",Category.HUD)) {
            override fun onDisable() {dev.solyanka.client.feature.PictureInPicture.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.PictureInPicture.reset()}
        })
        modules.register(object:Module(ModuleInfo(ModuleIds.MOVE_HOTBAR,"Move Hotbar",Category.HUD)) {})
        modules.register(object:Module(ModuleInfo(ModuleIds.BLOCKLIST,"ShitterList",Category.DUNGEON)) {
            override fun onDisable() {dev.solyanka.client.feature.ShitterList.reset()}
            override fun onReset(reason:ResetReason) {dev.solyanka.client.feature.ShitterList.reset()}
        })
    }
}
