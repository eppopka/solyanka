/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.ItemTracker
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.NumberRange
import dev.solyanka.utility.*

object TrackerUi {
    fun open(host:NoFrillsClickGui,model:InstantSettings) {
        var newName="";var error:String?=null;var resetArmedUntil=0L
        val names=mutableMapOf<Int,String>();val ids=mutableMapOf<Int,String>();val prices=mutableMapOf<Int,String>()
        lateinit var panel:SettingsPanel
        fun update(change:(TrackerSettings)->TrackerSettings):Boolean {
            val old=model.general
            val saved=model.updateGeneral {it.copy(features=it.features.copy(tracker=change(it.features.tracker)))}
            if(!saved) {val reason=model.error;model.updateGeneral {old};error=reason} else error=null
            panel.refreshStatus();return saved
        }
        fun remount() {names.clear();ids.clear();prices.clear();panel.rebuild(host.width,host.height)}
        panel=SettingsPanel("Tracker",{host.closePanel()},{error ?: model.error},{ItemTracker.status()}) {_,w->
            val s=model.general.features.tracker
            buildList {
                add(GuiKit.description(w,"Counts new gains of selected items. Existing inventory is only a baseline. Inventory moves are aggregated; container transfers are ignored. Detailed native Sacks hover messages are optional. This is not a complete SkyHanni tracker port."))
                add(GuiKit.toggleRow(w,"HUD Visible",s.hud.enabled) {v->update {it.copy(hud=it.hud.copy(enabled=v))}})
                add(HudNavigation.button(w))
                add(GuiKit.toggleRow(w,"SkyBlock only",s.skyblockOnly,"Disable for local/singleplayer testing.") {v->update {it.copy(skyblockOnly=v)}})
                add(GuiKit.toggleRow(w,"Online Tracker prices",s.onlinePrices,"Opt-in public price/catalog downloads using the shared pricing service. No inventory, IGN or counts are uploaded.") {v->update {it.copy(onlinePrices=v)}})
                val modes=listOf("instant_sell","sell_offer","lowest_bin","npc","manual")
                val priceNames=listOf("Bazaar instant-sell","Bazaar sell-offer estimate","Lowest BIN","NPC sell","Manual prices")
                add(GuiKit.button("Prices: "+priceNames[modes.indexOf(s.priceMode)],(w-8).coerceAtMost(310)) {
                    if(update {it.copy(priceMode=modes[(modes.indexOf(it.priceMode)+1)%modes.size])})remount()
                })
                add(GuiKit.description(w,"Bazaar modes fall back to lowest BIN, then NPC when unavailable. Sell-offer uses the current ask price, not a guaranteed sale. Profit/h* is estimated gross item value divided by active time: no costs or selling fees. Unknown prices show ? rather than zero."))
                add(GuiKit.button(if(s.persistence=="session") "Reset mode: Session" else "Reset mode: Never",(w-8).coerceAtMost(280)) {
                    if(update {it.copy(persistence=if(it.persistence=="session") "never" else "session")})remount()
                })
                add(GuiKit.description(w,"Session resets when Minecraft restarts, not on server/world changes. Never saves totals and active time locally until manual reset. Storage is per Minecraft account; reset when changing SkyBlock profiles."))
                add(GuiKit.button(if(s.activityMode=="item") "AFK mode: Until tracked item" else "AFK mode: Movement or clicks",(w-8).coerceAtMost(300)) {
                    if(update {it.copy(activityMode=if(it.activityMode=="item") "movement" else "item")})remount()
                })
                add(GuiKit.description(w,"After 30 seconds without activity, the entire idle interval is removed. Movement/click mode resumes on movement or clicks. Until tracked item starts/resumes only on a tracked gain; 30 seconds without gains pauses it again. Moving alone does not resume that mode."))
                add(GuiKit.toggleRow(w,"Count native Sacks gains",s.countSacks,"Requires detailed [Sacks] hover data. Summaries without item details cannot be counted. Sack/container transfers are conservatively suppressed.") {v->update {it.copy(countSacks=v)}})
                add(GuiKit.toggleRow(w,"Background",s.background) {v->update {it.copy(background=v)}})
                add(GuiKit.sliderRow(w,"Visible item rows",s.maxRows.toDouble(),NumberRange(1.0,16.0,1.0)) {v->update {it.copy(maxRows=v.toInt())}})
                add(GuiKit.toggleRow(w,"Colorful Tracker HUD",s.colorful,"Distinct colors for time, quantities, coin values and profit/hour. Text Color still controls item names; disable for a single color.") {v->update {it.copy(colorful=v)}})
                add(GuiKit.colorRow(w,"Text Color",s.hud.color) {v->update {it.copy(hud=it.hud.copy(color=v))}})
                add(GuiKit.textRow(w,"New tracked item name",newName,"Exact item display name, ignoring colors and letter case. Add several items separately.") {newName=it})
                add(GuiKit.button("Add tracked item",150) {
                    if(update {it.copy(items=it.items+TrackerItem(newName.trim()))}) {newName="";remount()}
                })
                add(GuiKit.button("Add held item",150) {
                    runCatching {ItemTracker.addHeldItem()}.onSuccess {held->if(update {it.copy(items=it.items+held)})remount()}
                        .onFailure {error=it.message;panel.refreshStatus()}
                })
                s.items.forEachIndexed {i,item->
                    fun change(block:(TrackerItem)->TrackerItem) {update {it.copy(items=it.items.mapIndexed {j,v->if(i==j)block(v) else v})}}
                    add(GuiKit.textRow(w,"Item ${i+1}",names.getOrPut(i) {item.name}) {v->names[i]=v;change {it.copy(name=v)}})
                    add(GuiKit.textRow(w,"SkyBlock item ID (optional)",ids.getOrPut(i) {item.itemId},"Use Add held item to capture an ID. Blank matches the exact display name; names resolve through the public item catalog for pricing.") {v->ids[i]=v;change {it.copy(itemId=v.trim())}})
                    add(GuiKit.textRow(w,"Manual unit price",prices.getOrPut(i) {item.manualPrice.toString()},"Used when the price mode is Manual prices.") {v->
                        prices[i]=v;val n=v.replace(",","").toDoubleOrNull()
                        if(n==null) {error="Manual price must be a finite non-negative number";panel.refreshStatus()} else change {it.copy(manualPrice=n)}
                    })
                    add(GuiKit.button("Remove item",110) {if(update {it.copy(items=it.items.filterIndexed {j,_->j!=i})})remount()})
                }
                add(GuiKit.button("Reset totals and timer",180) {
                    val now=System.nanoTime()/1_000_000L
                    if(now>resetArmedUntil) {resetArmedUntil=now+5000;error="Click Reset again within 5 seconds to confirm"}
                    else {runCatching {ItemTracker.resetTotals()}.onSuccess {error=null}.onFailure {error=it.message};resetArmedUntil=0}
                    panel.refreshStatus()
                })
                addAll(HudGlowUi.controls(ModuleIds.TRACKER,model,panel,w))
            }
        }
        host.showPanel(panel)
    }
}
