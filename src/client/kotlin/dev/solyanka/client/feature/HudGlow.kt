/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.HudGlowProfile
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Rounded, feathered halo behind the HUD; no shader, offscreen buffer or input changes. */
object HudGlow {
 fun draw(g:GuiGraphicsExtractor,id:String,x:Int,y:Int,w:Int,h:Int) {
  val settings=SolyankaClient.currentGeneral().features.hudGlow
  if(w<=0 || h<=0 || !settings.enabled(id)) return
  val intensity=settings.intensity.value(id)
  // Draw outer faint rings first and the strongest ring closest to the element.
  for(distance in HudGlowProfile.RADIUS downTo 1) {
   val color=(HudGlowProfile.alpha(distance,intensity) shl 24) or 0x7dd3fc
   ChromeDraw.outline(g,x-distance,y-distance,w+distance*2,h+distance*2,color,6+distance)
  }
 }
}
