/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.YandexMusic
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.ui.NumberRange
import dev.solyanka.utility.MusicSettings

object MusicUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        host.showPanel(SettingsPanel("Yandex Music",{host.closePanel()},{model.error},{YandexMusic.status}) {panel,w->
            val s=model.general.features.music
            fun update(change:(MusicSettings)->MusicSettings) {
                model.updateGeneral {it.copy(features=it.features.copy(music=change(it.features.music)))};panel.refreshStatus()
            }
            listOf(
                GuiKit.description(w,"Windows local player only. Play a track in the Yandex Music desktop app, enable the module and connect. No account sign-in in the mod."),
                GuiKit.button("Connect local (Windows)",180) {YandexMusic.connectLocalWindows()},
                GuiKit.button("Disconnect",100) {YandexMusic.disconnect()},
                HudNavigation.button(w),
                GuiKit.colorRow(w,"HUD Text Color",s.hud.color) {v->update {it.copy(hud=it.hud.copy(color=v))}},
                GuiKit.textRow(w,"Service Label",s.serviceLabel,"Text shown at the top left of the HUD.") {v->update {it.copy(serviceLabel=v.take(40))}},
                GuiKit.colorRow(w,"Service Label Color",s.serviceColor) {v->update {it.copy(serviceColor=v)}},
                GuiKit.textRow(w,"Playing Label",s.playingLabel,"Text shown while music is playing.") {v->update {it.copy(playingLabel=v.take(40))}},
                GuiKit.colorRow(w,"Playing Label Color",s.playingColor) {v->update {it.copy(playingColor=v)}},
                GuiKit.colorRow(w,"Progress Color",s.progressColor) {v->update {it.copy(progressColor=v)}},
                GuiKit.choiceRow(w,"Progress Effect",listOf("Static","Breathing","Rainbow"),s.progressEffect,{model.general.features.music.progressEffect}) {v->update {it.copy(progressEffect=v)}},
                GuiKit.sliderRow(w,"Effect Speed",s.effectSpeed,NumberRange(.2,3.0,.1)) {v->update {it.copy(effectSpeed=v)}},
                GuiKit.toggleRow(w,"Show Synced Lyrics",s.showLyrics,"Sends the current title, artist and duration to LRCLIB. Lyrics may be missing or mismatched.") {v->update {it.copy(showLyrics=v)}},
                GuiKit.description(w,"When lyrics are enabled, track title, artist and duration are sent to LRCLIB. !suggest is not available in local mode.")
            )
        })
    }
}
