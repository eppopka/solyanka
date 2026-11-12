/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds

import dev.solyanka.client.feature.YandexMusic
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.NumberRange
import dev.solyanka.utility.MusicSettings
import net.minecraft.util.Util

object MusicUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        var authorizationCode=""
        host.showPanel(SettingsPanel("Yandex Music",{host.closePanel()},{model.error},{YandexMusic.status}) {panel,w->
            val s=model.general.features.music
            fun update(change:(MusicSettings)->MusicSettings) {
                model.updateGeneral {it.copy(features=it.features.copy(music=change(it.features.music)))};panel.refreshStatus()
            }
            buildList {
                add(GuiKit.description(w,"A focused music panel: connect once, then keep the HUD lightweight. Account mode enables authenticated search and !suggest; the token is kept in memory only."))
                add(GuiKit.section(w,"Connection","Choose one source. Local mode reads the Windows desktop player; account mode controls the Yandex queue."))
                add(GuiKit.button("Connect local (Windows)",180) {YandexMusic.connectLocalWindows()})
                add(GuiKit.button("Connect Yandex account",180) {YandexMusic.connect()})
                add(GuiKit.button("Disconnect",100) {YandexMusic.disconnect()})
                add(GuiKit.row(w).apply {
                    child(GuiKit.button("Open sign-in page",135) {YandexMusic.verification?.let {Util.getPlatform().openUri(it.url)}})
                })
                add(GuiKit.textRow(w,"Authorization code",authorizationCode,"Enter the 7-digit code shown by Yandex after approving sign-in.") {v->authorizationCode=v.filter(Char::isDigit).take(7)})
                add(GuiKit.button("Submit authorization code",190) {YandexMusic.submitVerificationCode(authorizationCode)})
                add(GuiKit.section(w,"Queue controls","Use !suggest <song name> after account mode reports Connected."))
                add(GuiKit.toggleRow(w,"Allow !suggest",s.suggest,"Account mode only. Searches Yandex Music and appends the first matching track to the current queue.") {v->update {it.copy(suggest=v)}})
                add(GuiKit.toggleRow(w,"Announce Added Track",s.announce,"Only announces after Yandex confirms the queue update.") {v->update {it.copy(announce=v)}})
                add(GuiKit.section(w,"HUD appearance","The HUD editor controls placement and scale; these options control its visual hierarchy."))
                add(HudNavigation.button(w))
                add(GuiKit.colorRow(w,"HUD Text Color",s.hud.color) {v->update {it.copy(hud=it.hud.copy(color=v))}})
                add(GuiKit.textRow(w,"Service Label",s.serviceLabel,"Text shown at the top left of the HUD.") {v->update {it.copy(serviceLabel=v.take(40))}})
                add(GuiKit.colorRow(w,"Service Label Color",s.serviceColor) {v->update {it.copy(serviceColor=v)}})
                add(GuiKit.textRow(w,"Playing Label",s.playingLabel,"Text shown while music is playing.") {v->update {it.copy(playingLabel=v.take(40))}})
                add(GuiKit.colorRow(w,"Playing Label Color",s.playingColor) {v->update {it.copy(playingColor=v)}})
                add(GuiKit.colorRow(w,"Progress Color",s.progressColor) {v->update {it.copy(progressColor=v)}})
                add(GuiKit.choiceRow(w,"Progress Effect",listOf("Static","Breathing","Rainbow"),s.progressEffect,{model.general.features.music.progressEffect}) {v->update {it.copy(progressEffect=v)}})
                add(GuiKit.sliderRow(w,"Effect Speed",s.effectSpeed,NumberRange(.2,3.0,.1)) {v->update {it.copy(effectSpeed=v)}})
                add(GuiKit.section(w,"Lyrics","Lyrics use LRCLIB only when enabled. Missing tracks fall back to a title/artist search."))
                add(GuiKit.toggleRow(w,"Show Synced Lyrics",s.showLyrics,"Sends the current title, artist and duration to LRCLIB. Lyrics may be missing or mismatched.") {v->update {it.copy(showLyrics=v)}})
                add(GuiKit.description(w,"Local mode cannot search or change the Yandex queue. Account mode is required for !suggest."))
                addAll(HudGlowUi.controls(ModuleIds.MUSIC,model,panel,w))
            }
        })
    }
}
