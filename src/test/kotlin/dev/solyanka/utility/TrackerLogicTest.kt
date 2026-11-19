/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlin.test.*
import org.junit.jupiter.api.Test
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.*

class TrackerLogicTest {
    @Test fun `sixty seconds AFK rolls back the whole idle interval`() {
        val c=TrackerClock()
        c.step(0,true,true,false,false)
        c.step(10_000,true,false,false,false);assertEquals(10_000L,c.elapsedMillis)
        c.step(59_999,true,false,false,false);assertEquals(59_999L,c.elapsedMillis)
        c.step(60_000,true,false,false,false);assertEquals(0L,c.elapsedMillis);assertTrue(c.paused)
        c.step(100_000,true,true,false,false);assertEquals(0L,c.elapsedMillis);assertFalse(c.paused)
        c.step(101_000,true,true,false,false);assertEquals(1_000L,c.elapsedMillis)
    }
    @Test fun `brief inactivity counts and earlier active time survives rollback`() {
        val c=TrackerClock(5000)
        c.step(0,true,true,false,false);c.step(1000,true,true,false,false)
        c.step(5000,true,false,false,false);assertEquals(10_000L,c.elapsedMillis)
        c.step(6000,true,true,false,false);assertEquals(11_000L,c.committedMillis)
        c.step(66_000,true,false,false,false);assertEquals(11_000L,c.elapsedMillis)
    }
    @Test fun `item mode waits for actual tracked gains and ignores movement for resume`() {
        val c=TrackerClock()
        c.step(0,true,true,false,true);assertTrue(c.paused);assertEquals(0L,c.elapsedMillis)
        c.step(10_000,true,false,true,true);assertFalse(c.paused);assertEquals(0L,c.elapsedMillis)
        c.step(20_000,true,true,false,true);assertEquals(10_000L,c.elapsedMillis)
        c.step(70_000,true,true,false,true);assertTrue(c.paused);assertEquals(0L,c.elapsedMillis)
        c.step(80_000,true,true,false,true);assertTrue(c.paused)
        c.step(90_000,true,false,true,true);assertFalse(c.paused);assertEquals(0L,c.elapsedMillis)
    }
    @Test fun `disconnects do not count offline time and checkpoints omit provisional idle time`() {
        val c=TrackerClock()
        c.step(0,true,true,false,false);c.step(1000,true,true,false,false)
        c.step(2000,true,false,false,false);assertEquals(1000L,c.committedMillis)
        c.step(2000,false,false,false,false);assertEquals(2000L,c.elapsedMillis)
        c.step(100_000,true,true,false,false);assertEquals(2000L,c.elapsedMillis)
        c.step(101_000,true,true,false,false);assertEquals(3000L,c.elapsedMillis)
    }
    @Test fun `inventory baseline and GUI transfers are not acquisitions`() {
        val d=TrackerInventoryDelta()
        assertTrue(d.observe(mapOf("flint" to 100L),false).isEmpty())
        assertEquals(mapOf("flint" to 20L),d.observe(mapOf("flint" to 120L),false))
        assertTrue(d.observe(mapOf("flint" to 120L),false).isEmpty())
        assertTrue(d.observe(mapOf("flint" to 999L),true).isEmpty())
        assertTrue(d.observe(mapOf("flint" to 999L),false).isEmpty())
        d.reset();assertTrue(d.observe(mapOf("flint" to 1500L),false).isEmpty())
    }
    @Test fun `inventory to sack transfers are matched across sources not genuine same-source gains`() {
        val losses=TrackerTransferLosses()
        losses.record("flint","inventory",20,1000)
        assertEquals(0L,losses.consume("flint","sacks",15,1500))
        assertEquals(5L,losses.consume("flint","sacks",10,1600))
        losses.record("flint","inventory",20,2000)
        assertEquals(10L,losses.consume("flint","inventory",10,2500))
        assertEquals(10L,losses.consume("flint","sacks",10,5001))
    }
    @Test fun `sacks require per-item hover details and parse both signs`() {
        val changes=TrackerLogic.sackChanges(listOf("Added items", " §a+1,234 Flint (Mining Sack)"," -10 Flint (Mining Sack)"," +2 Enchanted Flint (Mining Sack)","[Sacks] +999 items"))
        assertEquals(mapOf("flint" to 1224L,"enchanted flint" to 2L),changes)
        assertTrue(TrackerLogic.sackChanges(listOf("+10 items", "Party > Player: +10 Flint (Mining Sack)")).isEmpty())
    }
    @Test fun `price modes are explicit unknown is not zero and hourly math uses active time`() {
        val item=TrackerItem("Flint",manualPrice=7.5)
        val price=MarketPrice(buy=20.0,sell=15.0,bin=25.0,npc=1.0)
        assertEquals(15.0,TrackerLogic.unitPrice(item,"instant_sell",price))
        assertEquals(20.0,TrackerLogic.unitPrice(item,"sell_offer",price))
        assertEquals(25.0,TrackerLogic.unitPrice(item,"lowest_bin",price))
        assertEquals(1.0,TrackerLogic.unitPrice(item,"npc",price))
        assertEquals(7.5,TrackerLogic.unitPrice(item,"manual",null))
        assertNull(TrackerLogic.unitPrice(item,"instant_sell",null))
        assertEquals(25.0,TrackerLogic.unitPrice(item,"instant_sell",price.copy(sell=0.0)))
        assertEquals(30000.0,TrackerLogic.perHour(1000.0,120_000))
        assertNull(TrackerLogic.perHour(1000.0,0));assertEquals("01:02:03",TrackerLogic.duration(3_723_000))
    }
    @Test fun `persistent totals codec rejects fractional counts corruption and unbounded data`() {
        val value=mapOf("player_uuid" to TrackerSaved(123456,mapOf("flint" to 13943L)))
        assertEquals(value,TrackerDataCodec.decode(TrackerDataCodec.encode(value)))
        assertFails {TrackerDataCodec.decode("""{"version":1,"accounts":{"me":{"elapsedMillis":0,"counts":{"flint":1.5}}}}""")}
        assertFails {TrackerDataCodec.decode("""{"version":2,"accounts":{}}""")}
        assertFails {TrackerSaved(-1).validate()}
        assertFails {TrackerDataCodec.decode("x".repeat(1_048_577))}
    }
    @Test fun `Tracker settings migrate validate and fully participate in movable HUD layouts`() {
        val defaults=FeatureCodec.decode(JsonObject());assertEquals("session",defaults.tracker.persistence)
        assertFalse(defaults.tracker.onlinePrices);assertTrue(defaults.tracker.skyblockOnly)
        val f=defaults.copy(tracker=TrackerSettings(items=listOf(TrackerItem("Flint"),TrackerItem("Enchanted Flint")),persistence="never"))
        assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
        assertFails {f.copy(tracker=f.tracker.copy(items=listOf(TrackerItem("Flint"),TrackerItem("flint")))).validate()}
        assertFails {TrackerSettings(priceMode="guess").validate()}
        assertFails {TrackerSettings(items=listOf(TrackerItem("Flint",manualPrice=Double.NaN))).validate()}
        assertEquals(ModuleIds.TRACKER,HudLayout.owner("extra.tracker"))
        assertEquals(listOf("extra.tracker"),HudLayout.visibleEntries(f,setOf(ModuleIds.TRACKER)).map {it.key})
        val moved=HudLayout.update(f,"extra.tracker") {it.copy(x=77,scale=1.5)}
        assertEquals(77,moved.tracker.hud.x);assertEquals(1.5,moved.tracker.hud.scale)
        assertEquals(moved,HudLayouts.apply(f,HudLayouts.capture(moved)))
        for(id in listOf("hud.tracker","hud.spotify","hud.yandex_music")) {
            val changed=f.hudGlow.withEnabled(id,true)
            assertTrue(changed.enabled(id));assertTrue(listOf("hud.tracker","hud.spotify","hud.yandex_music").filter {it!=id}.none {changed.enabled(it)})
        }
    }
}
