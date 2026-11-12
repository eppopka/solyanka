/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

import net.minecraft.network.chat.Component
import dev.solyanka.gfx.ClickGradientMath

/** Smooth per-codepoint color wave. Uses monotonic time; never changes text metrics. */
object ClickGradient {
 private var cachedValue="";private var cachedTick=Long.MIN_VALUE;private var cached:Component=Component.empty()
 fun text(value:String,seconds:Double):Component {
  val tick=(seconds*1000.0/30.0).toLong()
  if(value==cachedValue && tick==cachedTick) return cached
  val result=Component.empty();var index=0
  value.codePoints().forEach {cp->
   val rgb=ClickGradientMath.color(index,tick*0.03)
   result.append(Component.literal(String(Character.toChars(cp))).withColor(rgb));index++
  }
  cachedValue=value;cachedTick=tick;cached=result
  return result
 }
}
