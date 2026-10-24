/* SPDX-License-Identifier: GPL-3.0-only; user-provided SimonSays sequence adaptation. */
package dev.solyanka.utility

data class SimonSettings(val skip:Boolean=true,val blockWrong:Boolean=false,val first:Int=0xff00ff00.toInt(),val second:Int=0xffffff00.toInt(),val other:Int=0xffff0000.toInt(),val outline:Boolean=false,val throughWalls:Boolean=true,val numbers:Boolean=false,val numberColor:Int=-1,val alerts:Boolean=true,val partyAlerts:Boolean=false,val sound:Boolean=true,val progress:Boolean=true,val hud:HudSpec=HudSpec(x=12,y=280)) {fun validate()=hud.validate()}
class SimonLogic {
 data class Step(val cell:Int,val number:Int)
 val pending=mutableListOf<Step>()
 var stage=0;private set
 private var skipOver=false
 private var sequence=0
 private var lastClick=-1L
 fun reset() {pending.clear();stage=0;skipOver=false;sequence=0;lastClick=-1}
 fun flash(cell:Int,skip:Boolean) {
  require(cell in 0..15)
  if(pending.size>=32)return
  if(skip && pending.size==2 && !skipOver) {pending.removeAt(0);pending.indices.forEach {i->pending[i]=pending[i].copy(number=i+1)}}
  pending.add(Step(cell,pending.size+1));sequence=pending.size.coerceAtMost(5)
 }
 fun check(button:Boolean) {if(button){stage=maxOf(stage,sequence);skipOver=true}else sequence=0}
 /** Returns cancellation, not a claim that the server accepted a manual click. */
 fun click(cell:Int,tick:Long,blockWrong:Boolean,sneaking:Boolean):Boolean {
  if(pending.isEmpty())return false
  if(lastClick==tick)return true
  if(cell!=pending.first().cell) {
   if(blockWrong && !sneaking)return true
   if(pending.size==3 && cell==pending[1].cell){pending.removeAt(1);pending.removeAt(0)}
  } else pending.removeAt(0)
  lastClick=tick;return false
 }
}
