/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.core.ModuleIds
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.NumberRange
import dev.solyanka.utility.*
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.client.feature.MiningHelpers

object MiningHelpersUi {
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        var status=""
        var rngTestMessage=""
        val title=when(id) {ModuleIds.HIDE_PLAYERS->"Hide Players";ModuleIds.RNG_ANNOUNCE->"Auto RNG Announce";ModuleIds.POWDER_CHEST->"Powder Chest";ModuleIds.VANGUARD_ANNOUNCE->"Vanguard Announce";else->"Mineshaft Helper"}
        host.showPanel(SettingsPanel(title,{host.closePanel()},{model.error},{if(id==ModuleIds.VANGUARD_ANNOUNCE)dev.solyanka.client.feature.VanguardAnnounce.status() else status}) {panel,w->
            val f=model.general.features
            fun update(change:(FeatureSettings)->FeatureSettings) {model.updateGeneral {it.copy(features=change(it.features))};panel.refreshStatus()}
            buildList {
                when(id) {
                    ModuleIds.HIDE_PLAYERS -> {
                        add(GuiKit.description(w,"Only hides real players. Your player and server NPCs remain visible."))
                        add(GuiKit.toggleRow(w,"Only in Dungeons",f.hidePlayers.onlyInDungeons) {v->update {it.copy(hidePlayers=it.hidePlayers.copy(onlyInDungeons=v))}})
                        add(GuiKit.toggleRow(w,"Hide all",f.hidePlayers.hideAll) {v->update {it.copy(hidePlayers=it.hidePlayers.copy(hideAll=v))}})
                        add(GuiKit.sliderRow(w,"Distance",f.hidePlayers.distance,NumberRange(0.0,32.0,.5),"Ignored when Hide all is on.") {v->update {it.copy(hidePlayers=it.hidePlayers.copy(distance=v))}})
                    }
                    ModuleIds.RNG_ANNOUNCE -> {
                        val s=f.rngAnnounce
                        add(GuiKit.description(w,"Hypixel SkyBlock only. Supports your own RNG DROP/found-a/an messages, SkyHanni Tracker Drop and Corpse Profit. A leading test '* ' is ignored. Unknown item prices cannot be value-filtered."))
                        add(GuiKit.toggleRow(w,"All Chat /ac",s.allChat) {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(allChat=v))}})
                        add(GuiKit.toggleRow(w,"Guild Chat /gc",s.guildChat) {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(guildChat=v))}})
                        add(GuiKit.toggleRow(w,"Party Chat /pc",s.partyChat) {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(partyChat=v))}})
                        add(GuiKit.sliderRow(w,"Minimum value (M)",s.minimumMillions,NumberRange(0.0,1000.0,1.0)) {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(minimumMillions=v))}})
                        add(GuiKit.toggleRow(w,"Announce unknown value",s.announceUnknownValue,"Standard found-a/an messages do not include a price. Malformed SkyHanni prices are rejected.") {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(announceUnknownValue=v))}})
                        add(GuiKit.toggleRow(w,"Check drop message colors",s.requireColors,"Requires gold SkyHanni prefix and price, or a light-purple RNG DROP header. Colors are a format check, not proof of authenticity.") {v->update {it.copy(rngAnnounce=it.rngAnnounce.copy(requireColors=v))}})
                        add(GuiKit.textRow(w,"RNG preview message",rngTestMessage,"Paste a sample here, including & color codes if desired. Preview NEVER sends chat.") {v->rngTestMessage=v.take(4096)})
                        add(GuiKit.button("Preview only (no sending)",190) {status=dev.solyanka.client.feature.RngAnnounce.preview(rngTestMessage);panel.refreshStatus()})
                        add(GuiKit.button("Check RNG status",150) {status=dev.solyanka.client.feature.RngAnnounce.diagnostics();panel.refreshStatus()})
                    }
                    ModuleIds.VANGUARD_ANNOUNCE -> {
                        val s=f.vanguardAnnounce
                        add(GuiKit.description(w,"Hypixel SkyBlock only. Once per Vanguard Mineshaft, sends a guild announcement and accepts at most 3 unique guild replies for 60 seconds. Failed invitations still consume a slot; no automatic party warp. Enable only when you want to recruit."))
                        add(GuiKit.textRow(w,"Guild announcement",s.message,"Plain text or /gc message; other commands are not accepted.") {v->update {it.copy(vanguardAnnounce=it.vanguardAnnounce.copy(message=v))}})
                        add(GuiKit.textRow(w,"Invite keyword",s.keyword,"Exact guild reply, with surrounding reply spaces ignored. Default: +") {v->update {it.copy(vanguardAnnounce=it.vanguardAnnounce.copy(keyword=v))}})
                    }
                    ModuleIds.POWDER_CHEST -> {
                        add(GuiKit.description(w,"Marks nearby non-chest to chest block updates in the Crystal Hollows. Existing structure chests are not scanned. Markers expire after two minutes or block removal."))
                        add(GuiKit.toggleRow(w,"Tracer",f.powderChest.tracer) {v->update {it.copy(powderChest=it.powderChest.copy(tracer=v))}})
                        add(GuiKit.colorRow(w,"Powder Chest color",f.powderChest.color) {v->update {it.copy(powderChest=it.powderChest.copy(color=v))}})
                    }
                    else -> {
                        val s=f.mineshaft
                        add(GuiKit.toggleRow(w,"Announce Mineshaft to Party",s.announceType) {v->update {it.copy(mineshaft=it.mineshaft.copy(announceType=v))}})
                        add(GuiKit.toggleRow(w,"Announce Corpse TAB summary",s.announceCorpses) {v->update {it.copy(mineshaft=it.mineshaft.copy(announceCorpses=v))}})
                        add(GuiKit.toggleRow(w,"Print Corpse coordinates",s.coordinates) {v->update {it.copy(mineshaft=it.mineshaft.copy(coordinates=v))}})
                        add(GuiKit.toggleRow(w,"Hide clicked Corpses",s.hideOnClick,"Left/right clicking a corpse hides its ESP, label and tracer until leaving the shaft.") {v->update {it.copy(mineshaft=it.mineshaft.copy(hideOnClick=v))}})
                        add(GuiKit.sliderRow(w,"Tracer disable radius",s.tracerRadius,NumberRange(0.0,32.0,.5),"Hides ESP, label and tracer inside the radius. Set 0 to disable proximity hiding.") {v->update {it.copy(mineshaft=it.mineshaft.copy(tracerRadius=v))}})
                        CorpseKind.entries.forEach {kind->
                            fun change(block:(CorpseStyle)->CorpseStyle) {update {features->
                                val m=features.mineshaft;val next=block(m.style(kind))
                                features.copy(mineshaft=when(kind) {CorpseKind.LAPIS->m.copy(lapis=next);CorpseKind.TUNGSTEN->m.copy(tungsten=next);CorpseKind.UMBER->m.copy(umber=next);CorpseKind.VANGUARD->m.copy(vanguard=next)})
                            }}
                            add(GuiKit.colorRow(w,"${kind.label} Corpse color",s.style(kind).color) {v->change {it.copy(color=v)}})
                            add(GuiKit.toggleRow(w,"${kind.label} Tracer",s.style(kind).tracer) {v->change {it.copy(tracer=v)}})
                        }
                        add(GuiKit.button("Export loaded Corpses to Waypoints",(w-8).coerceAtMost(260)) {
                            status=runCatching {"Created pack: ${MiningHelpers.exportCorpses()}. Select it in Waypoints."}.getOrElse {it.message ?: "Export failed"};panel.refreshStatus()
                        })
                        add(GuiKit.button("Open Waypoints",150) {WaypointUi.open(host,model)})
                        add(GuiKit.description(w,"Routes use Waypoints packs: select individual packs, edit numbered route points, and enable route lines there. No unverified SkyHanni routes are bundled."))
                        add(GuiKit.description(w,"Corpse ESP sees loaded armor stands only; it cannot reveal entities the server has not sent. Party announcements are off by default."))
                    }
                }
            }
        })
    }
}
