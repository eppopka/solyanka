/* SPDX-License-Identifier: GPL-3.0-only; Stray rounded-panel visual adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.utility.PanelGeometry
import dev.solyanka.utility.PanelBand
import net.minecraft.client.gui.GuiGraphicsExtractor
object ChromeDraw {
 private data class Key(val width:Int,val height:Int,val radius:Int)
 // Size-only cache: moving panels and changing colors never grow it. GUI/render thread only.
 private val geometry=object:LinkedHashMap<Key,List<PanelBand>>(128,.75f,true) {
  override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Key,List<PanelBand>>?)=size>128
 }
 fun panel(g:GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,fill:Int,border:Int,radius:Int=5) {
  if(w<=0 || h<=0) return
  val key=Key(w,h,radius.coerceIn(0,minOf(w,h)/2))
  val bands=geometry.getOrPut(key) {PanelGeometry.bands(w,h,key.radius)}
  for(band in bands) {
   val left=x+band.inset;val right=x+w-band.inset
   if(right<=left) continue
   val top=y+band.top;val bottom=y+band.bottom
   if(band.borderOnly) g.fill(left,top,right,bottom,border)
   else {g.fill(left,top,right,bottom,fill);g.fill(left,top,left+1,bottom,border);g.fill(right-1,top,right,bottom,border)}
  }
 }
}
