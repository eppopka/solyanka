/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.solyanka.utility.*
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText

class SettingsCatalogTest {
    @Test fun `every exposed field exists in its config section`() {
        val root=Gson().toJsonTree(FeatureSettings()).asJsonObject
        assertEquals(13,SettingsCatalog.panels.size)
        SettingsCatalog.panels.values.forEach { panel ->
            assertTrue(panel.fields.isNotEmpty())
            assertEquals(panel.fields.size,panel.fields.distinct().size)
            panel.fields.forEach { assertTrue(root.getAsJsonObject(panel.section).has(it),"${panel.title}: $it") }
        }
    }
    @Test fun `features do not share environment or puzzle controls`() {
        for(section in listOf("environment","puzzles","map")) {
            val fields=SettingsCatalog.panels.values.filter { it.section==section }.flatMap { it.fields }
            assertEquals(fields.size,fields.distinct().size,"Shared setting in $section")
        }
        assertEquals(listOf("aspectRatio"),SettingsCatalog.panels.getValue("render.aspect").fields)
        assertEquals(listOf("mimicChestColor"),SettingsCatalog.panels.getValue("custom.mimic_chest").fields)
    }
    @Test fun `mode buttons preserve serialized ordinals and wrap`() {
        assertEquals(listOf("Unchanged","Sunny","Rain","Thunder","Snow"),SettingsCatalog.choices.getValue("weather"))
        assertEquals("Outside Rift",SettingsCatalog.choices.getValue("healthBar")[1])
        assertEquals("Both",SettingsCatalog.choices.getValue("vignette")[3])
        assertEquals("Etherwarp",SettingsCatalog.waypointTypes[3])
        (SettingsCatalog.choices.values+listOf(SettingsCatalog.waypointTypes)).forEach { choices ->
            assertEquals(0,SettingsCatalog.nextChoice(choices.lastIndex,choices.size))
            choices.dropLast(1).indices.forEach { assertEquals(it+1,SettingsCatalog.nextChoice(it,choices.size)) }
        }
        assertFailsWith<IllegalArgumentException> { SettingsCatalog.nextChoice(0,0) }
    }
    @Test fun `legacy shared depth setting migrates without changing existing choice`() {
        val f=FeatureCodec.decode(JsonParser.parseString("""{"puzzles":{"depthTest":false}}""").asJsonObject)
        assertFalse(f.puzzles.iceDepthTest);assertFalse(f.puzzles.blazeDepthTest);assertFalse(f.puzzles.beamDepthTest)
        assertTrue(FeatureCodec.decode(null).puzzles.iceDepthTest)
    }
    @Test fun `individual solver depth values override legacy and remain independent`() {
        val f=FeatureCodec.decode(JsonParser.parseString("""{"puzzles":{"depthTest":false,"iceDepthTest":true,"beamDepthTest":true}}""").asJsonObject)
        assertTrue(f.puzzles.iceDepthTest);assertFalse(f.puzzles.blazeDepthTest);assertTrue(f.puzzles.beamDepthTest)
        val next=FeatureCodec.decode(Gson().toJsonTree(f.copy(puzzles=f.puzzles.copy(blazeDepthTest=true))).asJsonObject)
        assertTrue(next.puzzles.blazeDepthTest);assertTrue(next.puzzles.iceDepthTest)
    }
    @Test fun `invalid depth values still reject malformed settings`() {
        for(json in listOf("""{"puzzles":{"depthTest":"false"}}""","""{"puzzles":{"iceDepthTest":0}}""")) {
            assertFails { FeatureCodec.decode(JsonParser.parseString(json).asJsonObject) }
        }
    }
    private fun client(file:String)=Path.of("src/client/kotlin/dev/solyanka/client/ui/$file").readText()
    @Test fun `palette and settings remain embedded without world dimming`() {
        val panel=client("nofrills/SettingsPanel.kt")
        assertFalse(panel.contains("Changes save instantly."))
        assertFalse(panel.contains("root.surface("))
        assertFalse(client("nofrills/NoFrillsClickGui.kt").contains("Surface.VANILLA_TRANSLUCENT"))
        val kit=client("nofrills/GuiKit.kt")
        assertTrue(kit.contains("ColorPickerComponent()"));assertTrue(kit.contains("UIComponents.box("))
        assertTrue(kit.contains("Edit color"));assertTrue(kit.contains("container.child(palette)"))
        assertFalse(kit.contains("setScreen"));assertTrue(panel.contains("Not saved:"))
    }
    @Test fun `player settings and nested hud editor have no player preview`() {
        val ui=client("FeatureUi.kt")
        val player=ui.substringAfter("private fun player(").substringBefore("private fun hud(")
        assertFalse(player.contains("Preview"))
        assertFalse(ui.contains("FeatureRuntime.preview(\"player\")"))
        assertFalse(ui.contains("FeatureRuntime.preview(if(timer)"))
        assertFalse(ui.contains("Preview 15 seconds"))
        assertTrue(ui.contains("GuiKit.colorRow(w,\"Color\",h.color)"))
        assertFalse(ui.contains("Color (#RRGGBB)"))
    }
}
