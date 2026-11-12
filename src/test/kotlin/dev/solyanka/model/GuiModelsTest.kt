/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class GuiModelsTest {
    @Test fun `catalog only contains implemented unique module cards`() {
        val cards=GuiCatalog.search("").values.flatten()
        assertEquals(setOf("hud.performance","dev.gui_settings","utility.spread_guesser","custom.item_aura","cosmetic.cs2_knife","custom.shitter_list","custom.move_hotbar","render.picture_in_picture","custom.pet_display","custom.bestiary_esp","custom.invisibug_esp","custom.tiki_esp","custom.china_hat","custom.halo","custom.secret_counter","custom.ambient_particles","custom.rotating_item","custom.move_hearts","render.motion_blur","custom.stopwatch","custom.dvd_bounce","render.skybox","cosmetic.held_item","hud.scoreboard","hud.themed_chat","render.player_glow","render.nametags","render.block_outline","mining.titanium","hud.inventory","hud.item_name","hud.skill_progress","render.ambience","render.aspect","render.fog","dungeon.waypoints","render.chat_commands","hud.yandex_music","dungeon.leap_message","dungeon.door_highlight","dungeon.map_info","dungeon.puzzle_solvers","dungeon.mimic","custom.mimic_chest","utility.price_tooltips","solver.experiments","render.suppression","utility.command_keybinds","utility.command_shortcuts","utility.chat","utility.info_tooltips","dungeon.tick_timers","hud.player_display","render.animation_fix","utility.middle_click_fix","utility.middle_click_override","utility.cursor","utility.loading_screen"),cards.map { it.id }.toSet())
        assertEquals(58,cards.size)
    }
    @Test fun `waypoints retains its id in visual`() {
        val card=GuiCatalog.search("waypoints").values.flatten().single { it.id=="dungeon.waypoints" }
        assertEquals("Visual",card.category)
    }
    @Test fun `reorganized cards have requested categories`() {
        val cards=GuiCatalog.features.associateBy {it.id}
        mapOf("dungeon.puzzle_solvers" to "Dungeons","solver.experiments" to "General","render.chat_commands" to "General", "render.picture_in_picture" to "HUD", "custom.item_aura" to "Visual","custom.halo" to "Visual","custom.china_hat" to "Visual","cosmetic.cs2_knife" to "Visual","custom.bestiary_esp" to "ESP").forEach { (id,category)->assertEquals(category,cards.getValue(id).category) }
        assertEquals(setOf("Dungeons","General","Visual","HUD","ESP","Dev"),cards.values.map {it.category}.toSet())
        assertTrue(PuzzleSolverModules.children.none {it in cards});assertFalse(cards.containsKey("custom.always_water"))
    }
    @Test fun `search ignores case and whitespace and includes descriptions`() {
        assertEquals("utility.command_keybinds",GuiCatalog.search("  COMMAND   keyBINDS ").values.flatten().single().id)
        assertEquals("utility.command_keybinds",GuiCatalog.search("dungeon class").values.flatten().single().id)
        assertTrue(GuiCatalog.search("not-a-feature").isEmpty())
    }
    @Test fun `search matches injected translated names without changing grouping or order`() {
        val translatedName:(FeatureCardInfo)->String = { card -> when(card.id) {
            "utility.command_keybinds" -> "Клавиши команд"
            "utility.command_shortcuts" -> "Быстрые команды"
            else -> card.name
        } }

        val groups=GuiCatalog.search("КОМАНД",favorites=listOf("utility.command_shortcuts"),recent=listOf("utility.command_keybinds","utility.command_shortcuts"),translatedName=translatedName)

        assertEquals(listOf("Favorites","Recently changed","General"),groups.keys.toList())
        assertEquals(listOf("utility.command_shortcuts"),groups.getValue("Favorites").map { it.id })
        assertEquals(listOf("utility.command_keybinds","utility.command_shortcuts"),groups.getValue("Recently changed").map { it.id })
        assertEquals(listOf("utility.command_keybinds","utility.command_shortcuts"),groups.getValue("General").map { it.id })
        assertEquals("utility.command_keybinds",GuiCatalog.search("dungeon class",translatedName=translatedName).values.flatten().single().id)
    }
    @Test fun `cards are sorted within categories`() {
        GuiCatalog.search("").values.forEach { assertEquals(it.sortedWith(compareBy<FeatureCardInfo> { c -> c.name.lowercase() }.thenBy { c -> c.id }),it) }
    }
}
