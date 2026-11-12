/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

/** One monotonic timestamp shared by components rendered in the same GUI frame. */
object GuiFrameClock {
 var now:Long=System.nanoTime();private set
 fun beginFrame() {now=System.nanoTime()}
}
