/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import kotlin.test.*
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import com.google.gson.Gson
import dev.solyanka.utility.*

class SliderControlsTest {
    @Test fun `right click reverses mode and wraps at first option`() {
        val modes=SettingsCatalog.choices.getValue("clock")
        assertEquals("Dusk",modes[SettingsCatalog.nextChoice(3,modes.size,1)])
        assertEquals("Noon",modes[SettingsCatalog.nextChoice(4,modes.size,-1)])
        assertEquals(6,SettingsCatalog.nextChoice(0,7,-1))
        assertEquals(0,SettingsCatalog.nextChoice(6,7,1))
        for(size in 1..10) for(i in 0 until size) assertEquals(i,SettingsCatalog.nextChoice(SettingsCatalog.nextChoice(i,size,1),size,-1))
        assertFails {SettingsCatalog.nextChoice(0,7,0)}
    }
    @Test fun `sliders snap decimal steps without leaking floating point noise`() {
        val r=NumberRange(.3,3.0,.01)
        assertEquals(.34,r.snap(.344));assertEquals(.35,r.snap(.346))
        assertEquals("0.35",r.label(r.snap(.346)))
        assertEquals(.3,r.snap(-1.0));assertEquals(3.0,r.snap(9.0))
        assertFails {r.snap(Double.NaN)}
    }
    @Test fun `integer sliders produce valid whole values`() {
        val r=SettingsNumbers.range("fixes","cursorClearTicks",320,180,200.0)
        assertEquals(201.0,r.snap(200.7));assertEquals(1200.0,r.snap(1201.0))
        assertEquals(100.0,SettingsNumbers.range("chat","lines",320,180,1000.0).min)
        assertEquals(512.0,SettingsNumbers.range("chat","feedbackLimit",320,180,0.0).max)
    }
    @Test fun `position sliders preserve old coordinates beyond current window`() {
        assertEquals(1000.0,SettingsNumbers.range("hud","x",320,180,1000.0).max)
        assertEquals(179.0,SettingsNumbers.range("map","y",320,180,30.0).max)
    }
    @Test fun `every advanced numeric field has either modes colors or a slider range`() {
        val root=Gson().toJsonTree(FeatureSettings()).asJsonObject
        SettingsCatalog.panels.values.forEach { spec -> spec.fields.forEach { field ->
            val value=root.getAsJsonObject(spec.section)[field].asJsonPrimitive
            if(value.isNumber && field !in SettingsCatalog.colors && field !in SettingsCatalog.choices) {
                val range=SettingsNumbers.range(spec.section,field,320,180,value.asDouble)
                assertTrue(value.asDouble in range.min..range.max)
            }
        } }
        val way=root.getAsJsonObject("waypoints")
        way.entrySet().filter {it.value.asJsonPrimitive.isNumber && it.key !in setOf("type","color")}.forEach {
            SettingsNumbers.range("waypoints",it.key,320,180,it.value.asDouble)
        }
    }
    private fun ui(file:String)=Path.of("src/client/kotlin/dev/solyanka/client/ui/$file").readText()
    @Test fun `UI contains sliders compact HUD rows and explicit right click handling`() {
        val kit=ui("nofrills/GuiKit.kt")
        assertTrue(kit.contains("SlimSliderComponent("));assertTrue(kit.contains("fun hudRow("))
        assertTrue(kit.contains("if(click.button()==1) {cycle(-1);return true}"))
        assertTrue(ui("FeatureUi.kt").contains("GuiKit.hudRow(w,timerName(id)"))
        assertFalse(ui("AdvancedUi.kt").contains("GuiKit.textRow("))
        assertFalse(ui("FeatureUi.kt").contains("GuiKit.textRow("))
    }
    @Test fun `timed preview implementation and commands are removed`() {
        val base=Path.of("src/client/kotlin/dev/solyanka/client")
        val client=base.resolve("SolyankaClient.kt").readText()
        assertFalse(client.contains("literal(\"preview\")"))
        assertFalse(base.resolve("feature/FeatureRuntime.kt").readText().contains("previewUntil"))
        assertTrue(client.contains("literal(\"gui\").executes { openHudRequested=true;1 }"))
        assertTrue(client.contains("client.setScreenAndShow(dev.solyanka.client.ui.HudEditorScreen())"))
        val editor=ui("HudEditorScreen.kt")
        assertTrue(editor.contains("HudLayout.drag("));assertTrue(editor.contains("override fun mouseScrolled("))
        assertFalse(editor.contains("root.surface("))
    }
}
