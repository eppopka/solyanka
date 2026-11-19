/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonObject
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.GuiCatalog
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test

class Release1481Test {
    @Test fun `Vanguard window accepts guild rank formats only and ignores self`() {
        val w=VanguardInviteWindow();w.begin(1000)
        assertNull(w.accept("Party > [MVP+] Guest: +","Owner","+",1001))
        assertNull(w.accept("Officer > Guest: +","Owner","+",1001))
        assertNull(w.accept("From Guest: +","Owner","+",1001))
        assertNull(w.accept("Guest: +","Owner","+",1001))
        assertNull(w.accept("Guild > Owner: +","Owner","+",1001))
        assertEquals("Guest",w.accept("Guild > [MVP+] Guest [Member]: +","Owner","+",1001))
        assertEquals(1,w.count)
    }
    @Test fun `Vanguard reserves at most three unique case insensitive names`() {
        val w=VanguardInviteWindow();w.begin(0)
        assertEquals("Alice",w.accept("Guild > Alice: +","Owner","+",1))
        assertNull(w.accept("Guild > ALICE: +","Owner","+",2))
        assertEquals("Bob",w.accept("Guild > Bob: +","Owner","+",3))
        assertEquals("Carol",w.accept("Guild > Carol: +","Owner","+",4))
        assertNull(w.accept("Guild > Fourth: +","Owner","+",5));assertEquals(3,w.count)
    }
    @Test fun `Vanguard timeout is exactly sixty seconds and does not slide`() {
        val w=VanguardInviteWindow();assertFalse(w.active(0));w.begin(1000)
        assertNull(w.accept("Guild > Early: +","Owner","+",999))
        assertEquals("Last",w.accept("Guild > Last: +","Owner","+",60_999))
        assertNull(w.accept("Guild > Late: +","Owner","+",61_000))
        assertFalse(w.active(61_000));assertFalse(w.active(61_001))
    }
    @Test fun `new Vanguard encounter resets cap dedup and deadline`() {
        val w=VanguardInviteWindow();w.begin(0)
        assertNotNull(w.accept("Guild > Alice: +","Owner","+",1))
        w.clear();assertNull(w.accept("Guild > Bob: +","Owner","+",2))
        w.begin(100_000);assertEquals(0,w.count)
        assertEquals("Alice",w.accept("Guild > Alice: +","Owner","+",100_001))
        assertTrue(w.active(159_999));assertFalse(w.active(160_000))
    }
    @Test fun `Vanguard custom keyword is exact and cannot inject commands`() {
        val w=VanguardInviteWindow();w.begin(0)
        assertNull(w.accept("Guild > Alice: ++","Owner","+",1))
        assertNull(w.accept("Guild > Alice: invite me now","Owner","invite me",1))
        assertNotNull(w.accept("Guild > Alice: invite me","Owner","invite me",1))
        assertNull(w.accept("Guild > Mallory: Guild > Victim: +","Owner","+",1))
        assertNull(w.accept("Guild > bad/name: +","Owner","+",1))
        assertNull(w.accept("Guild > Bob: /p victim","Owner","+",1))
    }
    @Test fun `Vanguard settings preserve requested defaults and validate safely`() {
        val s=VanguardAnnounceSettings();s.validate()
        assertEquals("/gc I got Vanguard, write '+' for an invite.",s.message)
        assertEquals("+",s.keyword);assertEquals("I got Vanguard, write '+' for an invite.",s.announcement())
        assertEquals("hello",s.copy(message="hello").announcement())
        assertFails {s.copy(message="/p victim").validate()}
        assertFails {s.copy(message="hello\n/gc bad").validate()}
        assertFails {s.copy(keyword=" ").validate()}
        assertFails {s.copy(keyword=" +").validate()}
        assertFails {s.copy(keyword="x".repeat(65)).validate()}
    }
    @Test fun `corpse hide radius applies to all visuals at inclusive boundary`() {
        assertFalse(CorpseVisibility.visible(false,25.0,5.0))
        assertFalse(CorpseVisibility.visible(false,24.999,5.0))
        assertTrue(CorpseVisibility.visible(false,25.001,5.0))
        assertFalse(CorpseVisibility.visible(true,10000.0,5.0))
        assertFalse(CorpseVisibility.visible(true,0.0,0.0))
        assertTrue(CorpseVisibility.visible(false,0.0,0.0))
        assertFalse(CorpseVisibility.visible(false,Double.NaN,5.0))
    }
    @Test fun `Performance adaptation retains TPS FPS Ping ordering and selectable direction`() {
        val s=PerformanceSettings();s.validate()
        val metrics=PerformanceLayout.metrics(s,120,"42 ms",19.5)
        assertEquals(listOf("TPS: ","FPS: ","Ping: "),metrics.map {it.label})
        assertEquals(listOf("TPS: 19.5 | FPS: 120 | Ping: 42 ms"),PerformanceLayout.lines(s,metrics))
        assertEquals(3,PerformanceLayout.lines(s.copy(direction="vertical"),metrics).size)
        assertEquals(1,PerformanceLayout.metrics(s.copy(showTps=false,showPing=false),99,"Local",null).size)
        assertTrue(PerformanceLayout.metrics(s.copy(showTps=false,showFps=false,showPing=false),99,"Local",null).isEmpty())
        assertFails {s.copy(direction="diagonal").validate()}
    }
    @Test fun `new defaults migrate without overwriting explicit corpse coordinate preferences`() {
        val f=FeatureCodec.decode(JsonObject());assertTrue(f.mineshaft.coordinates);assertTrue(f.mineshaft.hideOnClick)
        assertEquals("horizontal",f.performance.direction)
        assertEquals("+",f.vanguardAnnounce.keyword)
        val old=JsonObject().apply {add("mineshaft",JsonObject().apply {addProperty("coordinates",false)})}
        assertFalse(FeatureCodec.decode(old).mineshaft.coordinates)
        assertEquals("Mining",GuiCatalog.features.single {it.id==ModuleIds.VANGUARD_ANNOUNCE}.category)
    }
    private fun source(name:String)=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/$name"))
    @Test fun `corpse renderer uses frame camera orientation and partial tick entity positions`() {
        val s=source("feature/MiningHelpers.kt")
        val render=s.substringAfter("fun collect(").substringBefore("/** Explicit export")
        assertTrue(render.contains("cameraRenderState"));assertTrue(render.contains("getGameTimeDeltaPartialTick(true)"))
        assertTrue(render.contains("stand.getPosition(partial)"));assertTrue(render.contains("camera.orientation.transform"))
        assertTrue(render.contains("CorpseVisibility.visible"))
        assertFalse(s.contains("val origin=c.gameRenderer.mainCamera()"))
        assertTrue(source("feature/WorldOverlay.kt").contains("MiningHelpers.collect(c,snapshot,text)"))
        assertTrue(source("InputRouter.kt").contains("MiningHelpers.click(key)"))
    }
    @Test fun `corpse coordinates use both NBT layouts and are independent of visibility`() {
        val s=source("feature/MiningHelpers.kt")
        assertTrue(s.contains("getCompoundOrEmpty(\"ExtraAttributes\")"))
        assertTrue(s.contains("CorpseKind.fromHelmet(data.getStringOr(\"id\",\"\"))"))
        assertTrue(s.contains("s.coordinates && corpse.stand.isAlive && printed.add(corpse.id)"))
        assertTrue(s.contains("Corpse: x: ${'$'}{b.x}, y: ${'$'}{b.y}, z: ${'$'}{b.z}"))
    }
    @Test fun `Tracker bounded caches reduce repeated metadata and chat work without removing transfer wait`() {
        val s=source("feature/ItemTracker.kt")
        assertTrue(s.contains("IdentityHashMap<ItemStack,StackMatch>"))
        assertTrue(s.contains("stackMatches.size>=128"));assertTrue(s.contains("tick%20==0)stackMatches.clear()"))
        assertTrue(s.contains("idIndex="));assertTrue(s.contains("nameIndex="))
        assertTrue(s.contains("tick%2==0"));assertTrue(s.contains("tick%2==0)pollSacks()"))
        assertTrue(s.contains("all.take(16)"));assertTrue(s.contains("!active)inventory.reset()"))
        assertTrue(s.contains("time-pending.peek().time>=750"))
    }
    @Test fun `Vanguard network sends remain gated and expire queued invitations too`() {
        val s=source("feature/VanguardAnnounce.kt")
        assertTrue(s.contains("if(!window.active(time)) {queue.clear();return}"))
        assertTrue(s.contains("window.begin(time)"));assertTrue(s.contains("keyword=s.keyword"))
        assertTrue(s.contains("sendCommand(\"gc ${'$'}{s.announcement()}\")"))
        assertTrue(s.contains("sendCommand(\"p ${'$'}{queue.remove()}\")"))
        assertTrue(s.contains("RngAnnounce.hypixel()"));assertTrue(s.contains("FeatureRuntime.context.skyblock"))
        assertFalse(s.contains("sendChat("))
    }
}
