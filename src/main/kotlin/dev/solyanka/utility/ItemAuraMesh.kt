/* SPDX-License-Identifier: GPL-3.0-only; ItemAura math/gradient sprites from user impl.zip. */
package dev.solyanka.utility
import java.awt.Color
import kotlin.math.*
/** No Minecraft/GPU state. Whole triangles only; 300k-vertex hard ceiling per hand. */
class ItemAuraMesh(private val cfg:ItemAuraSettings) {
 private val color=Color(cfg.auraColor,true)
 private class TriBuffer {
  private var data=FloatArray(7*4096);private var used=0
  fun vertex(x:Float,y:Float,z:Float,r:Float,g:Float,b:Float,a:Float) {
   if(used>=MAX_FLOATS) return
   if(used+7>data.size) data=data.copyOf(minOf(MAX_FLOATS,data.size*2))
   data[used++]=x;data[used++]=y;data[used++]=z;data[used++]=r;data[used++]=g;data[used++]=b;data[used++]=a
  }
  fun finish()=data.copyOf(used)
 }
 companion object {const val MAX_FLOATS=300000*7}
    private fun hash(i: Int, salt: Int): Double {
        val x = sin(i * 127.1 + salt * 311.7) * 43758.5453
        return x - floor(x)
    }

    private fun colorFor(index: Int, fade: Double, t: Double): Color {
        val alpha = (color.alpha * fade).toInt().coerceIn(0, 255)
        if (! cfg.rainbow) return Color(color.red, color.green, color.blue, alpha)

        val hue = ((t * 0.08) + index * cfg.hueSpread / cfg.count.coerceAtLeast(1)).mod(1.0)
        val rgb = Color.HSBtoRGB(hue.toFloat(), 0.75f, 1f)
        return Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, alpha)
    }

    /** Particle offset in item model space at animation time [t]. */
    private fun offsetAt(i: Int, t: Double): Triple<Double, Double, Double> {
        val n = cfg.count.coerceAtLeast(1)
        val r = cfg.radius.toDouble()
        val phase = i * 2.0 * PI / n

        return when (cfg.style) {
            1 -> { // Orbit Rings
                val tilt = (i % 3) * PI / 3.0 + hash(i, 3) * 0.4
                val a = t * 1.6 + phase
                val rr = r * (0.85 + 0.15 * sin(t * 1.1 + i))
                Triple(rr * cos(a), rr * sin(a) * cos(tilt), rr * sin(a) * sin(tilt))
            }

            2 -> { // DNA Helix
                val strand = if (i % 2 == 0) 0.0 else PI
                val u = (t * 0.25 + i.toDouble() / n) % 1.0
                val a = u * 4.0 * PI + strand + t * 0.6
                val rr = r * 0.6 * (0.45 + 0.55 * sin(u * PI))
                Triple(rr * cos(a), (u - 0.5) * r * 2.4, rr * sin(a))
            }

            3 -> { // Comet Swarm
                val a = t * 2.4 + phase
                val rr = r * (0.35 + 0.65 * hash(i, 7))
                val drift = sin(t * 0.8 + i) * r * 0.5
                Triple(rr * cos(a), rr * sin(a * 0.7) * 0.7 + drift, rr * sin(a))
            }

            4 -> { // Vortex
                val u = (t * 0.4 + i.toDouble() / n) % 1.0
                val a = u * 8.0 * PI + t * 1.5
                val rr = r * (1.0 - u)
                Triple(rr * cos(a), (0.5 - u) * r * 1.6, rr * sin(a))
            }

            else -> { // Fireflies
                val s1 = hash(i, 11) * 6.28
                val s2 = hash(i, 23) * 6.28
                Triple(
                    sin(t * 0.6 + s1) * r + sin(t * 1.31 + s2) * r * 0.35,
                    sin(t * 0.45 + s2) * r * 0.7 + cos(t * 1.07 + s1) * r * 0.25,
                    cos(t * 0.53 + s1 * 1.7) * r + cos(t * 1.19 + s2) * r * 0.35
                )
            }
        }
    }

    private fun disc(
        buf: TriBuffer,
        cx: Float, cy: Float, cz: Float, r: Float, c: Color, segments: Int,
        ux: Float, uy: Float, uz: Float, vx: Float, vy: Float, vz: Float
    ) {
        val cr = c.red / 255f
        val cg = c.green / 255f
        val cb = c.blue / 255f
        val ca = c.alpha / 255f
        if (ca <= 0f || r <= 0f) return

        val step = (2.0 * PI / segments).toFloat()
        for (s in 0 until segments) {
            val a0 = step * s
            val a1 = step * (s + 1)
            val c0 = cos(a0.toDouble()).toFloat()
            val s0 = sin(a0.toDouble()).toFloat()
            val c1 = cos(a1.toDouble()).toFloat()
            val s1 = sin(a1.toDouble()).toFloat()

            val x0 = cx + (ux * c0 + vx * s0) * r
            val y0 = cy + (uy * c0 + vy * s0) * r
            val z0 = cz + (uz * c0 + vz * s0) * r
            val x1 = cx + (ux * c1 + vx * s1) * r
            val y1 = cy + (uy * c1 + vy * s1) * r
            val z1 = cz + (uz * c1 + vz * s1) * r

            buf.vertex(cx, cy, cz, cr, cg, cb, ca)
            buf.vertex(x0, y0, z0, cr, cg, cb, 0f)
            buf.vertex(x1, y1, z1, cr, cg, cb, 0f)
        }
    }

    /** Soft volumetric orb: three crossed gradient discs, so it reads round from any angle. */
    private fun orb(buf: TriBuffer, x: Double, y: Double, z: Double, s: Double, c: Color, segments: Int) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fz = z.toFloat()
        val r = s.toFloat()

        disc(buf, fx, fy, fz, r, c, segments, 1f, 0f, 0f, 0f, 1f, 0f)
        disc(buf, fx, fy, fz, r, c, segments, 1f, 0f, 0f, 0f, 0f, 1f)
        disc(buf, fx, fy, fz, r, c, segments, 0f, 1f, 0f, 0f, 0f, 1f)
    }

    /**
     * A 6-point sparkle: three pairs of tapered spikes along X, Y and Z, each one
     * a thin triangle that fades out at the tip. This is what gives the aura the
     * "magic sparkle" look instead of flat squares.
     */
    private fun star(buf: TriBuffer, x: Double, y: Double, z: Double, s: Double, c: Color, twist: Double) {
        val cr = c.red / 255f
        val cg = c.green / 255f
        val cb = c.blue / 255f
        val ca = c.alpha / 255f
        if (ca <= 0f) return

        val fx = x.toFloat()
        val fy = y.toFloat()
        val fz = z.toFloat()
        val len = (s * 3.4).toFloat()
        val half = (s * 0.42).toFloat()

        // Spikes are twisted per particle so they do not all point the same way.
        val ct = cos(twist).toFloat()
        val st = sin(twist).toFloat()

        val dirs = arrayOf(
            floatArrayOf(ct, st, 0f), floatArrayOf(- ct, - st, 0f),
            floatArrayOf(- st, ct, 0f), floatArrayOf(st, - ct, 0f),
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, - 1f)
        )
        val sides = arrayOf(
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, 1f),
            floatArrayOf(ct, st, 0f), floatArrayOf(ct, st, 0f)
        )

        for (i in dirs.indices) {
            val d = dirs[i]
            val p = sides[i]

            val tipX = fx + d[0] * len
            val tipY = fy + d[1] * len
            val tipZ = fz + d[2] * len

            buf.vertex(fx + p[0] * half, fy + p[1] * half, fz + p[2] * half, cr, cg, cb, ca)
            buf.vertex(fx - p[0] * half, fy - p[1] * half, fz - p[2] * half, cr, cg, cb, ca)
            buf.vertex(tipX, tipY, tipZ, cr, cg, cb, 0f)
        }
    }

    /** Full particle sprite: shape + optional wide halo layers. */
    private fun sprite(buf: TriBuffer, x: Double, y: Double, z: Double, s: Double, c: Color, index: Int, simple: Boolean) {
        val segments = if (simple) (cfg.quality / 2).coerceAtLeast(4) else cfg.quality

        if (cfg.shape != 2) orb(buf, x, y, z, s, c, segments)
        if (cfg.shape != 1 && ! simple) star(buf, x, y, z, s, c, hash(index, 41) * PI)

        if (! cfg.glow) return
        val halo1 = Color(c.red, c.green, c.blue, (c.alpha * 0.26).toInt().coerceIn(0, 255))
        val halo2 = Color(c.red, c.green, c.blue, (c.alpha * 0.10).toInt().coerceIn(0, 255))
        orb(buf, x, y, z, s * 2.1, halo1, (segments / 2).coerceAtLeast(4))
        if (! simple) orb(buf, x, y, z, s * 3.6, halo2, (segments / 2).coerceAtLeast(4))
    }


 fun build(time:Double):FloatArray {
  require(time.isFinite());val t=time*cfg.speed;val buf=TriBuffer()
  for(i in 0 until cfg.count) {
   var previous:Triple<Double,Double,Double>?=null
   for(k in 0..cfg.trail) {
    val fade=if(cfg.trail==0)1.0 else (1.0-k.toDouble()/(cfg.trail+1)).pow(2)
    val (x,y,z)=offsetAt(i,t-k*.05)
    val c=colorFor(i,fade,t)
    sprite(buf,x,y,z,cfg.size*(.45+.55*fade),c,i,k>0)
    if(cfg.ribbon && previous!=null) {
     // Two crossed gradient ribbons, avoiding a second deferred world-line pass.
     val (px,py,pz)=previous;val w=(cfg.size*.12).toFloat()
     for(axis in 0..1) {
      val dx=if(axis==0)w else 0f;val dy=if(axis==1)w else 0f
      fun v(a:Double,b:Double,d:Double,sign:Int) {buf.vertex(a.toFloat()+dx*sign,b.toFloat()+dy*sign,d.toFloat(),c.red/255f,c.green/255f,c.blue/255f,c.alpha/255f*.55f)}
      v(px,py,pz,1);v(px,py,pz,-1);v(x,y,z,1)
      v(x,y,z,1);v(px,py,pz,-1);v(x,y,z,-1)
     }
    }
    previous=Triple(x,y,z)
   }
  }
  return buf.finish()
 }
 fun position(index:Int,time:Double)=offsetAt(index,time*cfg.speed)
}
