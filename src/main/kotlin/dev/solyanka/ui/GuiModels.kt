/* SPDX-License-Identifier: GPL-3.0-only
 * GUI behavior/structure adapted from NoFrills, WhatYouThing and contributors.
 * Source: 74e9317c1a7b8484a575b2f177479f5e3f2bfb9a; see NOTICE. */
package dev.solyanka.ui

import dev.solyanka.utility.*

data class FeatureCardInfo(val id: String, val name: String, val category: String, val description: String)
object GuiCatalog {
    val features = listOf(
        FeatureCardInfo("hud.scoreboard","Scoreboard","HUD","Styled sidebar with team objectives, formatted scores and movable layout."),
        FeatureCardInfo("hud.themed_chat","Themed Chat","HUD","Rounded message pane and input styling; vanilla chat text and interactions retained."),
        FeatureCardInfo("render.player_glow","Player Glow","Render","Native player silhouette outlines; optional visible-only mode."),
        FeatureCardInfo("render.nametags","Nametags","Render","Player name, health and distance panels; real-account filter."),
        FeatureCardInfo("render.block_outline","Block Outline","Render","Selected voxel shape contour and fill; no blur shader."),
        FeatureCardInfo("mining.titanium","Titanium ESP","Render","Loaded-chunk titanium scanner with commission and region filters."),
        FeatureCardInfo("hud.inventory","Inventory HUD","HUD","Stray: inventory grid, optional armor/offhand and hotbar, filled count; display only."),
        FeatureCardInfo("hud.item_name","Item Name HUD","HUD","Stray: persistent styled held-item name and optional vanilla-name suppression."),
        FeatureCardInfo("hud.skill_progress","Skill Progress HUD","HUD","Stray: SkyBlock skill gains, ratio/percent/MAXED and observed level hints; no network requests."),
        FeatureCardInfo("render.ambience","Ambience","Render","Stray: client clock, weather, snowy precipitation and intensity overrides; no server time changes."),
        FeatureCardInfo("render.aspect","Aspect","Render","Stray: matched horizontal projection and entity/terrain culling aspect multiplier."),
        FeatureCardInfo("render.fog","Fog","Render","Stray: atmospheric fog distances, color and density; preserves liquid/status fog. No Render wins."),
        FeatureCardInfo("dungeon.waypoints","Dungeon Waypoints","Dungeon","Odin-compatible local packs, room rotation, titles, secret/etherwarp hiding and manual editor."),
        FeatureCardInfo("dungeon.door_highlight","Door Highlight","Dungeon","Odin: Magical Map doors, chat-tracked keys, key spawn alerts and openable colors."),
        FeatureCardInfo("dungeon.map_info","Map Info","Dungeon","Odin: secrets, estimated score, crypts, deaths, bonus flags, room secrets and portal highlight."),
        FeatureCardInfo("solver.ice_fill","Ice Fill","Solver","Odin: room-core identification, rotation and bundled easy/optimized Ice Fill routes."),
        FeatureCardInfo("solver.blaze","Blaze","Solver","Odin: Higher/Lower Blaze health ordering, three priority colors and connecting lines."),
        FeatureCardInfo("solver.creeper_beams","Creeper Beams","Solver","Odin: rotated sea-lantern pairs from bundled solutions, refreshed as blocks change."),
        FeatureCardInfo("dungeon.mimic","Mimic Detection","Dungeon","Odin: baby-zombie death, prince and bat detection, optional party announcements; baby-zombie highlights."),
        FeatureCardInfo("custom.mimic_chest","Mimic Chest","Dungeon","impl.zip: trapped-chest highlights, loaded chunks only; no forced chunk loads."),
        FeatureCardInfo("utility.price_tooltips","Price Tooltips","General","NoFrills: market prices, observed crafting recipes and local purchase costs. Online data is opt-in."),
        FeatureCardInfo("solver.experiments","Experiment Solver","Solver","Chronomatron, Ultrasequencer and Superpairs memory overlays, next-click hints and optional wrong-click prevention."),
        FeatureCardInfo("render.suppression","No Render","Render","Independent particle, entity, overlay, fog, recipe-book and HUD suppression filters."),
        FeatureCardInfo("dungeon.tick_timers","Tick Timers","Dungeon","Odin: Goldor, Storm pad/lightning/PY/phase and M3 Fire Freeze server-ping timers."),
        FeatureCardInfo("hud.player_display","Player Display","HUD","Odin: health, mana, overflow, defense, vitality, EHP, speed and vanilla/action-bar visibility."),
        FeatureCardInfo("render.animation_fix","Animation Fix","General","NoFrills: preserve local player pose from server metadata resets; optional SkyBlock-only guard."),
        FeatureCardInfo("utility.middle_click_fix","Middle Click Fix","General","NoFrills: allow pick-item CLONE clicks in survival container screens."),
        FeatureCardInfo("utility.middle_click_override","Middle Click Override","General","NoFrills: convert eligible manual menu left clicks to middle CLONE, with menu/transaction/loadout guards."),
        FeatureCardInfo("utility.cursor","No Cursor Reset","General","NoFrills: remember GUI cursor position across close/reopen, with configurable in-world expiry."),
        FeatureCardInfo("utility.loading_screen","No Loading Screen","General","Hide terrain-loading visuals only; keep vanilla loading readiness and controls. Optional remote-server-only guard."),

        FeatureCardInfo("utility.chat","Chat Tweaks","General","Copy hovered messages or lines, keep history on disconnect and expand history limits."),
        FeatureCardInfo("utility.info_tooltips","Info Tooltips","General","Dungeon quality, creation date, dye hex, museum category and market item IDs."),
        FeatureCardInfo("utility.command_keybinds","Command Keybinds","General","Create keyboard and mouse binds for commands or chat. Modifier, GUI, island and dungeon class filters."),
        FeatureCardInfo("utility.command_shortcuts","Command Shortcuts","General","Create command aliases with arguments, live expansion and cycle protection."),
        FeatureCardInfo("core.diagnostics","Lifecycle Diagnostics","Core","Debug module lifecycle, tick and world reset events.")
    )
    fun search(query: String): Map<String,List<FeatureCardInfo>> {
        fun normalized(value: String) = value.lowercase().filterNot { it.isWhitespace() }
        val term=normalized(query)
        return features.filter { term.isEmpty() || normalized(it.name+" "+it.description).contains(term) }
            .groupBy { it.category }.mapValues { (_,cards) -> cards.sortedBy { it.name } }
    }
}

