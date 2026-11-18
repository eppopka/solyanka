/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ChatCommandTargetsTest {
    private class CountingRandom:Random() {
        private val delegate=Random(19)
        var calls=0
        override fun nextBits(bitCount:Int):Int {calls++;return delegate.nextBits(bitCount)}
    }
    @Test fun `target IGN replaces the sender only when explicitly supplied`() {
        val settings=ChatCommandsSettings(commands=listOf(ChatCommand("iq","{name}'s iq: {randint(60,100)}")))
        val input=ChatCommandsLogic.parse("3ppopka: !iq Player123")!!
        val result=ChatCommandsLogic.response(settings,input,Random(1))!!
        assertTrue(result.startsWith("Player123's iq: "))
        assertTrue(ChatCommandsLogic.response(settings,input.copy(text="!iq"),Random(1))!!.startsWith("3ppopka's iq: "))
        assertNull(ChatCommandsLogic.response(settings,input.copy(text="!iq /pc")))
        assertNull(ChatCommandsLogic.response(settings,input.copy(text="!iq Player123 extra")))
    }
    @Test fun `two minute random results are shared by target case and all requesters but not sliding`() {
        val cache=ChatCommandResultCache();val random=CountingRandom()
        val command=ChatCommand("iq","{target} iq {randint(1,999999)} requested by {sender}")
        val a=cache.response(command,"Player123","Alice",1000,random)!!
        val used=random.calls
        val b=cache.response(command,"player123","Bob",120999,random)!!
        assertEquals(used,random.calls)
        assertEquals(a.substringAfter(" iq ").substringBefore(" requested"),b.substringAfter(" iq ").substringBefore(" requested"))
        assertTrue(b.endsWith("requested by Bob"))
        cache.response(command,"Player123","Alice",121000,random)
        assertTrue(random.calls>used)
    }
    @Test fun `cache isolates commands targets templates and clock rollbacks`() {
        val cache=ChatCommandResultCache();val random=CountingRandom();val command=ChatCommand("iq","{name} {randint(1,999999)}")
        cache.response(command,"One","A",1000,random);var old=random.calls
        cache.response(command,"Two","A",1001,random);assertTrue(random.calls>old);old=random.calls
        cache.response(command.copy(name="luck"),"One","A",1002,random);assertTrue(random.calls>old);old=random.calls
        cache.response(command.copy(response="new {randint(1,999999)}"),"One","A",1003,random);assertTrue(random.calls>old);old=random.calls
        cache.response(command,"One","A",999,random);assertTrue(random.calls>old)
        assertNull(cache.response(ChatCommand("bad","/p kick {target}"),"One","A",1004,random))
    }
    @Test fun `chat command response rows autosave without remounting on keystrokes`() {
        val source=Files.readString(Path.of("src/client/kotlin/dev/solyanka/client/ui/ChatCommandsUi.kt"))
        assertFalse(source.contains("GuiKit.button(\"Save\""))
        assertTrue(source.contains("replies[i]=it;saveRow()"))
        assertTrue(source.contains("names[i]=it;saveRow()"))
        val save=source.substringAfter("fun saveRow()").substringBefore("fun delete()")
        assertFalse(save.contains("panel.mount"))
    }
    @Test fun `Tracker is wired into module HUD lifecycle and shared opt in prices`() {
        val root=Path.of("src/client/kotlin/dev/solyanka/client")
        val tracker=Files.readString(root.resolve("feature/ItemTracker.kt"))
        assertFalse(tracker.contains("sendCommand("));assertFalse(tracker.contains("sendChat("))
        assertTrue(tracker.contains("TrackerInventoryDelta()"));assertTrue(tracker.contains("TrackerLogic.sackChanges"))
        assertTrue(tracker.contains("if(!writable)return"))
        val prices=Files.readString(root.resolve("feature/PriceTooltips.kt"))
        assertTrue(prices.contains("if(!tooltipOnline && !trackerOnline) return"))
        assertTrue(prices.contains("f.tracker.onlinePrices"))
        assertTrue(Files.readString(root.resolve("ui/TrackerUi.kt")).contains("HudGlowUi.controls(ModuleIds.TRACKER"))
    }
}
