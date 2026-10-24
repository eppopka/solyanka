/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class HudLayoutTest {
    @Test fun `editor covers every implemented movable HUD`() {
        val all=HudLayout.entries(FeatureSettings())
        assertEquals(17,all.size);assertEquals(17,all.map {it.key}.distinct().size)
        assertEquals(6,all.count {it.key.startsWith("timer.")});assertEquals(7,all.count {it.key.startsWith("player.")})
        assertFalse(all.first {it.key=="map"}.resizable)
    }
    @Test fun `moving a player HUD keeps all other settings`() {
        val f=FeatureSettings()
        val next=HudLayout.update(f,"player.health") {it.copy(x=150,y=70,scale=2.0)}
        assertEquals(f.copy(player=f.player.copy(health=f.player.health.copy(x=150,y=70,scale=2.0))),next)
    }
    @Test fun `timer visibility is independent from other timer HUDs`() {
        val f=FeatureSettings();val next=HudLayout.update(f,"timer.goldor") {it.copy(enabled=false)}
        assertFalse(next.timers.goldor.enabled);assertEquals(f.timers.pad,next.timers.pad)
        assertEquals(f.player,next.player)
    }
    @Test fun `map layout only changes stored map coordinates`() {
        val f=FeatureSettings();val next=HudLayout.update(f,"map") {it.copy(x=40,y=80)}
        assertEquals(f.copy(map=f.map.copy(x=40,y=80)),next)
    }
    @Test fun `invalid or unknown HUD edits are rejected`() {
        assertFails {HudLayout.update(FeatureSettings(),"fake") {it}}
        assertFails {HudLayout.update(FeatureSettings(),"timer.goldor") {it.copy(x=-1)}}
        assertFails {HudLayout.update(FeatureSettings(),"player.speed") {it.copy(scale=5.0)}}
    }
    @Test fun `bounds clamp using scaled text dimensions`() {
        val b=HudLayout.bounds(HudSpec(x=999,y=999,scale=2.0),100,2,320,180)
        assertEquals(HudBounds(112,136,208,44),b)
        assertTrue(b.contains(112.0,136.0));assertFalse(b.contains(320.0,180.0))
    }
    @Test fun `drag preserves pointer offset and stays on screen`() {
        val h=HudSpec(x=30,y=40);val b=HudLayout.bounds(h,60,1,320,180)
        val dragged=HudLayout.drag(h,100.0,110.0,5.0,6.0,b,320,180)
        assertEquals(95,dragged.x);assertEquals(104,dragged.y)
        assertEquals(0,HudLayout.drag(h,-100.0,-100.0,5.0,5.0,b,320,180).x)
        assertEquals(256,HudLayout.drag(h,999.0,999.0,0.0,0.0,b,320,180).x)
    }
    @Test fun `very small windows still have valid nonnegative bounds`() {
        val b=HudLayout.bounds(HudSpec(x=100,y=100,scale=4.0),300,3,20,10)
        assertEquals(HudBounds(0,0,20,10),b)
        assertEquals(0,HudLayout.drag(HudSpec(),50.0,50.0,0.0,0.0,b,20,10).y)
    }
    @Test fun `editor-like edits use immediate validated persistence`() {
        var saved=GeneralSettings();var calls=0
        val m=InstantSettings(CommandProfile(),saved) {_,g->saved=g;calls++;null}
        m.updateGeneral {it.copy(features=HudLayout.update(it.features,"timer.pad") {h->h.copy(x=80)})}
        assertEquals(80,saved.features.timers.pad.x);assertEquals(1,calls)
        val bad=InstantSettings(CommandProfile(),saved) {_,_->"read only"}
        assertFalse(bad.updateGeneral {it.copy(features=HudLayout.update(it.features,"player.mana") {h->h.copy(y=90)})})
        assertNotNull(bad.error);assertEquals(144,saved.features.player.mana.y)
    }
}
