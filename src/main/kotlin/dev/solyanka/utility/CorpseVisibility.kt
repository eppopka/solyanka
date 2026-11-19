/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Shared radius decision: proximity hides the label, box AND tracer, not only the line. */
object CorpseVisibility {
    fun visible(clicked:Boolean,distanceSquared:Double,radius:Double):Boolean = !clicked &&
        distanceSquared.isFinite() && radius.isFinite() && (radius<=0.0 || distanceSquared>radius*radius)
}
