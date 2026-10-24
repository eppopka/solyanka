/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Coalesce equal rounded scanlines. Color/alpha layering stays identical to the old renderer. */
data class PanelBand(val top:Int,val bottom:Int,val inset:Int,val borderOnly:Boolean)
object PanelGeometry {
 fun bands(width:Int,height:Int,radius:Int):List<PanelBand> {
  if(width<=0 || height<=0) return emptyList()
  val r=radius.coerceIn(0,minOf(width,height)/2)
  val out=ArrayList<PanelBand>()
  var row=0
  while(row<height) {
   val inset=VisualChrome.inset(row,height,r)
   val borderOnly=row==0 || row==height-1
   var end=if(row>=r && row<height-r && !borderOnly) maxOf(row+1,height-r-1) else row+1
   while(end<height && (end==0 || end==height-1)==borderOnly && VisualChrome.inset(end,height,r)==inset) end++
   out.add(PanelBand(row,end,inset,borderOnly));row=end
  }
  return out
 }
 fun fillCalls(bands:List<PanelBand>)=bands.sumOf {if(it.borderOnly) 1 else 3}
}
