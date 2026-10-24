/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class SkillProgressTest {
    @Test fun `ratio mode captures gain progress and table-derived level`() {
        val t=SkillProgress();assertTrue(t.overlay("+9.2 Farming (12,345/20,000)",100))
        val s=t.snapshot!!;assertEquals(SkillKind.FARMING,s.kind);assertEquals(12345L,s.current);assertEquals(20000L,s.needed)
        assertEquals(14,s.level);assertEquals(15,s.nextLevel);assertEquals("+9.2",s.gained)
        assertEquals("12,345/20,000",SkillProgress.progressText(s))
    }
    @Test fun `percent only never fabricates an absolute XP amount without evidence`() {
        val t=SkillProgress();t.overlay("+12.3 Mining (22.49%)",0)
        val s=t.snapshot!!;assertNull(s.current);assertNull(s.needed);assertNull(s.level)
        assertEquals("22.49%",SkillProgress.progressText(s))
    }
    @Test fun `percent mode uses observed tab level or earlier ratio`() {
        val t=SkillProgress();t.tab("Farming: 14");t.overlay("+9.2 Farming (50%)")
        assertEquals(10000L,t.snapshot!!.current);assertEquals(20000L,t.snapshot!!.needed)
        t.reset();t.overlay("+2 Combat (10/50)");t.overlay("+1 Combat (50%)")
        assertEquals(25L,t.snapshot!!.current)
    }
    @Test fun `level caches do not bleed across skills`() {
        val t=SkillProgress();t.overlay("+2 Combat (10/50)");t.overlay("+1 Farming (25%)")
        assertNull(t.snapshot!!.needed);assertEquals(SkillKind.FARMING,t.snapshot!!.kind)
    }
    @Test fun `MAXED and dungeon alias are recognized`() {
        val t=SkillProgress();assertTrue(t.overlay("+3 Dungeoneering (MAXED)"))
        val s=t.snapshot!!;assertEquals(SkillKind.CATACOMBS,s.kind);assertTrue(s.maxed);assertEquals(50,s.level)
        assertEquals("MAXED",SkillProgress.progressText(s))
        assertTrue(t.overlay("+1 social (max)"));assertEquals(25,t.snapshot!!.level)
    }
    @Test fun `all supported skills parse names and have correct table lengths`() {
        for(kind in SkillKind.entries) {
            val t=SkillProgress();assertTrue(t.overlay("+1 ${kind.label} (1/50)"),kind.label)
            assertEquals(kind,t.snapshot!!.kind)
        }
        assertEquals(60,SkillKind.FARMING.maxLevel);assertEquals(25,SkillKind.RUNECRAFTING.maxLevel)
        assertEquals(25,SkillKind.SOCIAL.maxLevel);assertEquals(50,SkillKind.CATACOMBS.maxLevel)
    }
    @Test fun `formatted unicode and compact quantities are accepted`() {
        val t=SkillProgress();assertTrue(t.overlay("§a＋9.2 ✦ Farming\u00a0(12.3k/20k) 200/300❤"))
        assertEquals(12300L,t.snapshot!!.current);assertEquals(20000L,t.snapshot!!.needed)
    }
    @Test fun `malformed impossible progress preserves last good snapshot`() {
        val t=SkillProgress();t.overlay("+1 Farming (1/50)");val before=t.snapshot
        for(text in listOf("+1 Farming (101%)","+1 Farming (-1%)","+1 Farming (5/0)","+1 Farming (99/50)","+1 Farming (1,2/50)","+1 Farming (NaN%)","+1 Unknown (1/50)","Nothing here")) {
            assertFalse(t.overlay(text),text);assertEquals(before,t.snapshot)
        }
    }
    @Test fun `quantity parser handles suffixes and rejects unsafe values`() {
        assertEquals(1500L,SkillProgress.amount("1.5k"));assertEquals(2000000L,SkillProgress.amount("2M"));assertEquals(1000000000L,SkillProgress.amount("1b"));assertEquals(12345L,SkillProgress.amount("12,345"))
        for(raw in listOf("NaN","Infinity","-1","1e12","1,2",".5","9".repeat(33))) assertNull(SkillProgress.amount(raw),raw)
    }
    @Test fun `menu observations are bounded valid level hints`() {
        val t=SkillProgress();t.menuItem("Farming\nLevel: 14\nRewards");t.overlay("+1 Farming (25%)")
        assertEquals(5000L,t.snapshot!!.current)
        t.reset();t.remember(SkillKind.FARMING,99);t.overlay("+1 Farming (25%)");assertNull(t.snapshot!!.needed)
    }
    @Test fun `unknown server denominator is preserved without claiming a table level`() {
        val t=SkillProgress();t.overlay("+1 Mining (20/1234)")
        assertEquals(1234L,t.snapshot!!.needed);assertNull(t.snapshot!!.level)
        t.overlay("+1 Mining (50%)");assertEquals(617L,t.snapshot!!.current)
    }
    @Test fun `reset clears snapshot and learned skill data`() {
        val t=SkillProgress();t.tab("Farming 14");t.overlay("+1 Farming (10%)");t.reset()
        assertNull(t.snapshot);t.overlay("+1 Farming (10%)");assertNull(t.snapshot!!.current)
    }
    @Test fun `optional timeout uses monotonic time and zero keeps latest`() {
        val t=SkillProgress();t.overlay("+1 Farming (10%)",1_000_000_000)
        assertNotNull(t.visible(5,5_999_999_999));assertNull(t.visible(5,6_000_000_001))
        assertNotNull(t.visible(0,99_000_000_000));assertNull(t.visible(1,0))
    }
    @Test fun `table returned to callers cannot mutate future inference`() {
        val a=SkillXp.table(SkillKind.FARMING);a[15]=99
        assertEquals(20000L,SkillXp.after(SkillKind.FARMING,14))
        assertNull(SkillXp.next(SkillKind.FARMING,123456));assertNull(SkillXp.after(SkillKind.FARMING,60))
    }
}
