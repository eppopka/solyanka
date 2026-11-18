/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test
import dev.solyanka.model.GuiCatalog

class Mining148Test {
    @Test fun `standard drops belong to the local player and accept a or an`() {
        assertNotNull(RngAnnounceLogic.parse("[MVP+] Test_User just found an Alloy!","Test_User"))
        assertNotNull(RngAnnounceLogic.parse("Test_User found a Rare Item!","Test_User"))
        assertNull(RngAnnounceLogic.parse("Other just found an Alloy!","Test_User"))
        assertNull(RngAnnounceLogic.parse("Party > Test_User: Test_User just found an Alloy!","Test_User"))
        assertNull(RngAnnounceLogic.parse("[MVP+] Test_User: Test_User just found an Alloy!","Test_User"))
    }
    @Test fun `SkyHanni prices enforce inclusive minimum without suppressing configured unknowns`() {
        val drop=RngAnnounceLogic.parse("[SkyHanni] +Tracker Drop: Rare Item (29.9M)","Me")!!
        assertFalse(RngAnnounceLogic.allowed(drop,RngAnnounceSettings()))
        assertTrue(RngAnnounceLogic.allowed(drop.copy(value=30000000.0),RngAnnounceSettings()))
        assertEquals(1234567.0,RngAnnounceLogic.price("1,234,567 coins"))
        assertEquals(1500000000.0,RngAnnounceLogic.price("1.5B"))
        assertNull(RngAnnounceLogic.price("NaN"))
        assertFalse(RngAnnounceLogic.allowed(drop.copy(value=null),RngAnnounceSettings(announceUnknownValue=false)))
    }
    @Test fun `dedup handles duplicate callbacks and tracker echoes with bounded lifetime`() {
        val d=DropDeduplicator()
        assertTrue(d.accept("Rare Item",1000));assertFalse(d.accept("rare   item",1001))
        assertTrue(d.accept("Rare Item",32000));d.clear();assertTrue(d.accept("Rare Item",32001))
    }
    @Test fun `TAB corpse counts preserve duplicate corpse types and looted corpses`() {
        val counts=MineshaftLogic.corpses(listOf(" Lapis: NOT LOOTED"," Lapis: LOOTED"," Vanguard: NOT LOOTED","not a corpse"))
        assertEquals(2,counts[CorpseKind.LAPIS]);assertEquals(1,counts[CorpseKind.VANGUARD])
        assertTrue(MineshaftLogic.summary(counts).startsWith("[solyanka]"))
        assertEquals(CorpseKind.TUNGSTEN,CorpseKind.fromHelmet("MINERAL_HELMET"))
        assertNull(CorpseKind.fromHelmet("DIAMOND_HELMET"))
    }
    @Test fun `new settings reject invalid radii and prices`() {
        assertFailsWith<IllegalArgumentException> {HidePlayersSettings(distance=Double.NaN).validate()}
        assertFailsWith<IllegalArgumentException> {MineshaftSettings(tracerRadius=-1.0).validate()}
        assertFailsWith<IllegalArgumentException> {RngAnnounceSettings(minimumMillions=Double.POSITIVE_INFINITY).validate()}
        assertTrue(PowderChestSettings().tracer)
    }
    @Test fun `Spotify can edit the shared coverless media HUD`() {
        val rows=dev.solyanka.model.HudLayout.visibleEntries(FeatureSettings(),setOf(dev.solyanka.core.ModuleIds.SPOTIFY))
        assertEquals(listOf("extra.music"),rows.map {it.key})
        assertEquals(150,rows.single().baseWidth)
        val value=FeatureSettings(rngAnnounce=RngAnnounceSettings(allChat=true),mineshaft=MineshaftSettings(vanguard=CorpseStyle(123,false)))
        assertEquals(value,FeatureCodec.decode(com.google.gson.Gson().toJsonTree(value).asJsonObject))
        assertEquals("Littlefoot's Den",MineshaftLogic.typeName("LITT_L"))
    }
    @Test fun `recent column is rightmost and category moves retain stable IDs`() {
        assertEquals("Recently changed",GuiCatalog.search("",recent=listOf("hud.performance")).keys.last())
        val cards=GuiCatalog.features.associateBy {it.id}
        assertEquals("HUD",cards.getValue(GuiCatalog.GUI_SETTINGS).category)
        assertEquals("Mining",cards.getValue("mining.titanium").category)
        assertEquals("Mining",cards.getValue("utility.spread_guesser").category)
        assertEquals("General",cards.getValue("custom.invisibug_esp").category)
    }
}
