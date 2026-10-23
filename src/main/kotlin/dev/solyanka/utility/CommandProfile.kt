/* SPDX-License-Identifier: GPL-3.0-only
 * Behavior reference: WhatYouThing/NoFrills, GPLv3; see NOTICE. */
package dev.solyanka.utility

import java.util.UUID

enum class InputDevice { KEYBOARD, MOUSE }
enum class Modifier(val mask: Int) { ANY(-1), NONE(0), SHIFT(1), CTRL(2), ALT(4) }
data class CommandBinding(
    val id: String = UUID.randomUUID().toString(), val name: String = "New keybind",
    val device: InputDevice = InputDevice.KEYBOARD, val key: Int = -1,
    val command: String = "", val enabled: Boolean = false, val allowInGui: Boolean = false,
    val modifier: Modifier = Modifier.ANY,
    val classFilters: List<String> = emptyList(), val islandFilters: List<String> = emptyList()
)
data class Shortcut(val id: String = UUID.randomUUID().toString(), val alias: String = "",
                    val message: String = "", val enabled: Boolean = false)
data class CommandProfile(val allowAllInGui: Boolean = false,
                          val binds: List<CommandBinding> = emptyList(), val shortcuts: List<Shortcut> = emptyList())
data class InputContext(val connected: Boolean, val focused: Boolean, val screen: ScreenKind = ScreenKind.WORLD,
                        val textFocused: Boolean = false, val island: String? = null, val dungeonClass: String? = null)
enum class ScreenKind { WORLD, CONTAINER, ANVIL, OTHER }

object CommandRules {
    const val MAX_MESSAGE_LENGTH = 256
    private val aliasPattern = Regex("[A-Za-z0-9_-]{1,64}")
    fun validateMessage(message: String) {
        require(message.isNotBlank()) { "Message/command is empty" }
        require(message.length <= MAX_MESSAGE_LENGTH) { "Message exceeds 256 characters" }
        require(message.none { it.code < 32 || it.code == 127 || it == '§' }) { "Invalid chat control/format character" }
        require(message.trim() != "/") { "Command is empty" }
    }
    fun alias(value: String) = value.trim().removePrefix("/")
    fun validate(profile: CommandProfile) {
        require(profile.binds.size <= 128 && profile.shortcuts.size <= 128) { "Maximum 128 entries per list" }
        require(profile.binds.map { it.id }.distinct().size == profile.binds.size) { "Duplicate bind IDs" }
        require(profile.shortcuts.map { it.id }.distinct().size == profile.shortcuts.size) { "Duplicate shortcut IDs" }
        profile.binds.forEach {
            require(it.id.isNotBlank() && it.id.length <= 128 && it.name.length <= 128) { "Invalid keybind metadata" }
            require(if (it.device == InputDevice.KEYBOARD) it.key == -1 || it.key in 32..348 else it.key == -1 || it.key in 0..7) { "Invalid input key code" }
            require(it.classFilters.size <= 64 && it.islandFilters.size <= 64) { "Too many filters" }
            require((it.classFilters + it.islandFilters).all { f -> f.length <= 128 }) { "Filter too long" }
            if (it.enabled) validateMessage(it.command.trim())
        }
        require(profile.shortcuts.all { it.id.isNotBlank() && it.id.length <= 128 }) { "Invalid shortcut ID" }
        val active = profile.shortcuts.filter { it.enabled }
        require(active.map { alias(it.alias) }.distinct().size == active.size) { "Duplicate shortcut names" }
        active.forEach {
            require(it.id.isNotBlank() && aliasPattern.matches(alias(it.alias))) { "Shortcut must be a single command token" }
            require(alias(it.alias) != "solyanka") { "/solyanka is reserved" }
            validateMessage(it.message.trim())
        }
        // Reject cycles before installing the profile, including cycles with growing arguments.
        active.forEach { require(CommandResolver.resolve("/" + alias(it.alias), profile) !is Resolution.Error) { "Shortcut cycle, depth or length limit" } }
    }
    fun matches(bind: CommandBinding, profile: CommandProfile, device: InputDevice, key: Int, modifiers: Int, ctx: InputContext): Boolean {
        if (!bind.enabled || bind.key < 0 || bind.device != device || bind.key != key || !ctx.connected || !ctx.focused || ctx.textFocused) return false
        if (ctx.screen != ScreenKind.WORLD && !(ctx.screen == ScreenKind.CONTAINER && (profile.allowAllInGui || bind.allowInGui))) return false
        if (bind.modifier != Modifier.ANY && modifiers and 15 != bind.modifier.mask) return false
        fun accepts(filters: List<String>, current: String?) = filters.isEmpty() || current != null && filters.any { it.trim().equals(current.trim(), true) }
        return accepts(bind.classFilters, ctx.dungeonClass) && accepts(bind.islandFilters, ctx.island)
    }
}

sealed interface Resolution {
    data class Message(val text: String, val expanded: Boolean) : Resolution
    data class Error(val reason: String) : Resolution
}
object CommandResolver {
    fun isAliasInvocation(input: String, profile: CommandProfile): Boolean {
        if (!input.startsWith("/")) return false
        val token = input.substring(1).takeWhile { !it.isWhitespace() }
        return profile.shortcuts.any { it.enabled && CommandRules.alias(it.alias) == token }
    }
    fun resolve(input: String, profile: CommandProfile, enabled: Boolean = true): Resolution {
        var text = input.trim(); var expanded = false
        val seen = hashSetOf<String>()
        val aliases = if (enabled) profile.shortcuts.filter { it.enabled }.associateBy { CommandRules.alias(it.alias) } else emptyMap()
        repeat(17) {
            try { CommandRules.validateMessage(text) } catch (e: IllegalArgumentException) { return Resolution.Error(e.message ?: "Invalid message") }
            if (!text.startsWith("/")) return Resolution.Message(text, expanded)
            val content = text.substring(1)
            val name = content.takeWhile { !it.isWhitespace() }
            val shortcut = aliases[name] ?: return Resolution.Message(text, expanded)
            if (!seen.add(name)) return Resolution.Error("Shortcut cycle at /$name")
            if (seen.size > 16) return Resolution.Error("Shortcut depth exceeds 16")
            val tail = content.drop(name.length).trim()
            text = shortcut.message.trim() + if (tail.isEmpty()) "" else " $tail"
            expanded = true
        }
        return Resolution.Error("Shortcut depth exceeds 16")
    }
}

/** Tiny tab-list adapter only; not a full SkyBlock/dungeon state model. */
object TabContextParser {
    data class Context(val island: String?, val dungeonClass: String?)
    fun parse(lines: List<String>, playerName: String): Context {
        val clean = lines.map { it.replace(Regex("§."), "").trim() }
        val area = clean.firstOrNull { it.startsWith("Area: ") || it.startsWith("Dungeon: ") }?.substringAfter(':')?.trim()?.ifEmpty { null }
        val pattern = Regex("(?:^|\\s)${Regex.escape(playerName)}\\s+\\((Healer|Mage|Berserk|Archer|Tank)(?:\\s+[^)]*)?\\)$", RegexOption.IGNORE_CASE)
        val clazz = clean.firstNotNullOfOrNull { pattern.find(it)?.groupValues?.get(1) }
        return Context(area, clazz)
    }
}
