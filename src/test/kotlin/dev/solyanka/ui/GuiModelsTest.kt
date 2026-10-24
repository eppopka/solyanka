/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class GuiModelsTest {
    @Test fun `catalog only contains implemented unique module cards`() {
        val cards=GuiCatalog.search("").values.flatten()
        assertEquals(setOf("hud.scoreboard","hud.themed_chat","render.player_glow","render.nametags","render.block_outline","mining.titanium","hud.inventory","hud.item_name","hud.skill_progress","render.ambience","render.aspect","render.fog","dungeon.waypoints","dungeon.door_highlight","dungeon.map_info","solver.ice_fill","solver.blaze","solver.creeper_beams","dungeon.mimic","custom.mimic_chest","utility.price_tooltips","solver.experiments","render.suppression","utility.command_keybinds","utility.command_shortcuts","core.diagnostics","utility.chat","utility.info_tooltips","dungeon.tick_timers","hud.player_display","render.animation_fix","utility.middle_click_fix","utility.middle_click_override","utility.cursor","utility.loading_screen"),cards.map { it.id }.toSet())
        assertEquals(35,cards.size)
    }
    @Test fun `search ignores case and whitespace and includes descriptions`() {
        assertEquals("utility.command_keybinds",GuiCatalog.search("  COMMAND   keyBINDS ").values.flatten().single().id)
        assertEquals("utility.command_keybinds",GuiCatalog.search("dungeon class").values.flatten().single().id)
        assertTrue(GuiCatalog.search("not-a-feature").isEmpty())
    }
    @Test fun `cards are sorted within categories`() {
        GuiCatalog.search("").values.forEach { assertEquals(it.sortedBy { c -> c.name },it) }
    }
    @Test fun `draft stages bind changes without mutating initial profile`() {
        val bind=CommandBinding(id="one",name="Original")
        val original=CommandProfile(binds=listOf(bind));val draft=CommandGuiDraft(original)
        draft.updateBind("one") { it.copy(name="Edited",command="/help",enabled=true) }
        assertTrue(draft.changed());assertEquals("Original",original.binds.single().name)
        assertEquals("Edited",draft.validated().binds.single().name)
    }
    @Test fun `new entries are disabled and removing them restores draft`() {
        val draft=CommandGuiDraft(CommandProfile());val b=draft.addBind();val s=draft.addShortcut()
        assertFalse(b.enabled);assertEquals(-1,b.key);assertFalse(s.enabled);assertTrue(draft.changed())
        draft.removeBind(b.id);draft.removeShortcut(s.id);assertFalse(draft.changed())
    }
    @Test fun `stable IDs survive editor changes`() {
        val draft=CommandGuiDraft(CommandProfile());val b=draft.addBind();val s=draft.addShortcut()
        draft.updateBind(b.id) { it.copy(name="Named") };draft.updateShortcut(s.id) { it.copy(alias="dh") }
        assertEquals(b.id,draft.profile.binds.single().id);assertEquals(s.id,draft.profile.shortcuts.single().id)
    }
    @Test fun `invalid IDs and stale editor updates cannot mutate profile`() {
        val draft=CommandGuiDraft(CommandProfile());val b=draft.addBind();val before=draft.profile
        assertFailsWith<IllegalArgumentException> { draft.updateBind("missing") { it } }
        assertFailsWith<IllegalArgumentException> { draft.updateBind(b.id) { it.copy(id="other") } }
        assertEquals(before,draft.profile)
    }
    @Test fun `invalid staged entries fail validation without changing source`() {
        val original=CommandProfile();val draft=CommandGuiDraft(original);val s=draft.addShortcut()
        draft.updateShortcut(s.id) { it.copy(alias="solyanka",message="/help",enabled=true) }
        assertFailsWith<IllegalArgumentException> { draft.validated() }
        assertTrue(original.shortcuts.isEmpty())
    }
    @Test fun `list limit enforced before add`() {
        val draft=CommandGuiDraft(CommandProfile(binds=List(128) { CommandBinding() },shortcuts=List(128) { Shortcut() }))
        assertFailsWith<IllegalArgumentException> { draft.addBind() }
        assertFailsWith<IllegalArgumentException> { draft.addShortcut() }
        assertEquals(128,draft.profile.binds.size);assertEquals(128,draft.profile.shortcuts.size)
    }
    @Test fun `global GUI option is staged`() {
        val original=CommandProfile();val draft=CommandGuiDraft(original);draft.allowAllInGui(true)
        assertFalse(original.allowAllInGui);assertTrue(draft.validated().allowAllInGui)
    }
}
