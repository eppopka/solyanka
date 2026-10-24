/* SPDX-License-Identifier: GPL-3.0-only
 * Odin room transforms/core/Blaze port (c) 2025 odtheking, BSD-3-Clause;
 * full notice in licenses/Odin-BSD-3-Clause.txt. */
package dev.solyanka.utility

data class GridPos(val x:Int,val y:Int,val z:Int)
enum class RoomFacing(val dx:Int,val dz:Int) { NORTH(15,15),SOUTH(-15,-15),WEST(15,-15),EAST(-15,15) }
object DungeonLogic {
 fun real(p:GridPos,origin:GridPos,facing:RoomFacing):GridPos {
  val (x,z)=when(facing) { RoomFacing.NORTH->-p.x to -p.z;RoomFacing.SOUTH->p.x to p.z;RoomFacing.WEST->-p.z to p.x;RoomFacing.EAST->p.z to -p.x }
  return GridPos(origin.x+x,p.y,origin.z+z)
 }
 fun relative(p:GridPos,origin:GridPos,facing:RoomFacing):GridPos {
  val x=p.x-origin.x;val z=p.z-origin.z
  return when(facing) { RoomFacing.NORTH->GridPos(-x,p.y,-z);RoomFacing.SOUTH->GridPos(x,p.y,z);RoomFacing.WEST->GridPos(z,p.y,-x);RoomFacing.EAST->GridPos(-z,p.y,x) }
 }
 fun tile(x:Int,z:Int):Pair<Int,Int>? { val tx=(x+201) shr 5;val tz=(z+201) shr 5;return if(tx in 0..5 && tz in 0..5) tx to tz else null }
 // Keep the source column hashing, including ignored chest/plank blocks and bedrock cutoff.
 fun core(column:List<Pair<Int,String>>):Pair<Int,Int> {
  val text=StringBuilder(1024);var highest=0;var found=false;var bedrock=0
  for((y,name) in column) {
   val air=name=="minecraft:air" || name=="minecraft:cave_air" || name=="minecraft:void_air"
   if(!found) { if(!air && name!="minecraft:gold_block") { found=true;highest=y } else text.append('0') }
   if(found) {
    if(air && bedrock>=2 && y<69) { repeat((y-11).coerceAtLeast(0)) {text.append('0')};break }
    if(name=="minecraft:bedrock") bedrock++ else { bedrock=0;if(name in setOf("minecraft:oak_planks","minecraft:trapped_chest","minecraft:chest")) continue }
    text.append("Block{$name}")
   }
  }
  return text.toString().hashCode() to highest
 }
 private val blaze=Regex("^\\[Lv+\\d+] (?:\uE07C )?Blaze [\\d,]+/([\\d,]+)❤$")
 fun blazeHealth(text:String)=blaze.matchEntire(ChatLogic.plain(text))?.groupValues?.get(1)?.replace(",","")?.toIntOrNull()?.takeIf { it>0 }
 fun mimicDeath(event:Int,baby:Boolean,floor:Int?,clear:Boolean,already:Boolean)=event==3 && baby && floor in setOf(6,7) && clear && !already
}
data class PuzzleSettings(val optimizeIce:Boolean=true,val blazeLines:Boolean=true,val beamLines:Boolean=true,val depthTest:Boolean=true,val iceDepthTest:Boolean=true,val blazeDepthTest:Boolean=true,val beamDepthTest:Boolean=true,val announceMimic:Boolean=false,val announcePrince:Boolean=false,val announceBat:Boolean=false,val mimicChestColor:Int=0x64ff0000)
