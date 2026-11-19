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
                add(HudNavigation.button(w))
                add(GuiKit.toggleRow(w,"Online prices",s.onlinePrices,"Downloads public prices; no inventory data is uploaded.") {v->update {it.copy(onlinePrices=v)}})
                val modes=listOf("instant_sell","sell_offer","lowest_bin","npc","manual")
                val priceNames=listOf("Bazaar instant-sell","Bazaar sell-offer estimate","Lowest BIN","NPC sell","Manual prices")
                add(GuiKit.button("Prices: "+priceNames[modes.indexOf(s.priceMode)],(w-8).coerceAtMost(310)) {
                    if(update {it.copy(priceMode=modes[(modes.indexOf(it.priceMode)+1)%modes.size])})remount()
                })
                val resetModes=listOf("session","never","world_change")
                val resetNames=listOf("Session","Never","World Change")
                add(GuiKit.button("Reset mode: "+resetNames[resetModes.indexOf(s.persistence)],(w-8).coerceAtMost(280)) {
                    if(update {it.copy(persistence=resetModes[(resetModes.indexOf(it.persistence)+1)%resetModes.size])})remount()
                })
                add(GuiKit.button(if(s.activityMode=="item") "AFK mode: Until tracked item" else "AFK mode: Movement or clicks",(w-8).coerceAtMost(300)) {
                    if(update {it.copy(activityMode=if(it.activityMode=="item") "movement" else "item")})remount()
                })
                add(GuiKit.toggleRow(w,"Track Sacks",s.countSacks,"Requires per-item Sacks message details.") {v->update {it.copy(countSacks=v)}})
                add(GuiKit.toggleRow(w,"Compact values",s.compactValues,"319,284 coins → 319k") {v->update {it.copy(compactValues=v)}})
                add(GuiKit.toggleRow(w,"Background",s.background) {v->update {it.copy(background=v)}})
                add(GuiKit.colorRow(w,"Text Color",s.hud.color) {v->update {it.copy(hud=it.hud.copy(color=v))}})
                add(GuiKit.textRow(w,"New tracked item name",newName,"Exact item name.") {newName=it})
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
                    add(GuiKit.textRow(w,"SkyBlock item ID (optional)",ids.getOrPut(i) {item.itemId},"Blank = match by name.") {v->ids[i]=v;change {it.copy(itemId=v.trim())}})
                    add(GuiKit.textRow(w,"Manual unit price",prices.getOrPut(i) {item.manualPrice.toString()},"Used in Manual price mode.") {v->
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
