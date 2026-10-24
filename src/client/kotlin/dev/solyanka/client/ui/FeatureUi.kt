/* SPDX-License-Identifier: GPL-3.0-only
 * solyanka instant-save embedded editors for Odin/NoFrills-derived settings. */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.InstantSettings
import dev.solyanka.utility.*
import io.wispforest.owo.ui.core.UIComponent

object FeatureUi {
    private class View(val host:NoFrillsClickGui,val model:InstantSettings) {
        val text=mutableMapOf<String,String>()
        val errors=linkedMapOf<String,String>()
        fun error()=errors.values.firstOrNull() ?: model.error
        fun change(panel:SettingsPanel,block:(FeatureSettings)->FeatureSettings) {
            model.updateGeneral { it.copy(features=block(it.features)) };panel.refreshStatus()
        }
        fun number(panel:SettingsPanel,w:Int,key:String,label:String,initial:String,help:String="",apply:(FeatureSettings,Double)->FeatureSettings):UIComponent =
            GuiKit.textRow(w,label,text.getOrPut(key) { initial },help) { raw ->
                text[key]=raw
                try {
                    val number=raw.trim().toDoubleOrNull() ?: throw IllegalArgumentException("$label must be numeric")
                    require(number.isFinite()) { "$label must be finite" }
                    val candidate=apply(model.general.features,number);candidate.validate()
                    errors.remove(key);change(panel) { candidate }
                } catch(e:IllegalArgumentException) { errors[key]=e.message ?: "Invalid $label";panel.refreshStatus() }
            }
    }
    fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
        val v=View(host,model)
        when(id) {
            SolyankaClient.WAYPOINTS->WaypointUi.open(host,model)
            SolyankaClient.AMBIENCE,SolyankaClient.ASPECT,SolyankaClient.FOG,SolyankaClient.DOORS,SolyankaClient.MAP_INFO,SolyankaClient.ICE,SolyankaClient.BLAZE,SolyankaClient.BEAMS,SolyankaClient.MIMIC,SolyankaClient.MIMIC_CHEST,SolyankaClient.PRICES,SolyankaClient.EXPERIMENTS,SolyankaClient.SUPPRESSION -> AdvancedUi.open(id,host,model)
            SolyankaClient.TICK_TIMERS -> timers(v)
            SolyankaClient.PLAYER_DISPLAY -> player(v)
            else -> fixes(v,id)
        }
    }
    private fun whole(n:Double,label:String):Int { require(n%1.0==0.0 && n in 0.0..10000.0) { "$label must be a whole number (0–10000)" };return n.toInt() }
    private fun timers(v:View) {
        v.host.showPanel(SettingsPanel("Tick Timers — Odin",{ v.host.closePanel() },{ v.error() }) { panel,w ->
            val s=v.model.general.features.timers
            fun update(change:(TimerSettings)->TimerSettings)=v.change(panel) { it.copy(timers=change(it.timers)) }
            buildList {
                add(GuiKit.description(w,"All six Odin timers use nonzero server pings, not client ticks. Needs a recognized dungeon floor/boss arena. Every valid change saves instantly."))
                add(GuiKit.toggleRow(w,"Display in Ticks",s.ticks) { value -> update { it.copy(ticks=value) } })
                add(GuiKit.toggleRow(w,"Display Symbol",s.symbols) { value -> update { it.copy(symbols=value) } })
                add(GuiKit.toggleRow(w,"Show Prefix",s.prefix) { value -> update { it.copy(prefix=value) } })
                add(GuiKit.toggleRow(w,"Goldor Start Timer",s.startTimer,"64 server ticks after Storm's closing line.") { value -> update { it.copy(startTimer=value) } })
                add(GuiKit.row(w).apply { child(GuiKit.button("Preview 15 seconds",155) { FeatureRuntime.preview("timers") }) })
                s.huds().forEach { (id,h) ->
                    add(GuiKit.toggleRow(w,timerName(id),h.enabled) { value -> update { current -> current.withHud(id,current.huds().getValue(id).copy(enabled=value)) } })
                    add(GuiKit.row(w).apply { child(GuiKit.button("Position / Scale",125) { hud(v,true,id) }) })
                }
            }
        })
    }
    private fun player(v:View) {
        v.host.showPanel(SettingsPanel("Player Display — Odin",{ v.host.closePanel() },{ v.error() }) { panel,w ->
            val s=v.model.general.features.player
            fun update(change:(PlayerDisplaySettings)->PlayerDisplaySettings)=v.change(panel) { it.copy(player=change(it.player)) }
            buildList {
                add(GuiKit.description(w,"SkyBlock sidebar required. Seven independent HUDs with position/scale/color. Action bar data is parsed before selected fields are hidden."))
                add(GuiKit.toggleRow(w,"Hide Armor Bar",s.hideArmor) { value -> update { it.copy(hideArmor=value) } })
                add(GuiKit.toggleRow(w,"Hide Food Bar",s.hideFood) { value -> update { it.copy(hideFood=value) } })
                add(GuiKit.toggleRow(w,"Hide Hearts",s.hideHearts) { value -> update { it.copy(hideHearts=value) } })
                add(GuiKit.toggleRow(w,"Hide XP Level",s.hideXp,"XP bar stays unchanged, matching Odin.") { value -> update { it.copy(hideXp=value) } })
                add(GuiKit.toggleRow(w,"Hide Action Health",s.hideHealth) { value -> update { it.copy(hideHealth=value) } })
                add(GuiKit.toggleRow(w,"Hide Action Mana",s.hideMana) { value -> update { it.copy(hideMana=value) } })
                add(GuiKit.toggleRow(w,"Hide Action Overflow",s.hideOverflow) { value -> update { it.copy(hideOverflow=value) } })
                add(GuiKit.toggleRow(w,"Hide Action Defense",s.hideDefense) { value -> update { it.copy(hideDefense=value) } })
                add(GuiKit.toggleRow(w,"Hide Action Vitality",s.hideVitality) { value -> update { it.copy(hideVitality=value) } })
                add(GuiKit.toggleRow(w,"Separate Overflow",s.separateOverflow) { value -> update { it.copy(separateOverflow=value) } })
                add(GuiKit.toggleRow(w,"Hide Zero Overflow",s.hideZeroOverflow) { value -> update { it.copy(hideZeroOverflow=value) } })
                add(GuiKit.toggleRow(w,"Show Icons",s.icons) { value -> update { it.copy(icons=value) } })
                s.huds().forEach { (id,h) ->
                    add(GuiKit.toggleRow(w,id.replaceFirstChar { it.uppercase() }+" HUD",h.enabled) { value -> update { current -> current.withHud(id,current.huds().getValue(id).copy(enabled=value)) } })
                    add(GuiKit.row(w).apply { child(GuiKit.button("Position / Scale / Color",175) { hud(v,false,id) }) })
                }
            }
        })
    }
    private fun hud(v:View,timer:Boolean,id:String) {
        val title=if(timer) timerName(id) else id.replaceFirstChar { it.uppercase() }+" HUD"
        v.host.showPanel(SettingsPanel(title,{ if(timer) timers(v) else player(v) },{ v.error() }) { panel,w ->
            val h=if(timer) v.model.general.features.timers.huds().getValue(id) else v.model.general.features.player.huds().getValue(id)
            fun apply(f:FeatureSettings,block:(HudSpec)->HudSpec)=if(timer) f.copy(timers=f.timers.withHud(id,block(f.timers.huds().getValue(id)))) else f.copy(player=f.player.withHud(id,block(f.player.huds().getValue(id))))
            val key=(if(timer) "timer" else "player")+".$id"
            buildList {
                add(GuiKit.description(w,"GUI-scaled pixels; rendering clamps to the visible screen. Scale 0.5–4."))
                if(timer) add(GuiKit.row(w).apply { child(GuiKit.button("Preview 15 seconds",155) { FeatureRuntime.preview("timers") }) })
                add(v.number(panel,w,"$key.x","X",h.x.toString()) { f,n -> apply(f) { it.copy(x=whole(n,"X")) } })
                add(v.number(panel,w,"$key.y","Y",h.y.toString()) { f,n -> apply(f) { it.copy(y=whole(n,"Y")) } })
                add(v.number(panel,w,"$key.scale","Scale",h.scale.toString()) { f,n -> apply(f) { it.copy(scale=n) } })
                if(!timer) add(GuiKit.colorRow(w,"Color",h.color) { color ->
                    v.change(panel) { f -> apply(f) { it.copy(color=color) } }
                })
            }
        })
    }
    private fun fixes(v:View,id:String) {
        val title=when(id) { SolyankaClient.ANIMATION->"Animation Fix";SolyankaClient.MIDDLE_FIX->"Middle Click Fix";SolyankaClient.MIDDLE_OVERRIDE->"Middle Click Override";SolyankaClient.CURSOR->"No Cursor Reset";else->"No Loading Screen" }
        v.host.showPanel(SettingsPanel(title,{ v.host.closePanel() },{ v.error() }) { panel,w ->
            val f=v.model.general.features.fixes
            fun update(block:(UtilityFixSettings)->UtilityFixSettings)=v.change(panel) { it.copy(fixes=block(it.fixes)) }
            when(id) {
                SolyankaClient.ANIMATION -> listOf(
                    GuiKit.description(w,"Filters the first server POSE metadata entry for your local player only; preserves every other metadata entry and other players. No outgoing packet automation."),
                    GuiKit.toggleRow(w,"SkyBlock Only",f.animationSkyblockOnly) { value -> update { it.copy(animationSkyblockOnly=value) } }
                )
                SolyankaClient.MIDDLE_FIX -> listOf(GuiKit.description(w,"Allows the configured pick-item click to reach vanilla's CLONE path in survival container screens. Does not grant creative items. Turn the feature card off to restore vanilla behavior."))
                SolyankaClient.MIDDLE_OVERRIDE -> listOf(
                    GuiKit.description(w,"Only eligible manual left PICKUP clicks in SkyBlock chest menus become middle CLONE. Vanilla inventories, shift-clicks, carried items, transactions in blacklisted menus and loadout editing are protected. Review server rules; test harmless menus first."),
                    GuiKit.toggleRow(w,"Debug Feedback",f.middleDebug) { value -> update { it.copy(middleDebug=value) } }
                )
                SolyankaClient.CURSOR -> listOf(
                    GuiKit.description(w,"Remembers the cursor when returning to gameplay and restores it on GUI reopen. Expiry counts only in-world ticks with no GUI. World changes/disable clear it; Spirit Leap remains centered."),
                    v.number(panel,w,"cursor.expiry","Clear After Ticks",f.cursorClearTicks.toString(),"0–1200; 0 retains until reset.") { features,n -> features.copy(fixes=features.fixes.copy(cursorClearTicks=whole(n,"Clear After Ticks"))) }
                )
                else -> listOf(
                    GuiKit.description(w,"Safety adaptation: hides LevelLoadingScreen foreground/background only. Vanilla loading ticks, readiness and input blocking remain intact. Does not hide disconnect/error/resource-pack screens or accelerate loading."),
                    GuiKit.toggleRow(w,"Remote Server Only",f.loadingServerOnly,"Excludes singleplayer and LAN when enabled.") { value -> update { it.copy(loadingServerOnly=value) } }
                )
            }
        })
    }
    private fun timerName(id:String)=when(id) { "goldor"->"Goldor";"pad"->"Storm Pad";"lightning"->"Storm Lightning";"py"->"Storm PY";"storm"->"Storm Phase";else->"M3 Fire Freeze" }
}
