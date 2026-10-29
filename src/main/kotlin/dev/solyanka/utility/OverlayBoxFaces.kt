/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
/** Six independent quads; never connect adjacent boxes as one strip. */
object OverlayBoxFaces {
 val indices:List<List<Int>> = listOf(listOf(0,1,2,3),listOf(4,5,6,7),listOf(0,1,5,4),listOf(3,2,6,7),listOf(0,3,7,4),listOf(1,2,6,5))
}
