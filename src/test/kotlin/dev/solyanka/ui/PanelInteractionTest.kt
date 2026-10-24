/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText
class PanelInteractionTest {
 private fun drag()=PanelDrag().apply {layout(960,540,400,350)}
 @Test fun `panel starts centered`() {assertEquals(PanelDrag.Point(280,95),drag().position)}
 @Test fun `header drag retains grab offset`() {val d=drag();assertTrue(d.begin(290.0,105.0,0,true));assertEquals(PanelDrag.Point(390,190),d.move(400.0,200.0))}
 @Test fun `right click cannot start window drag`() {val d=drag();assertFalse(d.begin(300.0,110.0,1,true));assertFalse(d.dragging)}
 @Test fun `slider or body click cannot start window drag`() {val d=drag();assertFalse(d.begin(300.0,200.0,0,false));assertNull(d.move(400.0,300.0))}
 @Test fun `drag clamps top and left`() {val d=drag();d.begin(280.0,95.0,0,true);assertEquals(PanelDrag.Point(0,0),d.move(-500.0,-300.0))}
 @Test fun `drag clamps bottom and right`() {val d=drag();d.begin(280.0,95.0,0,true);assertEquals(PanelDrag.Point(560,190),d.move(9999.0,9999.0))}
 @Test fun `release ends window movement`() {val d=drag();d.begin(280.0,95.0,0,true);assertTrue(d.end());assertNull(d.move(500.0,400.0));assertFalse(d.end())}
 @Test fun `resize preserves position where possible`() {val d=drag();d.place(PanelDrag.Point(30,40));d.layout(1000,600,400,350);assertEquals(PanelDrag.Point(30,40),d.position)}
 @Test fun `resize clamps stored position and cancels drag`() {val d=drag();d.begin(280.0,95.0,0,true);d.layout(320,240,300,200);assertEquals(PanelDrag.Point(20,40),d.position);assertFalse(d.dragging)}
 @Test fun `oversized panel never gets negative coordinates`() {val d=PanelDrag();d.layout(20,10,400,350);assertEquals(PanelDrag.Point(0,0),d.position)}
 @Test fun `invalid pointer cannot corrupt state`() {val d=drag();assertFalse(d.begin(Double.NaN,0.0,0,true));d.begin(280.0,95.0,0,true);assertNull(d.move(Double.POSITIVE_INFINITY,5.0));assertEquals(PanelDrag.Point(280,95),d.position)}
 @Test fun `opening only queues navigation`() {val n=PanelNavigation<String>();n.open("held item");assertTrue(n.pending());assertEquals("held item",n.take()!!.panel);assertFalse(n.pending());assertNull(n.take())}
 @Test fun `latest open request wins`() {val n=PanelNavigation<String>();n.open("first");n.open("second");assertEquals("second",n.take()!!.panel)}
 @Test fun `close supersedes queued open`() {val n=PanelNavigation<String>();n.open("held item");n.close();assertNotNull(n.take().also {assertNull(it!!.panel)})}
 @Test fun `new open supersedes pending close`() {val n=PanelNavigation<String>();n.close();n.open("new");assertEquals("new",n.take()!!.panel)}
 @Test fun `dispose clears queued work`() {val n=PanelNavigation<String>();n.open("held item");n.clear();assertFalse(n.pending());assertNull(n.take())}
 private fun source(name:String)=Path.of("src/client/kotlin/dev/solyanka/client/ui/nofrills/$name").readText()
 @Test fun `screen navigation is deferred until frame boundary`() {val s=source("NoFrillsClickGui.kt");assertTrue("fun showPanel(next:SettingsPanel) {navigation.open(next)}" in s);assertTrue(s.indexOf("commitNavigation()\n        // A modal panel")>0);assertTrue(s.indexOf("commitNavigation()\n        // A modal panel")<s.indexOf("if(panel==null) super.extractRenderState"));assertTrue(s.indexOf("next.mount(width,height)")<s.indexOf("panel?.dispose();panel=next"))}
 @Test fun `header drag never remounts or writes feature settings`() {val s=source("SettingsPanel.kt");val body=s.substringAfter("fun mouseDragged(").substringBefore("fun mouseReleased(");assertTrue("drag.move(" in body);assertFalse("mount(" in body);assertFalse("updateGeneral" in body);assertFalse("root.surface(" in s)}
 @Test fun `palette construction is lazy and after explicit edit action`() {val s=source("GuiKit.kt").substringAfter("fun colorRow(").substringBefore("fun choiceRow(");assertTrue(s.indexOf("edit.onPress {")<s.indexOf("ColorPickerComponent()"));assertTrue("container.child(palette)" in s);assertTrue("catch(e:Exception)" in s)}
 @Test fun `settings failures are logged without claiming crash diagnosis`() {val s=source("NoFrillsClickGui.kt");assertTrue("logger.error(\"Cannot open settings panel\",e)" in s);assertTrue("Settings panel input failed" in s);assertTrue("Settings panel rendering failed" in s)}
}
