/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills-derived controls; instant-saving embedded solyanka settings. See NOTICE. */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.utility.*
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ConfigUi {
    fun create(parent:Screen?):Screen=NoFrillsClickGui(parent)
    fun openSettings(id:String,host:NoFrillsClickGui) {
        val model=InstantSettings(SolyankaClient.currentProfile(),SolyankaClient.currentGeneral()) { p,g ->
            SolyankaClient.applySettings(p,SolyankaClient.states(),g)
        }
        when(id) {
            SolyankaClient.KEYBINDS -> bindings(host,model)
            SolyankaClient.SHORTCUTS -> shortcuts(host,model)
            SolyankaClient.CHAT -> general(host,model,true)
            SolyankaClient.TOOLTIPS -> general(host,model,false)
            else -> FeatureUi.open(id,host,model)
        }
    }
    private fun bindings(host:NoFrillsClickGui,m:InstantSettings) {
        fun again()=bindings(host,m)
        host.showPanel(SettingsPanel("Command Keybinds",{ host.closePanel() },{ m.error }) { panel,w ->
            fun update(change:(CommandProfile)->CommandProfile) { m.updateProfile(change);panel.refreshStatus() }
            buildList {
                add(GuiKit.description(w,"Valid edits save immediately. New entries default off. Edit opens here, inside this same GUI."))
                add(GuiKit.toggleRow(w,"Allow All in GUI",m.profile.allowAllInGui,"Container GUIs only; never chat, anvils or focused text fields.") { v -> update { it.copy(allowAllInGui=v) } })
                add(GuiKit.row(w).apply { child(GuiKit.button("Add New Keybind",140) {
                    update { require(it.binds.size<128) { "Maximum 128 keybinds" };it.copy(binds=it.binds+CommandBinding()) };again()
                }) })
                m.profile.binds.forEach { b ->
                    add(GuiKit.row(w).apply {
                        child(FlatTextbox(Sizing.fixed((w-185).coerceAtLeast(30))).apply {
                            text(b.name);setMaxLength(128)
                            onChanged().subscribe { v -> update { p -> p.copy(binds=p.binds.map { if(it.id==b.id) it.copy(name=v) else it }) } }
                        })
                        child(GuiKit.toggle(b.enabled) { v -> update { p -> p.copy(binds=p.binds.map { if(it.id==b.id) it.copy(enabled=v) else it }) } })
                        child(GuiKit.button("Edit",44) { binding(host,m,b.id) })
                        child(GuiKit.button("Delete",50,true) { update { p -> p.copy(binds=p.binds.filterNot { it.id==b.id }) };again() })
                    })
                }
            }
        })
    }
    private fun binding(host:NoFrillsClickGui,m:InstantSettings,id:String) {
        host.showPanel(SettingsPanel("Command Keybind",{ bindings(host,m) },{ m.error }) { panel,w ->
            val b=m.profile.binds.first { it.id==id }
            fun update(change:(CommandBinding)->CommandBinding) {
                m.updateProfile { p -> p.copy(binds=p.binds.map { if(it.id==id) change(it) else it }) };panel.refreshStatus()
            }
            listOf(
                GuiKit.textRow(w,"Name",b.name) { v -> update { it.copy(name=v) } },
                GuiKit.textRow(w,"Command / message",b.command,"Use / for commands, otherwise chat. Valid edits save immediately.") { v -> update { it.copy(command=v) } },
                panel.inputRow(w,b.device,b.key) { d,k -> update { it.copy(device=d,key=k) } },
                GuiKit.description(w,"Escape cancels key capture; Backspace unbinds. Back does not undo saved changes."),
                GuiKit.row(w).apply {
                    child(GuiKit.label("Modifier"))
                    child(GuiKit.button(b.modifier.name,80) {}.apply { onPress {
                        val current=m.profile.binds.first { it.id==id }.modifier
                        val next=Modifier.entries[(current.ordinal+1)%Modifier.entries.size]
                        update { it.copy(modifier=next) };message=Component.literal(next.name)
                    } })
                },
                GuiKit.toggleRow(w,"Enabled",b.enabled) { v -> update { it.copy(enabled=v) } },
                GuiKit.toggleRow(w,"Allow in GUI",b.allowInGui) { v -> update { it.copy(allowInGui=v) } },
                GuiKit.textRow(w,"Class Filter",b.classFilters.joinToString(", "),"Comma-separated dungeon classes; empty = unrestricted.") { v -> update { it.copy(classFilters=filters(v)) } },
                GuiKit.textRow(w,"Island Filter",b.islandFilters.joinToString(", "),"Comma-separated tab-list labels; unknown filtered context blocks binds.") { v -> update { it.copy(islandFilters=filters(v)) } }
            )
        })
    }
    private fun shortcuts(host:NoFrillsClickGui,m:InstantSettings) {
        fun again()=shortcuts(host,m)
        host.showPanel(SettingsPanel("Command Shortcuts",{ host.closePanel() },{ m.error }) { panel,w ->
            fun update(change:(CommandProfile)->CommandProfile) { m.updateProfile(change);panel.refreshStatus() }
            buildList {
                add(GuiKit.description(w,"Changes save instantly when valid. Invalid aliases/cycles stay local until corrected. Disable a row to freely edit incomplete text."))
                add(GuiKit.row(w).apply { child(GuiKit.button("Add New Shortcut",145) {
                    update { require(it.shortcuts.size<128) { "Maximum 128 shortcuts" };it.copy(shortcuts=it.shortcuts+Shortcut()) };again()
                }) })
                m.profile.shortcuts.forEach { s ->
                    add(UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
                        surface(Surface.flat(0x55101010))
                        child(GuiKit.row(w).apply {
                            child(FlatTextbox(Sizing.fixed((w-18)/2-4)).apply {
                                text(s.alias);setSuggestion("Shortcut /alias")
                                onChanged().subscribe { v -> update { p -> p.copy(shortcuts=p.shortcuts.map { if(it.id==s.id) it.copy(alias=v) else it }) } }
                            })
                            child(FlatTextbox(Sizing.fixed((w-18)/2-4)).apply {
                                text(s.message);setSuggestion("Command / chat")
                                onChanged().subscribe { v -> update { p -> p.copy(shortcuts=p.shortcuts.map { if(it.id==s.id) it.copy(message=v) else it }) } }
                            })
                        })
                        child(GuiKit.row(w).apply {
                            child(GuiKit.toggle(s.enabled) { v -> update { p -> p.copy(shortcuts=p.shortcuts.map { if(it.id==s.id) it.copy(enabled=v) else it }) } })
                            child(GuiKit.button("Delete",50,true) { update { p -> p.copy(shortcuts=p.shortcuts.filterNot { it.id==s.id }) };again() })
                        })
                    })
                }
                add(GuiKit.description(w,"/solyanka and registered client commands retain priority. Alias completion remains pending."))
            }
        })
    }
    private fun general(host:NoFrillsClickGui,m:InstantSettings,chat:Boolean) {
        host.showPanel(SettingsPanel(if(chat) "Chat Tweaks" else "Info Tooltips",{ host.closePanel() },{ m.error }) { panel,w ->
            fun change(block:(ChatSettings)->ChatSettings) { m.updateGeneral { it.copy(chat=block(it.chat)) };panel.refreshStatus() }
            fun tooltip(block:(TooltipSettings)->TooltipSettings) { m.updateGeneral { it.copy(tooltips=block(it.tooltips)) };panel.refreshStatus() }
            val c=m.general.chat;val t=m.general.tooltips
            if(chat) listOf(
                GuiKit.description(w,"Changes save instantly when valid. Copy keys work only over visible chat; Copy Line wins if keys match."),
                GuiKit.description(w,"Copy full message:"),panel.inputRow(w,c.copy.device,c.copy.key) { d,k -> change { it.copy(copy=CopyBinding(d,k)) } },
                GuiKit.description(w,"Copy wrapped line:"),panel.inputRow(w,c.copyLine.device,c.copyLine.key) { d,k -> change { it.copy(copyLine=CopyBinding(d,k)) } },
                GuiKit.toggleRow(w,"Trim On Copy",c.trimOnCopy) { v -> change { it.copy(trimOnCopy=v) } },
                GuiKit.toggleRow(w,"Message On Copy",c.messageOnCopy) { v -> change { it.copy(messageOnCopy=v) } },
                GuiKit.textRow(w,"Feedback Limit (0–512)",m.feedback,"0 = confirmation without copied text.") { m.feedback(it);panel.refreshStatus() },
                GuiKit.toggleRow(w,"Keep History",c.keepHistory,"Session memory only. Manual F3+D still clears chat.") { v -> change { it.copy(keepHistory=v) } },
                GuiKit.toggleRow(w,"Extra Lines",c.extraLines) { v -> change { it.copy(extraLines=v) } },
                GuiKit.textRow(w,"History Lines (100–5000)",m.lines,"Applies on subsequent message additions; does not expand sent-command history.") { m.lines(it);panel.refreshStatus() }
            ) else listOf(
                GuiKit.description(w,"Changes save instantly. SkyBlock custom-data items only. Pricing is a separate upcoming module."),
                GuiKit.toggleRow(w,"Dungeon Quality",t.dungeonQuality) { v -> tooltip { it.copy(dungeonQuality=v) } },
                GuiKit.toggleRow(w,"Created Date",t.createdDate,"Local timezone and locale.") { v -> tooltip { it.copy(createdDate=v) } },
                GuiKit.toggleRow(w,"Dye Hex Color",t.hexColor) { v -> tooltip { it.copy(hexColor=v) } },
                GuiKit.toggleRow(w,"Museum",t.museum,"Opt-in: downloads api.hypixel.net public item resource every 10 minutes in-world. IP visible; no API key or player/item data sent. Session cache only.") { v -> tooltip { it.copy(museum=v) } },
                GuiKit.toggleRow(w,"Item ID",t.skyblockId,"Pet/rune/book/potion/perfect dungeon/shard IDs.") { v -> tooltip { it.copy(skyblockId=v) } }
            )
        })
    }
    private fun filters(s:String)=s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
