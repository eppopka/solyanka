/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.core

import kotlin.test.*
import org.junit.jupiter.api.Test

class ModuleTest {
    private class Probe(id: String = "test.probe") : Module(ModuleInfo(id, id, Category.CORE)) {
        var starts = 0; var stops = 0; var ticks = 0; var resets = 0
        var failStart = false; var failTick = false; var failStop = false
        override fun onEnable() { starts++; if (failStart) error("start") }
        override fun onDisable() { stops++; if (failStop) error("stop") }
        override fun onTick() { ticks++; if (failTick) error("tick") }
        override fun onReset(reason: ResetReason) { resets++ }
    }
    @Test fun `transitions are idempotent`() {
        val p = Probe(); p.setEnabled(true); p.setEnabled(true); p.setEnabled(false); p.setEnabled(false)
        assertEquals(1, p.starts); assertEquals(1, p.stops); assertFalse(p.enabled)
    }
    @Test fun `failed startup is cleaned up`() {
        val p = Probe(); p.failStart = true
        assertFailsWith<IllegalStateException> { p.setEnabled(true) }
        assertFalse(p.enabled); assertEquals(1, p.stops)
    }
    @Test fun `failed tick quarantines module without stopping siblings`() {
        val errors = mutableListOf<String>(); val r = ModuleRegistry { id, _ -> errors.add(id) }
        val bad = Probe("test.bad").also { it.failTick = true; it.setEnabled(true) }
        val good = Probe("test.good").also { it.setEnabled(true) }
        r.register(bad); r.register(good); r.tick(); r.tick()
        assertFalse(bad.enabled); assertEquals(1, bad.ticks); assertEquals(2, good.ticks); assertEquals(1, errors.size)
    }
    @Test fun `disabled modules never receive ticks or resets`() {
        val r = ModuleRegistry { _, _ -> }; val p = Probe(); r.register(p)
        r.tick(); r.reset(ResetReason.DISCONNECTED)
        assertEquals(0, p.ticks); assertEquals(0, p.resets)
    }
    @Test fun `duplicate ids rejected`() {
        val r = ModuleRegistry { _, _ -> }; r.register(Probe())
        assertFailsWith<IllegalArgumentException> { r.register(Probe()) }
    }
    @Test fun `reset preserves enabled state`() {
        val r = ModuleRegistry { _, _ -> }; val p = Probe(); r.register(p); p.setEnabled(true)
        r.reset(ResetReason.WORLD_CHANGED); r.reset(ResetReason.DISCONNECTED)
        assertTrue(p.enabled); assertEquals(2, p.resets)
    }
    @Test fun `shutdown continues after cleanup failure`() {
        val errors = mutableListOf<String>(); val r = ModuleRegistry { id, _ -> errors.add(id) }
        val a = Probe("test.a"); val b = Probe("test.b").also { it.failStop = true }
        listOf(a,b).forEach { r.register(it); it.setEnabled(true) }; r.shutdown()
        assertFalse(a.enabled); assertFalse(b.enabled); assertEquals(1,a.stops); assertEquals(1,errors.size)
    }
}
