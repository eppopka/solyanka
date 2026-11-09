/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import net.minecraft.client.gui.GuiGraphicsExtractor

/** A small layered halo, drawn behind the HUD. No shaders or offscreen buffers. */
object HudGlow {
 fun draw(g:GuiGraphicsExtractor,id:String,x:Int,y:Int,w:Int,h:Int) {
  if(w<=0 || h<=0 || !SolyankaClient.currentGeneral().features.hudGlow.enabled(id)) return
  // Four translucent rings fade outward; their center stays clear for the HUD itself.
  for(distance in 4 downTo 1) {
   val color=(when(distance) {4->5;3->9;2->16;else->25} shl 24) or 0x7dd3fc
   val left=x-distance;val right=x+w+distance;val top=y-distance;val bottom=y+h+distance
   g.fill(left,top,right,top+1,color);g.fill(left,bottom-1,right,bottom,color)
   g.fill(left,top+1,left+1,bottom-1,color);g.fill(right-1,top+1,right,bottom-1,color)
  }
 }
}
