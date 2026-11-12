/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.InstantSettings

object LeapMessageUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        host.showPanel(SettingsPanel("Leap Message+",{host.closePanel()},{model.error}) { panel,w ->
            val settings=model.general.features.leapMessage
            listOf(
                GuiKit.description(w,"After a dungeon teleport confirmation, send a party message. {name} is the target; {leapcount} is the saved count, or ? when Count Leaps is off. Counts start when enabled and are stored by player name."),
                GuiKit.toggleRow(w,"Count Leaps",settings.countLeaps,"Counts confirmed teleports while this feature and Count Leaps are enabled.") { value ->
                    model.updateGeneral { it.copy(features=it.features.copy(leapMessage=it.features.leapMessage.copy(countLeaps=value))) };panel.refreshStatus()
                },
                GuiKit.textRow(w,"Party Message",settings.message,"Use {name} and {leapcount}; leave empty to disable announcements.") { value ->
                    model.updateGeneral { it.copy(features=it.features.copy(leapMessage=it.features.leapMessage.copy(message=value))) };panel.refreshStatus()
                }
            )
        })
    }
}