/** A shared, immutable-row draft for nested list/editor screens. No disk/network access. */
class CommandGuiDraft(initial: CommandProfile) {
    private val original=initial
    var profile: CommandProfile=initial
        private set
    fun changed() = profile != original
    fun allowAllInGui(value: Boolean) { profile=profile.copy(allowAllInGui=value) }
    fun addBind(): CommandBinding {
        require(profile.binds.size < 128) { "Maximum 128 keybinds" }
        val bind=CommandBinding();profile=profile.copy(binds=profile.binds+bind);return bind
    }
    fun addShortcut(): Shortcut {
        require(profile.shortcuts.size < 128) { "Maximum 128 shortcuts" }
        val shortcut=Shortcut();profile=profile.copy(shortcuts=profile.shortcuts+shortcut);return shortcut
    }
    fun updateBind(id: String, change: (CommandBinding)->CommandBinding) {
        require(profile.binds.any { it.id==id }) { "Unknown keybind" }
        profile=profile.copy(binds=profile.binds.map { if(it.id==id) change(it).also { n -> require(n.id==id) } else it })
    }
    fun updateShortcut(id: String, change: (Shortcut)->Shortcut) {
        require(profile.shortcuts.any { it.id==id }) { "Unknown shortcut" }
        profile=profile.copy(shortcuts=profile.shortcuts.map { if(it.id==id) change(it).also { n -> require(n.id==id) } else it })
    }
    fun removeBind(id: String) { profile=profile.copy(binds=profile.binds.filterNot { it.id==id }) }
    fun removeShortcut(id: String) { profile=profile.copy(shortcuts=profile.shortcuts.filterNot { it.id==id }) }
    fun validated(): CommandProfile { CommandRules.validate(profile);return profile }
}
