/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.YandexMusic
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import net.minecraft.client.Minecraft
import net.minecraft.util.Util

object MusicUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        host.showPanel(SettingsPanel("Yandex Music",{host.closePanel()},{model.error},{YandexMusic.status}) {panel,w->
            val s=model.general.features.music
            listOf(
                GuiKit.description(w,"Unofficial experimental integration. Sign in with a Yandex device code; your token is held in memory only until disconnect or game exit. The account must have an active Yandex Music player."),
                GuiKit.button("Connect",100) {YandexMusic.connect()},
                GuiKit.button("Disconnect",100) {YandexMusic.disconnect()},
                GuiKit.row(w).apply {
                    child(GuiKit.button("Open sign-in page",135) {YandexMusic.verification?.let {Util.getPlatform().openUri(it.url)}})
                    child(GuiKit.button("Copy code",100) {YandexMusic.verification?.let {Minecraft.getInstance().keyboardHandler.clipboard=it.code}})
                },
                GuiKit.description(w,"After Connect, use these buttons while the sign-in code is displayed."),
                HudNavigation.button(w),
                GuiKit.colorRow(w,"HUD Text Color",s.hud.color) {v->model.updateGeneral {it.copy(features=it.features.copy(music=it.features.music.copy(hud=it.features.music.hud.copy(color=v))))};panel.refreshStatus()},
                GuiKit.toggleRow(w,"Allow !suggest",s.suggest,"Experimental whole-queue update; may overwrite concurrent changes. Test only with a disposable queue. Radio, My Wave and shuffle are not supported.") {v->model.updateGeneral {it.copy(features=it.features.copy(music=it.features.music.copy(suggest=v)))};panel.refreshStatus()},
                GuiKit.toggleRow(w,"Announce Added Track",s.announce,"Only announces when the server confirms the queue update.") {v->model.updateGeneral {it.copy(features=it.features.copy(music=it.features.music.copy(announce=v)))};panel.refreshStatus()}
            )
        })
    }
}
