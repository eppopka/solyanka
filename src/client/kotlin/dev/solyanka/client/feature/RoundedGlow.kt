/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.utility.GlowBand
import dev.solyanka.utility.RoundedGlowGeometry
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

/** Alpha-coverage feather around a rounded rectangle; no scissor or opaque inner rectangle. */
object RoundedGlow {
 private data class Key(val w:Int,val h:Int,val radius:Int,val spread:Int)
 private val cache=object:LinkedHashMap<Key,List<GlowBand>>(32,.75f,true) {
  override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Key,List<GlowBand>>?)=size>32
 }
 fun draw(g:GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,radius:Int,color:Int,strength:Double,spread:Int=8) {
  if(w<=0 || h<=0 || w>4096 || h>4096 || strength<=0 || !strength.isFinite()) return
  val key=Key(w,h,radius.coerceIn(0,minOf(w,h)/2),spread.coerceIn(1,12))
  val bands=cache.getOrPut(key) {RoundedGlowGeometry.bands(key.w,key.h,key.radius,key.spread)}
  val alpha=(color ushr 24)/255.0
  for(b in bands) {
   val a=(b.level/32.0*255*strength*alpha).roundToInt().coerceIn(0,255)
   if(a>0) g.fill(x+b.x,y+b.y,x+b.x+b.width,y+b.y+b.height,(a shl 24) or (color and 0xffffff))
  }
 }
}
