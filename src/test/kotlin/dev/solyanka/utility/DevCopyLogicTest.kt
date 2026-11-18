/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class DevCopyLogicTest {
    @Test fun `named and exact vanilla RGB colors use standard section codes`() {
        assertEquals("§6",DevCopyLogic.colorCodes("gold"))
        assertEquals("§6",DevCopyLogic.colorCodes("#FFAA00"))
        assertEquals("§d",DevCopyLogic.colorCodes("light_purple"))
        assertEquals("&a",DevCopyLogic.colorCodes("green",'&'))
        assertEquals("",DevCopyLogic.colorCodes(null))
        assertEquals("",DevCopyLogic.colorCodes("invalid"))
    }
    @Test fun `custom RGB has explicit conventional hex notation`() {
        assertEquals("§x§1§2§3§4§5§6",DevCopyLogic.colorCodes("#123456"))
        assertEquals("&x&1&2&3&4&5&6",DevCopyLogic.colorCodes("#123456",'&'))
    }
    @Test fun `all legacy style flags and paragraphs are preserved without bleed`() {
        val text=DevCopyLogic.legacy(listOf(
            DevTextRun("First\n\nparagraph",color="gold",bold=true,italic=true,underlined=true,strikethrough=true,obfuscated=true),
            DevTextRun(" plain")
        ))
        assertEquals("§r§6§k§l§m§n§oFirst\n\nparagraph§r plain§r",text)
        assertEquals("&r&6&lText&r",DevCopyLogic.legacy(listOf(DevTextRun("Text","gold",bold=true)),'&'))
    }
    @Test fun `unicode and literal ampersands are not rewritten`() {
        val value="Corpse 💀 & literal &6"
        assertEquals("§r$value§r",DevCopyLogic.legacy(listOf(DevTextRun(value))))
        assertEquals("",DevCopyLogic.legacy(emptyList()))
        assertEquals("",DevCopyLogic.legacy(listOf(DevTextRun(""))))
    }
    @Test fun `default radius is three and oversized scans are rejected`() {
        assertEquals(3,DevCopyLogic.DEFAULT_RADIUS)
        DevCopyLogic.validateRadius(3);DevCopyLogic.validateRadius(16)
        assertFailsWith<IllegalArgumentException> {DevCopyLogic.validateRadius(0)}
        assertFailsWith<IllegalArgumentException> {DevCopyLogic.validateRadius(17)}
    }
    @Test fun `long diagnostic fields are visibly bounded`() {
        assertEquals("abc",DevCopyLogic.boundedText("abc",3))
        assertEquals("abc…[truncated]",DevCopyLogic.boundedText("abcdef",3))
        assertEquals(4*1024*1024,DevCopyLogic.MAX_CLIPBOARD_BYTES)
    }
}
