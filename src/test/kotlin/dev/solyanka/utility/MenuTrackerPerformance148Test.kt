/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test

class MenuTrackerPerformance148Test {
    @Test fun `remote frozen game time uses measured packet cadence instead of zero`() {
        val t=ServerTpsEstimator()
        t.observe(6000,0,true);assertNull(t.value(0))
        t.observe(6000,1_000_000_000,true)
        assertEquals(20.0,t.value(1_000_000_000));assertTrue(t.usingPacketCadence)
        assertEquals(2L,t.updates)
    }
    @Test fun `slow remote frozen game clock does not fabricate twenty TPS`() {
        val t=ServerTpsEstimator();t.observe(6000,0,true);t.observe(6000,2_000_000_000,true)
        assertEquals(10.0,t.value(2_000_000_000))
        assertNull(t.value(8_000_000_000))
    }
    @Test fun `remote backwards clock can recover via time-packet cadence`() {
        val t=ServerTpsEstimator();t.observe(24000,0,true);t.observe(0,1_000_000_000,true)
        assertEquals(20.0,t.value(1_000_000_000));assertTrue(t.usingPacketCadence)
        t.observe(20,2_000_000_000,true)
        assertEquals(20.0,t.value(2_000_000_000));assertFalse(t.usingPacketCadence)
    }
    @Test fun `local paused clock remains zero and reset requires a fresh baseline`() {
        val t=ServerTpsEstimator();t.observe(5,0);t.observe(5,1_000_000_000)
        assertEquals(0.0,t.value(1_000_000_000));assertFalse(t.usingPacketCadence)
        t.reset();assertEquals(0L,t.updates);assertNull(t.value(2_000_000_000))
        t.observe(20,2_000_000_000);assertNull(t.value(2_000_000_000))
    }
    @Test fun `stale gaps and nonmonotonic observation times discard samples`() {
        val t=ServerTpsEstimator();t.observe(0,0,true);t.observe(0,1_000_000_000,true)
        t.observe(0,40_000_000_000,true);assertNull(t.value(40_000_000_000))
        t.observe(0,39_000_000_000,true);assertNull(t.value(39_000_000_000))
    }
    @Test fun `Tracker semantic spans preserve full item text and distinguish numbers`() {
        val line="Flint x13,943 (194,293 coins)"
        val spans=TrackerHudStyle.spans(line,-1)
        assertEquals(line,spans.joinToString("") {it.text})
        assertEquals(5,spans.size);assertEquals(-1,spans[0].color)
        assertNotEquals(spans[0].color,spans[1].color)
        assertNotEquals(spans[1].color,spans[3].color)
        assertEquals(TrackerHudStyle.COINS and 0xffffff,spans[3].color and 0xffffff)
    }
    @Test fun `Tracker header totals hourly estimates and unknown prices use distinct accents`() {
        val title=TrackerHudStyle.spans("Tracker · 00:12:30 [paused]",-1)
        assertEquals("Tracker · 00:12:30 [paused]",title.joinToString("") {it.text})
        assertEquals(3,title.map {it.color}.distinct().size)
        val total=TrackerHudStyle.spans("Total: 194,293 coins",-1)
        val hourly=TrackerHudStyle.spans("Profit/h*: 932,606 coins",-1)
        assertNotEquals(total.last().color,hourly.last().color)
        val unknown=TrackerHudStyle.spans("Flint x1 (? coins)",-1)
        assertEquals(TrackerHudStyle.WARNING and 0xffffff,unknown[3].color and 0xffffff)
    }
    @Test fun `Tracker monochrome opt-out and configured opacity are retained`() {
        val color=0x80554433.toInt()
        val line="Flint x13,943 (194,293 coins)"
        assertEquals(listOf(TrackerHudSpan(line,color)),TrackerHudStyle.spans(line,color,false))
        assertTrue(TrackerHudStyle.spans(line,color).all {it.color ushr 24==0x80})
        assertTrue(FeatureCodec.decode(JsonObject()).tracker.colorful)
        val old=JsonObject().apply {add("tracker",JsonObject().apply {addProperty("background",false)})}
        assertTrue(FeatureCodec.decode(old).tracker.colorful)
        val disabled=JsonObject().apply {add("tracker",JsonObject().apply {addProperty("colorful",false)})}
        assertTrue(FeatureCodec.decode(disabled).tracker.colorful)
    }
    private fun source(name:String)=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/$name"))
    @Test fun `settings rebuild preserves offset and does not restart opening animation`() {
        val s=source("ui/nofrills/SettingsPanel.kt")
        assertTrue(s.contains("savedScroll=if(::scroll.isInitialized)scroll.offset() else initialScroll"))
        assertTrue(s.contains("scroll.restoreOffset(savedScroll)"))
        assertTrue(s.contains("savedPosition?.let {drag.place(it)}"))
        assertTrue(s.contains("if(!rebuilding) openedAt="))
        assertTrue(s.indexOf("scroll.restoreOffset(savedScroll)")>s.indexOf("adapter.inflateAndMount()"))
        val callback=s.substringAfter("fun rebuild(").substringBefore("fun inheritView(")
        assertFalse(callback.contains("dispose()"));assertFalse(callback.contains("mount("))
        assertTrue(s.contains("pendingSize=null;mount(it.first,it.second)"))
    }
    @Test fun `category refresh saves horizontal and per-category scroll rather than resetting`() {
        val s=source("ui/nofrills/NoFrillsClickGui.kt")
        assertTrue(s.contains("categoryOffsets[name]=scroll.offset()"))
        assertTrue(s.contains("scroll.restoreOffset(categoryOffsets[name] ?: 0.0)"))
        assertTrue(s.contains("mainScroll.restoreOffset(horizontal)"))
        assertTrue(s.contains("inheritView(panel as? SettingsPanel)"))
        for(name in listOf("TrackerUi.kt","ChatCommandsUi.kt","WaypointUi.kt")) {
            val editor=source("ui/$name")
            assertFalse(editor.contains("panel.mount(host.width,host.height)"))
            assertTrue(editor.contains("panel.rebuild(host.width,host.height)"))
        }
    }
    @Test fun `Performance UI exposes packet diagnostics and actual Tracker draw uses semantic spans`() {
        val performance=source("feature/PerformanceHud.kt")
        assertTrue(performance.contains("cadenceFallback=!Minecraft.getInstance().hasSingleplayerServer()"))
        assertTrue(source("ui/PerformanceUi.kt").contains("PerformanceHud.status()"))
        val tracker=source("feature/ItemTracker.kt")
        assertTrue(tracker.contains("TrackerHudStyle.spans(line,s.hud.color)"))
        assertTrue(tracker.contains("span.color,true"))
    }
}
