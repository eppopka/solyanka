/* SPDX-License-Identifier: GPL-3.0-only; Stray rounded-panel visual adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.utility.VisualChrome
import net.minecraft.client.gui.GuiGraphicsExtractor
object ChromeDraw {
 fun panel(g:GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,fill:Int,border:Int,radius:Int=5) {
  if(w<=0 || h<=0) return
  val r=radius.coerceIn(0,minOf(w,h)/2)
  for(row in 0 until h) {
   val inset=VisualChrome.inset(row,h,r);val left=x+inset;val right=x+w-inset
   if(right<=left) continue
   if(row==0 || row==h-1) g.fill(left,y+row,right,y+row+1,border)
   else {g.fill(left,y+row,right,y+row+1,fill);g.fill(left,y+row,left+1,y+row+1,border);g.fill(right-1,y+row,right,y+row+1,border)}
  }
 }
}
