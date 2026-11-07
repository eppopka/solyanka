/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import org.junit.jupiter.api.Test
import kotlin.test.*

class SpreadLogicTest {
    @Test fun `reads the labelled stat without using gemstone spread`() {
        assertEquals(1250.5,SpreadLogic.readTab(listOf("§6▚ Mining Spread: §f1,250.5", "Gemstone Spread: 20")))
        assertEquals(0.0,SpreadLogic.readTab(listOf("Mining Spread 0")))
        assertNull(SpreadLogic.readTab(listOf("Gemstone Spread: 250")))
        assertNull(SpreadLogic.readTab(listOf("Mining Spread: 200", "Mining Spread: 300")))
        assertNull(SpreadLogic.readTab(listOf("Mining Spread: 10001")))
        assertNull(SpreadLogic.readTab(emptyList()))
    }
}
