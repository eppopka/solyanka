/* SPDX-License-Identifier: GPL-3.0-only; adaptations of user-provided impl.zip. */
package dev.solyanka.utility

import kotlin.math.*

data class BestiarySettings(val names:String="",val exact:Boolean=false,val color:Int=0xff00ffff.toInt(),val tracer:Boolean=true,val range:Int=0) {
 fun validate() {require(names.length<=2048 && names.none {it=='\n' || it=='\r'});require(range in 0..100)}
 fun matches(name:String):Boolean {val plain=ChatLogic.plain(name);return names.split(',').map(String::trim).filter(String::isNotEmpty).any {if(exact) plain.equals(it,true) else plain.contains(it,true)}}
}
data class InvisibugSettings(val color:Int=0x82ff0000.toInt(),val tracer:Boolean=true,val distance:Boolean=true)
data class TikiSettings(val style:Int=2,val tracer:Boolean=false,val distance:Boolean=true,val sneaky:Int=0x6e55ff55,val cheeky:Int=0x6effc82d,val shrieky:Int=0x6eff55ff,val particles:Boolean=false,val particleStyle:Int=1,val particleColor:Int=0xd255ffff.toInt(),val count:Int=18) {
 fun validate() {require(style in 0..2 && particleStyle in 0..2 && count in 4..48)}
 fun color(type:Int)=when(type) {0->sneaky;1->cheeky;else->shrieky}
}
data class ChinaHatSettings(val hideFirstPerson:Boolean=true,val color:Int=0x5affffff,val rainbow:Boolean=false,val radius:Double=.45,val height:Double=.4,val fill:Boolean=true,val width:Double=2.0,val throughWalls:Boolean=false) {
 fun validate() {require(radius.isFinite() && radius in .2..1.0 && height.isFinite() && height in .1..1.0 && width.isFinite() && width in 1.0..6.0)}
}
data class HaloSettings(val hideFirstPerson:Boolean=true,val color:Int=0xffffd25a.toInt(),val rainbow:Boolean=false,val radius:Double=.35,val height:Double=.30,val thickness:Double=.03,val glow:Double=.6,val opacity:Double=1.0,val shimmer:Boolean=true,val bob:Boolean=true,val fill:Boolean=false,val fillOpacity:Double=.3,val throughWalls:Boolean=false) {
 fun validate() {require(radius.isFinite() && radius in .1..1.0 && height.isFinite() && height in 0.0..1.5 && thickness.isFinite() && thickness in .01.. .1);require(listOf(glow,fillOpacity).all {it.isFinite() && it in 0.0..1.0});require(opacity.isFinite() && opacity in .05..1.0)}
}
/** Bounded packet evidence; a single particle is never treated as a confirmed mob. */
class BugEvidence {
 data class Track(val points:MutableList<Point3>,var ttl:Int=20)
 private val tracks=mutableListOf<Track>()
 fun clear()=tracks.clear()
 fun observe(p:Point3) {
  if(!listOf(p.x,p.y,p.z).all(Double::isFinite))return
  val t=tracks.firstOrNull {distanceSquared(it.points.last(),p)<=.09}
  if(t!=null) {if(t.points.size==4)t.points.removeAt(0);t.points.add(p);t.ttl=20}
  else if(tracks.size<128)tracks.add(Track(mutableListOf(p)))
 }
 fun tick() {tracks.forEach {it.ttl--};tracks.removeIf {it.ttl<=0}}
 fun confirmed()=tracks.filter {it.points.size==4}.map {it.points.last()}
 companion object {fun distanceSquared(a:Point3,b:Point3)=(a.x-b.x).pow(2)+(a.y-b.y).pow(2)+(a.z-b.z).pow(2)}
}
object CosmeticMesh {
 data class Face(val a:Point3,val b:Point3,val c:Point3,val color:Int)
 data class Edge(val a:Point3,val b:Point3)
 fun alpha(c:Int,a:Double)=(c and 0xffffff) or ((a.coerceIn(0.0,1.0)*255).toInt() shl 24)
 fun cone(s:ChinaHatSettings):Pair<List<Face>,List<Edge>> {
  val apex=Point3(0.0,s.height,0.0);fun p(i:Int)=Point3(s.radius*cos(i*2*PI/32),0.0,s.radius*sin(i*2*PI/32))
  return (if(s.fill)(0..31).map {Face(apex,p(it),p(it+1),s.color)} else emptyList()) to buildList {repeat(32){i->add(Edge(p(i),p(i+1)));if(i%4==0)add(Edge(apex,p(i)))}}
 }
 fun halo(s:HaloSettings,time:Double):List<Face> = buildList {
  val core=s.thickness/2;val spread=core*(1+s.glow*4)
  repeat(3) {shell->
   val tube=when(shell){0->core;1->core+spread*.5;else->core+spread};val fade=listOf(1.0,.35,.12)[shell]
   val color=if(shell==0) (s.color and -0x1000000) or (((((s.color ushr 16) and 255)+255)/2) shl 16) or (((((s.color ushr 8) and 255)+255)/2) shl 8) or (((s.color and 255)+255)/2) else s.color
   repeat(48){i->repeat(8){k->
    val a=i*2*PI/48;val b=(i+1)*2*PI/48;val u=k*2*PI/8;val v=(k+1)*2*PI/8
    fun p(t:Double,f:Double)=Point3((s.radius+tube*cos(f))*cos(t),tube*sin(f),(s.radius+tube*cos(f))*sin(t))
    val c=alpha(color,s.opacity*fade*(if(s.shimmer).65+.35*sin(a*3+time*2) else 1.0))
    add(Face(p(a,u),p(b,u),p(b,v),c));add(Face(p(a,u),p(b,v),p(a,v),c))
   }}
  }
  if(s.fill)repeat(48){i->val a=i*2*PI/48;val b=(i+1)*2*PI/48;add(Face(Point3(0.0,0.0,0.0),Point3(s.radius*cos(a),0.0,s.radius*sin(a)),Point3(s.radius*cos(b),0.0,s.radius*sin(b)),alpha(s.color,s.fillOpacity*s.opacity)))}
 }
}
