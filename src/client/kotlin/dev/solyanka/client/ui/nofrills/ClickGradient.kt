/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

import net.minecraft.network.chat.Component
import dev.solyanka.utility.ClickGradientMath

/** Smooth per-codepoint color wave. Uses monotonic time; never changes text metrics. */
object ClickGradient {
 fun text(value:String,seconds:Double):Component {
  val result=Component.empty();var index=0
  value.codePoints().forEach {cp->
   val rgb=ClickGradientMath.color(index,seconds)
   result.append(Component.literal(String(Character.toChars(cp))).withColor(rgb));index++
  }
  return result
 }
}
