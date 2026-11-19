/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test

class Release1482Test {
    @Test fun `compact coin values truncate rather than round and preserve full mode`() {
        assertEquals("319k",TrackerValues.format(319284.0,true))
        assertEquals("319,284 coins",TrackerValues.format(319284.0,false))
        assertEquals("999k",TrackerValues.format(999999.0,true))
        assertEquals("1m",TrackerValues.format(1000000.0,true))
        assertEquals("1b",TrackerValues.format(1999999999.0,true))
        assertEquals("999",TrackerValues.format(999.9,true))
        assertEquals("0",TrackerValues.format(0.0,true))
        assertEquals("?",TrackerValues.format(null,true))
        assertEquals("?",TrackerValues.format(Double.NaN,true))
        assertEquals("?",TrackerValues.format(-1.0,true))
    }
    @Test fun `compact item row retains all semantic color spans`() {
        for(line in listOf("Flint x13,943 (319k)","Flint x13,943 (319,284 coins)","Flint x1 (?)")) {
            val spans=TrackerHudStyle.spans(line,-1)
            assertEquals(line,spans.joinToString("") {it.text})
            assertEquals(5,spans.size)
        }
    }
    @Test fun `AFK waits a full minute and rolls back that minute`() {
        val c=TrackerClock();c.step(0,true,true,false,false)
        c.step(30000,true,false,false,false);assertFalse(c.paused);assertEquals(30000L,c.elapsedMillis)
        c.step(59999,true,false,false,false);assertFalse(c.paused)
        c.step(60000,true,false,false,false);assertTrue(c.paused);assertEquals(0L,c.elapsedMillis)
        c.step(61000,true,true,false,false);assertFalse(c.paused);assertEquals(0L,c.elapsedMillis)
    }
    @Test fun `world change is a valid reset mode and compact formatting migrates off`() {
        TrackerSettings(persistence="world_change").validate()
        assertFalse(FeatureCodec.decode(JsonObject()).tracker.compactValues)
        assertFails {TrackerSettings(persistence="sometimes").validate()}
    }
    @Test fun `legacy hidden colorful and non SkyBlock toggles no longer disable required defaults`() {
        val tracker=JsonObject().apply {
            addProperty("colorful",false);addProperty("skyblockOnly",false)
            add("hud",JsonObject().apply {addProperty("enabled",false)})
        }
        val f=FeatureCodec.decode(JsonObject().apply {add("tracker",tracker)})
        assertTrue(f.tracker.colorful);assertTrue(f.tracker.skyblockOnly);assertTrue(f.tracker.hud.enabled)
    }
    @Test fun `long help is concise but short statuses remain unchanged`() {
        assertEquals("Connected",UiHelp.summary("Connected"))
        assertTrue(UiHelp.summary("A".repeat(300)).length<=110)
        assertTrue(UiHelp.summary("Windows only. Captures ONLY a window "+"detail ".repeat(100)).contains("no uploads"))
    }
    private fun source(name:String)=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/$name"))
    @Test fun `Tracker editor removes redundant toggles and paragraphs`() {
        val s=source("ui/TrackerUi.kt")
        for(label in listOf("Colorful Tracker HUD","Visible item rows","SkyBlock only","HUD Visible"))assertFalse(s.contains("\"$label\""))
        assertFalse(s.contains("GuiKit.description("))
        assertTrue(s.contains("Compact values"));assertTrue(s.contains("World Change"))
        val runtime=source("feature/ItemTracker.kt")
        assertFalse(runtime.contains("cfg().hud.enabled"));assertFalse(runtime.contains("!cfg().skyblockOnly"))
        assertTrue(runtime.contains("TrackerHudStyle.spans(line,s.hud.color)"))
    }
    @Test fun `Sacks capture has no broad ten second menu block and tracks local hover versions`() {
        val s=source("feature/ItemTracker.kt")
        val capture=s.substringAfter("private fun sack(").substringBefore("private fun pollSacks(")
        assertFalse(capture.contains("time<ignoreUntil"))
        assertTrue(s.contains("ClientReceiveMessageEvents.MODIFY_GAME.register"))
        assertTrue(s.contains("title.contains(\"sack\",true)"))
        assertFalse(s.contains("sackIgnoreUntil=time+10_000"))
        assertFalse(s.contains("sackIgnoreUntil=now()+10_000"))
        assertTrue(s.contains("previous!=version"));assertTrue(s.contains("if(primed &&"))
        assertTrue(s.contains("time-pending.peek().time>=750"))
        assertTrue(s.contains("if(eligible())acceptPending(time)"))
    }
    @Test fun `world resets work while disabled without changing Session persistence`() {
        val s=source("feature/ItemTracker.kt")
        assertTrue(s.contains("if(cfg().persistence==\"world_change\")sessions.clear()"))
        assertTrue(source("ClientModuleCatalog.kt").contains("ItemTracker.worldChanged()"))
        assertTrue(source("SolyankaClient.kt").contains("if(!isEnabled(dev.solyanka.core.ModuleIds.TRACKER))dev.solyanka.client.feature.ItemTracker.worldChanged()"))
    }
    @Test fun `so redirects every subcommand to the same solyanka root`() {
        val s=source("Commands.kt")
        assertTrue(s.contains("val solyanka=dispatcher.register(literal(\"solyanka\")"))
        assertTrue(s.contains("literal(\"so\").executes {S.requestSettings();1}.redirect(solyanka)"))
    }
    @Test fun `full descriptive help survives on hover across panels`() {
        val s=source("ui/nofrills/GuiKit.kt")
        assertTrue(s.contains("UiHelp.summary(text)"));assertTrue(s.contains("tooltip(Component.literal(text))"))
        assertTrue(s.contains("if(subtitle.isNotBlank())tooltip(Component.literal(subtitle))"))
    }
}
