/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings

object ArchiveDungeonUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        val title="Secret Counter"
        var thresholdError:String?=null
        host.showPanel(SettingsPanel(title,{host.closePanel()},{thresholdError ?: model.error}) {panel,w->
            buildList {
                val s=model.general.features.secretCounter
                add(GuiKit.description(w,"Archive personal dungeon secrets counter. Enabling this module permits ONE /showextrastats request, 1 second after a recognized Catacombs completion header. Waits up to 10 seconds for an exact personal Secrets Found line, then reports locally. No API estimates. World/floor/run/toggle resets cancel pending work."))
                add(GuiKit.toggleRow(w,"Auto Announce",s.autoAnnounce,"Opt-in: sends your observed count to the selected chat channel after 0.5 seconds. Review server rules. Off by default.") {v->model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(autoAnnounce=v)))};panel.refreshStatus()})
                add(GuiKit.toggleRow(w,"Only Announce Above Threshold",s.thresholdEnabled,"Only send when the count is strictly greater than the threshold.") {v->model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(thresholdEnabled=v)))};panel.refreshStatus()})
                add(GuiKit.textRow(w,"Secret Threshold",s.threshold.toString(),"Non-negative whole number. Equal or lower counts are not announced.") {input->
                    val value=input.toIntOrNull()
                    thresholdError=if(value==null || value<0) "Secret threshold must be a non-negative whole number" else null
                    if(thresholdError==null) model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(threshold=value!!)))}
                    panel.refreshStatus()
                })
                val channels=listOf("current","party")
                add(GuiKit.choiceRow(w,"Announce Channel",listOf("Current Chat","Party Chat"),channels.indexOf(s.channel),readCurrent={channels.indexOf(model.general.features.secretCounter.channel)}) {i->model.updateGeneral {it.copy(features=it.features.copy(secretCounter=it.features.secretCounter.copy(channel=channels[i])))};panel.refreshStatus()})
            }
        })
    }
}
