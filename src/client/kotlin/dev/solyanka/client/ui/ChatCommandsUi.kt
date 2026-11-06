/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.utility.*

object ChatCommandsUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        var newName="";var newResponse="";var error:String?=null
        val names=mutableMapOf<Int,String>();val replies=mutableMapOf<Int,String>()
        lateinit var panel:SettingsPanel
        fun update(block:(ChatCommandsSettings)->ChatCommandsSettings) {
            val old=model.general
            if(!model.updateGeneral {it.copy(features=it.features.copy(chatCommands=block(it.features.chatCommands)))}) {
                val reason=model.error;model.updateGeneral {old};error=reason
            } else error=null
            panel.refreshStatus()
        }
        panel=SettingsPanel("Chat Commands",{host.closePanel()},{error ?: model.error}) {_,w->
            val s=model.general.features.chatCommands
            buildList {
                add(GuiKit.description(w,"Hypixel: responds to other players in the same chat channel. 5 seconds between replies and 10 seconds per player. Review server rules before enabling."))
                add(GuiKit.textRow(w,"Prefix",s.prefix) {value->update {it.copy(prefix=value)}})
                add(GuiKit.toggleRow(w,"Public Chat",s.publicChat) {v->update {it.copy(publicChat=v)}})
                add(GuiKit.toggleRow(w,"Party Chat",s.party) {v->update {it.copy(party=v)}})
                add(GuiKit.toggleRow(w,"Guild Chat",s.guild) {v->update {it.copy(guild=v)}})
                add(GuiKit.toggleRow(w,"Officer Chat",s.officer) {v->update {it.copy(officer=v)}})
                add(GuiKit.toggleRow(w,"Private Messages",s.privateChat) {v->update {it.copy(privateChat=v)}})
                add(GuiKit.toggleRow(w,"Co-op Chat",s.coop) {v->update {it.copy(coop=v)}})
                add(GuiKit.description(w,"Responses support {name} and {randint(60,100)}. Commands are text replies, not executable slash commands."))
                s.commands.forEachIndexed {i,command->
                    add(GuiKit.textRow(w,"Command ${i+1}",names.getOrPut(i) {command.name}) {names[i]=it})
                    add(GuiKit.textRow(w,"Response",replies.getOrPut(i) {command.response}) {replies[i]=it})
                    add(GuiKit.row(w).apply {
                        child(GuiKit.button("Save",65) {update {c->c.copy(commands=c.commands.mapIndexed {j,v->if(j==i) ChatCommand(names[i] ?: v.name,replies[i] ?: v.response) else v})};if(error==null) {names.clear();replies.clear();panel.mount(host.width,host.height)}})
                        child(GuiKit.button("Delete",65) {update {c->c.copy(commands=c.commands.filterIndexed {j,_->j!=i})};if(error==null) {names.clear();replies.clear();panel.mount(host.width,host.height)}})
                    })
                }
                add(GuiKit.textRow(w,"New Command",newName) {newName=it})
                add(GuiKit.textRow(w,"New Response",newResponse) {newResponse=it})
                add(GuiKit.button("Add Command",110) {update {it.copy(commands=it.commands+ChatCommand(newName,newResponse))};if(error==null) {newName="";newResponse="";panel.mount(host.width,host.height)}})
            }
        }
        host.showPanel(panel)
    }
}
