/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings

object ArchiveDungeonUi {
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        val title=if(id==S.SECRET_COUNTER) "Secret Counter" else "Always Water"
        host.showPanel(SettingsPanel(title,{host.closePanel()},{model.error}) {panel,w->
            buildList {
                if(id==S.SECRET_COUNTER) {
                    val s=model.general.features.secretCounter
                    add(GuiKit.description(w,"Archive personal dungeon secrets counter. Enabling this module permits ONE /showextrastats request, 1 second after a recognized Catacombs completion header. Waits up to 10 seconds for an exact personal Secrets Found line, then reports locally. No API estimates. World/floor/run/toggle resets cancel pending work."))
                    add(GuiKit.toggleRow(w,"Auto Announce",s.autoAnnounce,"Opt-in: sends your observed count to the selected chat channel after 0.5 seconds. Review server rules. Off by default.") {v->model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(autoAnnounce=v)))};panel.refreshStatus()})
                    val channels=listOf("current","party")
                    add(GuiKit.choiceRow(w,"Announce Channel",listOf("Current Chat","Party Chat"),channels.indexOf(s.channel),readCurrent={channels.indexOf(model.general.features.secretCounter.channel)}) {i->model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(channel=channels[i])))};panel.refreshStatus()})
                } else {
                    add(GuiKit.description(w,"Archive Always Water means ALWAYS RAINING, even under roofs—not water placement or swimming physics. The master toggle forces client rain and rainy precipitation in the native weather pass. It wins over Ambience Clear/Snow; Ambience intensity settings can still adjust opacity. Disabling restores the current Ambience/server weather. No world blocks, server weather, heightmap data or fluid state are changed. Shader packs may replace the native precipitation pass."))
                }
            }
        })
    }
}
