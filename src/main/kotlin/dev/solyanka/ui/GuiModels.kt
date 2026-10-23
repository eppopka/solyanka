/* SPDX-License-Identifier: GPL-3.0-only
 * GUI behavior/structure adapted from NoFrills, WhatYouThing and contributors.
 * Source: 74e9317c1a7b8484a575b2f177479f5e3f2bfb9a; see NOTICE. */
package dev.solyanka.ui

import dev.solyanka.utility.*

data class FeatureCardInfo(val id: String, val name: String, val category: String, val description: String)
object GuiCatalog {
    val features = listOf(
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
