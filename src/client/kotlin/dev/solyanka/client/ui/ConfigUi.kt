/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills command settings, ClickGui and InnerSettingsScreen.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.CommandGuiDraft
import dev.solyanka.utility.*
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.AlertScreen
import net.minecraft.network.chat.Component

object ConfigUi {
    fun create(parent:Screen?):Screen=NoFrillsClickGui(parent)
    private fun open(screen:Screen) { Minecraft.getInstance().setScreenAndShow(screen) }
    fun error(back:()->Screen,message:String) {
        open(AlertScreen({ open(back()) },Component.literal("solyanka — not saved"),Component.literal(message)))
    }
    fun openSettings(id:String,back:()->Screen) {
        val draft=CommandGuiDraft(SolyankaClient.currentProfile())
        when(id) {
            SolyankaClient.KEYBINDS -> open(bindings(draft,back))
            SolyankaClient.SHORTCUTS -> open(shortcuts(draft,back))
        }
    }
    private fun save(draft:CommandGuiDraft,retry:()->Screen,back:()->Screen) {
        val failure=try { SolyankaClient.applySettings(draft.validated(),SolyankaClient.states()) }
            catch(e:IllegalArgumentException) { e.message ?: "Invalid settings" }
        if(failure==null) open(back()) else error(retry,failure)
    }
    private fun guarded(retry:()->Screen,action:()->Unit) {
        try { action() } catch(e:IllegalArgumentException) { error(retry,e.message ?: "Invalid settings") }
    }
    private fun bindings(draft:CommandGuiDraft,back:()->Screen):Screen {
        fun again():Screen=bindings(draft,back)
        return SettingsPanel("Command Keybinds",back,"Save & back",{ save(draft,{ again() },back) }) { _,w ->
            val rows=mutableListOf<UIComponent>()
            rows+=GuiKit.description(w,"Edits are staged until Save & back. Use Edit for command, capture, modifiers and filters.")
            rows+=GuiKit.toggleRow(w,"Allow All in GUI",draft.profile.allowAllInGui,
                "Container GUIs only; never chat, anvils, settings or focused text fields.") { draft.allowAllInGui(it) }
            rows+=GuiKit.row(w).apply { child(GuiKit.button("Add New Keybind",140) {
                guarded({ again() }) { draft.addBind();open(again()) }
            }) }
            if(draft.profile.binds.isEmpty()) rows+=GuiKit.description(w,"No keybinds. New entries and module toggles default off.")
            draft.profile.binds.forEach { bind ->
                rows+=GuiKit.row(w).apply {
                    val nameWidth=(w-10-58-44-50-15).coerceAtLeast(30)
                    child(FlatTextbox(Sizing.fixed(nameWidth-4)).apply {
                        text(bind.name);tooltip(Component.literal("Keybind name"));onChanged().subscribe { value -> draft.updateBind(bind.id) { it.copy(name=value) } }
                    })
                    child(GuiKit.toggle(bind.enabled) { value -> draft.updateBind(bind.id) { it.copy(enabled=value) } })
                    child(GuiKit.button("Edit",44) {
                        val latest=draft.profile.binds.first { it.id==bind.id }
                        open(bindingEditor(draft,BindEdit(latest),back))
                    })
                    child(GuiKit.button("Delete",50,true) { draft.removeBind(bind.id);open(again()) })
                }
            }
            rows
        }
    }
    private data class BindEdit(var value:CommandBinding)
    private fun bindingEditor(draft:CommandGuiDraft,edit:BindEdit,back:()->Screen):Screen {
        fun list():Screen=bindings(draft,back)
        return SettingsPanel("Command Keybind: ${edit.value.name}",{ list() },"Done (stage)",{
            draft.updateBind(edit.value.id) { edit.value };open(list())
        }) { panel,w ->
            listOf(
                GuiKit.textRow(w,"Name",edit.value.name) { edit.value=edit.value.copy(name=it) },
                GuiKit.textRow(w,"Command / message",edit.value.command,
                    "Use / for commands; otherwise sends chat. Max 256 characters.") { edit.value=edit.value.copy(command=it) },
                panel.inputRow(w,edit.value.device,edit.value.key) { d,k -> edit.value=edit.value.copy(device=d,key=k) },
                GuiKit.description(w,"Click keybind, then press a main key or mouse button. Escape cancels capture; Backspace unbinds. Set modifiers separately."),
                GuiKit.row(w).apply {
                    child(GuiKit.label("Modifier"))
                    child(GuiKit.button(edit.value.modifier.name,80) {}.apply {
                        onPress {
                            val values=Modifier.entries
                            edit.value=edit.value.copy(modifier=values[(edit.value.modifier.ordinal+1)%values.size])
                            message=Component.literal(edit.value.modifier.name)
                        }
                    })
                },
                GuiKit.toggleRow(w,"Enabled",edit.value.enabled) { edit.value=edit.value.copy(enabled=it) },
                GuiKit.toggleRow(w,"Allow in GUI",edit.value.allowInGui) { edit.value=edit.value.copy(allowInGui=it) },
                GuiKit.textRow(w,"Class Filter",edit.value.classFilters.joinToString(", "),
                    "Comma-separated Healer, Mage, Berserk, Archer, Tank. Empty = unrestricted; unknown context blocks filtered binds.") { edit.value=edit.value.copy(classFilters=filters(it)) },
                GuiKit.textRow(w,"Island Filter",edit.value.islandFilters.joinToString(", "),
                    "Comma-separated tab-list Area/Dungeon labels. Empty = unrestricted.") { edit.value=edit.value.copy(islandFilters=filters(it)) },
                GuiKit.description(w,"Done stages this row. Save & back on the list persists it. Cancel / back discards this editor's un-staged edits.")
            )
        }
    }
    private fun shortcuts(draft:CommandGuiDraft,back:()->Screen):Screen {
        fun again():Screen=shortcuts(draft,back)
        return SettingsPanel("Command Shortcuts",back,"Save & back",{ save(draft,{ again() },back) }) { _,w ->
            val rows=mutableListOf<UIComponent>()
            rows+=GuiKit.description(w,"Exact alias names, optional leading /. Arguments are appended. Changes apply live after Save & back.")
            rows+=GuiKit.row(w).apply { child(GuiKit.button("Add New Shortcut",145) {
                guarded({ again() }) { draft.addShortcut();open(again()) }
            }) }
            if(draft.profile.shortcuts.isEmpty()) rows+=GuiKit.description(w,"No shortcuts. The module and each new entry default off.")
            draft.profile.shortcuts.forEach { shortcut ->
                val block=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
                    surface(Surface.flat(0x55101010))
                    child(GuiKit.row(w).apply {
                        child(FlatTextbox(Sizing.fixed((w-18)/2-4)).apply {
                            text(shortcut.alias);setSuggestion("Shortcut /alias");tooltip(Component.literal("Shortcut name"))
                            onChanged().subscribe { v -> draft.updateShortcut(shortcut.id) { it.copy(alias=v) } }
                        })
                        child(FlatTextbox(Sizing.fixed((w-18)/2-4)).apply {
                            text(shortcut.message);setSuggestion("Command / chat");tooltip(Component.literal("Message / command"))
                            onChanged().subscribe { v -> draft.updateShortcut(shortcut.id) { it.copy(message=v) } }
                        })
                    })
                    child(GuiKit.row(w).apply {
                        child(GuiKit.toggle(shortcut.enabled) { v -> draft.updateShortcut(shortcut.id) { it.copy(enabled=v) } })
                        child(GuiKit.button("Delete",50,true) { draft.removeShortcut(shortcut.id);open(again()) })
                    })
                }
                rows+=block
            }
            rows+=GuiKit.description(w,"/solyanka and registered client commands keep priority. Cycles and overlong expansions are rejected. Alias completion nodes remain pending.")
            rows
        }
    }
    private fun filters(s:String)=s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
