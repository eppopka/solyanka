/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.Size
import io.wispforest.owo.ui.core.Sizing

/** Retains logical pixels, not a percentage that jumps when a setting adds rows.
 * Uses the protected extension points in owo 0.13.1 / MC 26.2, without reflection. */
class RetainedScroll(direction:ScrollContainer.ScrollDirection,horizontal:Sizing,vertical:Sizing,child:FlowLayout):
    ScrollContainer<FlowLayout>(direction,horizontal,vertical,child) {
    private var retainedOffset:Double?=null
    fun offset():Double=currentScrollPosition
    fun restoreOffset(pixels:Double) {
        if(!pixels.isFinite())return
        val value=pixels.coerceAtLeast(0.0)
        retainedOffset=value
        super.scrollBy(value-scrollOffset,true,false)
    }
    override fun layout(space:Size) {
        super.layout(space)
        // Also restore after deferred owo reflow: maxScroll may still have been zero at rebuild time.
        retainedOffset?.let {super.scrollBy(it-scrollOffset,true,false)}
    }
    override fun scrollBy(offset:Double,instant:Boolean,showScrollbar:Boolean) {
        // Actual wheel/keyboard/scrollbar input takes over immediately from the saved view.
        retainedOffset=null
        super.scrollBy(offset,instant,showScrollbar)
    }
}
