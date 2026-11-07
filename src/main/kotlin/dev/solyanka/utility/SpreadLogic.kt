/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

object SpreadLogic {
    private val stat = Regex("(?i)\\bmining\\s+spread\\s*[:：]?\\s*[+]?([0-9][0-9,]*(?:\\.[0-9]+)?)\\b")
    fun readTab(lines:Iterable<String>):Double? {
        val values=lines.mapNotNull { line ->
            val match=stat.find(ChatLogic.plain(line)) ?: return@mapNotNull null
            match.groupValues[1].replace(",","").toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10000.0 }
        }.distinct()
        return values.singleOrNull()
    }
}
