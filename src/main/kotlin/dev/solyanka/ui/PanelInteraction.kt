/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

/** GUI-only state: moving a window never writes feature configuration. */
class PanelDrag {
 data class Point(val x:Int,val y:Int)
 private var viewportWidth=1;private var viewportHeight=1;private var frameWidth=1;private var frameHeight=1
 var position:Point?=null;private set
 var dragging=false;private set
 private var offsetX=0.0;private var offsetY=0.0
 fun layout(width:Int,height:Int,w:Int,h:Int) {
  viewportWidth=width.coerceAtLeast(1);viewportHeight=height.coerceAtLeast(1);frameWidth=w.coerceAtLeast(1);frameHeight=h.coerceAtLeast(1)
  val old=position ?: Point((viewportWidth-frameWidth)/2,(viewportHeight-frameHeight)/2)
  position=clamp(old);dragging=false
 }
 fun place(point:Point) {position=clamp(point)}
 private fun clamp(p:Point)=Point(p.x.coerceIn(0,(viewportWidth-frameWidth).coerceAtLeast(0)),p.y.coerceIn(0,(viewportHeight-frameHeight).coerceAtLeast(0)))
 fun begin(x:Double,y:Double,button:Int,header:Boolean):Boolean {
  if(button!=0 || !header || !x.isFinite() || !y.isFinite())return false
  val p=position ?: return false;dragging=true;offsetX=x-p.x;offsetY=y-p.y;return true
 }
 fun move(x:Double,y:Double):Point? {
  if(!dragging || !x.isFinite() || !y.isFinite())return null
  return clamp(Point((x-offsetX).toInt(),(y-offsetY).toInt())).also {position=it}
 }
 fun end():Boolean {val old=dragging;dragging=false;return old}
}
/** Last navigation request wins; the host commits only between input dispatches. */
class PanelNavigation<T> {
 data class Request<T>(val panel:T?)
 private var request:Request<T>?=null
 fun open(panel:T){request=Request(panel)}
 fun close(){request=Request(null)}
 fun pending()=request!=null
 fun take():Request<T>?=request.also {request=null}
 fun clear(){request=null}
}
