/* SPDX-License-Identifier: GPL-3.0-only; adapted from user-provided impl.zip. */
package dev.solyanka.utility

import kotlin.math.sin
import kotlin.random.Random

/** Settings remain inert until their independent master modules are enabled. */
data class AmbientParticleSettings(val particle:String="end_rod",val amount:Int=2,val radius:Int=12) {
    fun validate() {require(particle in AmbientParticleLogic.types);require(amount in 1..10);require(radius in 4..32)}
}
data class ParticleSpawn(val x:Double,val y:Double,val z:Double,val vx:Double,val vy:Double,val vz:Double)
object AmbientParticleLogic {
    val types=listOf("end_rod","firefly","cherry","snowflake","soul","portal","witch","happy_villager")
    val labels=listOf("End Rod","Fireflies","Cherry Petals","Snowflakes","Souls","Portal","Witch","Happy Villager")
    /** Exact source box distribution: radius in X/Z, Y -1.5..6.5; no server packets. */
    fun sample(s:AmbientParticleSettings,random:Random=Random.Default):ParticleSpawn = ParticleSpawn(
        (random.nextDouble()-.5)*2*s.radius,random.nextDouble()*8-1.5,(random.nextDouble()-.5)*2*s.radius,
        (random.nextDouble()-.5)*.02,-.005-random.nextDouble()*.01,(random.nextDouble()-.5)*.02)
}
data class RotatingItemSettings(val mode:String="chaotic",val speed:Int=50,val amplitude:Int=40,
    val offsetX:Double=0.0,val offsetY:Double=0.0,val offsetZ:Double=0.0) {
    fun validate() {require(mode in RotatingItemLogic.modes);require(speed in 5..200);require(amplitude in 5..100)
        require(listOf(offsetX,offsetY,offsetZ).all {it.isFinite() && it in -.5.. .5})}
}
data class ItemMotion(val x:Double,val y:Double,val z:Double,val rx:Double=0.0,val ry:Double=0.0,val rz:Double=0.0)
object RotatingItemLogic {
    val modes=listOf("chaotic","clockwise","counterclockwise","spin","wave")
    val labels=listOf("Chaotic","Clockwise","Counterclockwise","Spin","Wave")
    /** Monotonic per-enable epoch avoids tying animation phase to wall-clock time. */
    fun motion(elapsedNanos:Long,s:RotatingItemSettings,left:Boolean):ItemMotion {
        val t=elapsedNanos.coerceAtLeast(0)/1_000_000_000.0*(s.speed/50.0)
        val amp=s.amplitude/100.0;val degrees=(t*90)%360
        val base=ItemMotion(s.offsetX*if(left) -1 else 1,s.offsetY,s.offsetZ)
        return when(s.mode) {
            "chaotic"->base.copy(rx=sin(t*2.1)*35*amp,ry=sin(t*1.6+1.5)*45*amp,rz=sin(t*2.6+3)*35*amp)
            "clockwise"->base.copy(rz=-degrees)
            "counterclockwise"->base.copy(rz=degrees)
            "spin"->base.copy(ry=degrees*2)
            "wave"->base.copy(y=base.y+sin(t*2)*.05*amp,rz=sin(t*2+.8)*15*amp)
            else->error("Unknown rotation mode")
        }
    }
}
data class MoveHeartsSettings(val customPosition:Boolean=false,val moveArmor:Boolean=false,val hud:HudSpec=HudSpec(x=0,y=0)) {
    fun validate()=hud.validate()
}
data class HeartTransform(val x:Double,val y:Double,val scale:Double,val translateX:Double,val translateY:Double)
object HeartLayout {
    const val WIDTH=81;const val HEIGHT=9
    /** Same baseline as the source wrapper. Multirow health and armor keep native relative offsets. */
    fun transform(s:MoveHeartsSettings,width:Int,height:Int):HeartTransform {
        val baseX=width/2.0-91;val baseY=height-39.0;val scale=s.hud.scale
        val x=(if(s.customPosition) s.hud.x.toDouble() else baseX).coerceIn(0.0,(width-WIDTH*scale).coerceAtLeast(0.0))
        val y=(if(s.customPosition) s.hud.y.toDouble() else baseY).coerceIn(0.0,(height-HEIGHT*scale).coerceAtLeast(0.0))
        return HeartTransform(x,y,scale,x-scale*baseX,y-scale*baseY)
    }
    fun editorHud(s:MoveHeartsSettings,width:Int,height:Int):HudSpec {
        val t=transform(s,width,height);return s.hud.copy(x=t.x.toInt(),y=t.y.toInt())
    }
}
