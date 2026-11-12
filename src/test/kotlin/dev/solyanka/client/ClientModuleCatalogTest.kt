/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import dev.solyanka.core.ModuleIds
import dev.solyanka.core.ModuleRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClientModuleCatalogTest {
    @Test
    fun `registers modules in the existing order`() {
        val modules = ModuleRegistry { _, _ -> }

        ClientModuleCatalog.register(modules)

        assertEquals(
            listOf(
                ModuleIds.KEYBINDS,
                ModuleIds.SHORTCUTS,
                ModuleIds.CHAT,
                ModuleIds.TOOLTIPS,
                ModuleIds.TICK_TIMERS,
                ModuleIds.PLAYER_DISPLAY,
                ModuleIds.PERFORMANCE,
                ModuleIds.INVENTORY_HUD,
                ModuleIds.ITEM_NAME_HUD,
                ModuleIds.SKILL_HUD,
                ModuleIds.ANIMATION,
                ModuleIds.MIDDLE_FIX,
                ModuleIds.MIDDLE_OVERRIDE,
                ModuleIds.CURSOR,
                ModuleIds.LOADING,
                ModuleIds.PRICES,
                ModuleIds.SPREAD_GUESSER,
                ModuleIds.EXPERIMENTS,
                ModuleIds.SUPPRESSION,
                ModuleIds.WAYPOINTS,
                ModuleIds.LEAP_MESSAGE,
                ModuleIds.CHAT_COMMANDS,
                ModuleIds.MUSIC,
                ModuleIds.DOORS,
                ModuleIds.MAP_INFO,
                ModuleIds.ICE,
                ModuleIds.BLAZE,
                ModuleIds.BEAMS,
                ModuleIds.MIMIC,
                ModuleIds.MIMIC_CHEST,
                ModuleIds.AMBIENCE,
                ModuleIds.ASPECT,
                ModuleIds.FOG,
                ModuleIds.PLAYER_GLOW,
                ModuleIds.NAMETAGS,
                ModuleIds.BLOCK_OUTLINE,
                ModuleIds.TITANIUM,
                ModuleIds.SCOREBOARD,
                ModuleIds.THEMED_CHAT,
                ModuleIds.ITEM_AURA,
                ModuleIds.CS2_KNIFE,
                ModuleIds.HELD_ITEM,
                ModuleIds.SKYBOX,
                ModuleIds.MOTION_BLUR,
                ModuleIds.STOPWATCH,
                ModuleIds.DVD,
                ModuleIds.AMBIENT_PARTICLES,
                ModuleIds.ROTATING_ITEM,
                ModuleIds.MOVE_HEARTS,
                ModuleIds.SECRET_COUNTER,
                ModuleIds.BESTIARY,
                ModuleIds.INVISIBUG,
                ModuleIds.TIKI,
                ModuleIds.CHINA_HAT,
                ModuleIds.HALO,
                ModuleIds.PUZZLE_SOLVERS,
                ModuleIds.SIMON,
                ModuleIds.PET,
                ModuleIds.PIP,
                ModuleIds.MOVE_HOTBAR,
                ModuleIds.BLOCKLIST,
            ),
            modules.all().map { it.info.id },
        )
    }
}
