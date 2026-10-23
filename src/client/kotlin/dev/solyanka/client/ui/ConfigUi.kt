/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.*
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.AlertScreen
import net.minecraft.network.chat.Component

object ConfigUi {
    private fun t(key: String) = Component.translatable("solyanka.config.$key")
    private fun desc(text: String) = OptionDescription.of(Component.literal(text))

    fun create(parent: Screen?): Screen {
        var profile = SolyankaClient.currentProfile()
        val enabled = SolyankaClient.states().toMutableMap()
        val tracked = mutableListOf<Option<*>>()
        fun <T> track(option: Option<T>): Option<T> = option.also { tracked.add(it) }
        fun bool(key: String, get: () -> Boolean, set: (Boolean) -> Unit, help: String = "") = track(
            Option.createBuilder<Boolean>().name(t(key)).description(desc(help))
                .binding(false,get,set).controller { BooleanControllerBuilder.create(it).yesNoFormatter() }.build())
        fun string(key: String, get: () -> String, set: (String) -> Unit, help: String = "") = track(
            Option.createBuilder<String>().name(t(key)).description(desc(help))
                .binding("",get,set).controller { StringControllerBuilder.create(it) }.build())
        lateinit var screen: Screen
        fun flush() { tracked.forEach { it.applyValue() } }
        fun apply(rebuild: Boolean = false): Boolean {
            val error = SolyankaClient.applySettings(profile,enabled)
            val mc = Minecraft.getInstance()
            if (error != null) mc.setScreenAndShow(AlertScreen({ mc.setScreenAndShow(screen) },t("error"),Component.literal(error)))
            else if (rebuild) mc.setScreenAndShow(create(parent))
            return error == null
        }
        fun mutate(change: (CommandProfile) -> CommandProfile) {
            flush()
            val previous = profile
            profile = change(profile)
            if (!apply(true)) profile = previous
        }
        val general = ConfigCategory.createBuilder().name(t("general"))
            .option(bool("keybinds", { enabled[SolyankaClient.KEYBINDS] == true }, { enabled[SolyankaClient.KEYBINDS] = it }))
            .option(bool("shortcuts", { enabled[SolyankaClient.SHORTCUTS] == true }, { enabled[SolyankaClient.SHORTCUTS] = it }))
            .option(bool("diagnostics", { enabled["core.diagnostics"] == true }, { enabled["core.diagnostics"] = it }))
            .option(bool("all_gui", { profile.allowAllInGui }, { profile = profile.copy(allowAllInGui = it) },
                "Container screens only. Never chat, anvils, settings or focused text fields."))
            .option(LabelOption.create(Component.literal("Commands and messages may be sent to the server. All modules and new entries default off.")))
            .option(LabelOption.create(Component.literal("Save & add/remove applies all pending edits. Filters use current tab-list Area/Dungeon and your class; unknown context blocks filtered binds.")))
        val bindings = ConfigCategory.createBuilder().name(t("bindings"))
            .option(ButtonOption.createBuilder().name(t("add_bind")).text(t("add"))
                .action { _, _ -> mutate { it.copy(binds = it.binds + CommandBinding()) } }.build())
        profile.binds.forEachIndexed { index, bind ->
            fun row() = profile.binds[index]
            fun update(change: (CommandBinding) -> CommandBinding) { profile = profile.copy(binds = profile.binds.mapIndexed { i,b -> if (i == index) change(b) else b }) }
            val key = track(Option.createBuilder<Int>().name(t("keycode"))
                .description(desc("Use Capture input, or -1 to unbind. GLFW keyboard codes / mouse buttons 0–7."))
                .binding(-1,{ row().key },{ v -> update { it.copy(key=v) } })
                .controller { IntegerFieldControllerBuilder.create(it).range(-1,348) }.build())
            val device = track(Option.createBuilder<InputDevice>().name(t("device"))
                .binding(InputDevice.KEYBOARD,{ row().device },{ v -> update { it.copy(device=v) } })
                .controller { EnumControllerBuilder.create(it).enumClass(InputDevice::class.java) }.build())
            val group = OptionGroup.createBuilder().name(Component.literal(bind.name)).collapsed(true)
                .option(bool("enabled",{ row().enabled },{ v -> update { it.copy(enabled=v) } }))
                .option(string("name",{ row().name },{ v -> update { it.copy(name=v) } }))
                .option(string("message",{ row().command },{ v -> update { it.copy(command=v) } },"Use / for a command; otherwise sends ordinary chat. Maximum 256 characters."))
                .option(device).option(key)
                .option(ButtonOption.createBuilder().name(t("capture")).text(t("capture"))
                    .action { _, _ -> Minecraft.getInstance().setScreenAndShow(KeyCaptureScreen(screen) { d,k -> device.requestSet(d); key.requestSet(k) }) }.build())
                .option(track(Option.createBuilder<Modifier>().name(t("modifier"))
                    .binding(Modifier.ANY,{ row().modifier },{ v -> update { it.copy(modifier=v) } })
                    .controller { EnumControllerBuilder.create(it).enumClass(Modifier::class.java) }.build()))
                .option(bool("gui",{ row().allowInGui },{ v -> update { it.copy(allowInGui=v) } }))
                .option(string("classes",{ row().classFilters.joinToString(", ") },{ v -> update { it.copy(classFilters=filters(v)) } },"Comma-separated Healer, Mage, Berserk, Archer, Tank. Empty = unrestricted."))
                .option(string("islands",{ row().islandFilters.joinToString(", ") },{ v -> update { it.copy(islandFilters=filters(v)) } },"Comma-separated tab-list area names. Empty = unrestricted."))
                .option(ButtonOption.createBuilder().name(t("remove")).text(t("remove"))
                    .action { _, _ -> mutate { it.copy(binds=it.binds.filterIndexed { i,_ -> i != index }) } }.build())
            bindings.group(group.build())
        }
        val shortcuts = ConfigCategory.createBuilder().name(t("shortcuts"))
            .option(ButtonOption.createBuilder().name(t("add_shortcut")).text(t("add"))
                .action { _, _ -> mutate { it.copy(shortcuts=it.shortcuts + Shortcut()) } }.build())
        profile.shortcuts.forEachIndexed { index, shortcut ->
            fun row() = profile.shortcuts[index]
            fun update(change: (Shortcut) -> Shortcut) { profile=profile.copy(shortcuts=profile.shortcuts.mapIndexed { i,s -> if (i == index) change(s) else s }) }
            shortcuts.group(OptionGroup.createBuilder().name(Component.literal(shortcut.alias.ifBlank { "New shortcut" })).collapsed(true)
                .option(bool("enabled",{ row().enabled },{ v -> update { it.copy(enabled=v) } }))
                .option(string("alias",{ row().alias },{ v -> update { it.copy(alias=v) } },"Single token, optional leading /. /solyanka is reserved. Example: dh"))
                .option(string("message",{ row().message },{ v -> update { it.copy(message=v) } },"Example: /warp dungeon_hub. Invocation arguments are appended. Plain chat is also supported."))
                .option(ButtonOption.createBuilder().name(t("remove")).text(t("remove"))
                    .action { _, _ -> mutate { it.copy(shortcuts=it.shortcuts.filterIndexed { i,_ -> i != index }) } }.build()).build())
        }
        screen = YetAnotherConfigLib.createBuilder().title(Component.literal("solyanka • Minecraft 26.2"))
            .category(general.build()).category(bindings.build()).category(shortcuts.build())
            .save { apply() }.build().generateScreen(parent)
        return screen
    }
    private fun filters(s: String) = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
