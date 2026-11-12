/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HudFramePlacementTest {
    @Test fun `positions clamp to both screen edges at scaled sizes`() {
        assertEquals(0,HudFramePlacement.coordinate(-10,200,80,1f))
        assertEquals(120,HudFramePlacement.coordinate(150,200,80,1f))
        assertEquals(80,HudFramePlacement.coordinate(150,200,80,1.5f))
        assertEquals(0,HudFramePlacement.coordinate(50,100,80,2f))
    }
    @Test fun `editor coordinates are not clamped twice`() {
        assertEquals(-8,HudFramePlacement.coordinate(10,200,80,1f,-8))
        assertEquals(195,HudFramePlacement.coordinate(10,200,80,1f,195))
    }
}
