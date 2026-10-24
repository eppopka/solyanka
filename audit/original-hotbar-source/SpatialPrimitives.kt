package dev.eppopkaaddons.ui.clickgui.foundation

import dev.eppopkaaddons.features.impl.general.AetherOSSettings
import java.awt.Color
import kotlin.math.abs

object FrameClock {
    var now = System.nanoTime(); private set
    var delta = 0f; private set
    fun tick() {
        val next = System.nanoTime()
        delta = ((next - now) / 1_000_000_000.0).toFloat().coerceIn(0f, 1f / 20f)
        now = next
    }
}

/** Compatibility clock for the few non-/ea animations that share frame timing. */
object MotionClock {
    val nowNanos get() = FrameClock.now
    val deltaSeconds get() = FrameClock.delta
    fun beginFrame() = FrameClock.tick()
}

data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right get() = x + width
    val bottom get() = y + height
    val centerX get() = x + width * .5f
    val centerY get() = y + height * .5f
    fun contains(px: Float, py: Float) = px >= x && px <= right && py >= y && py <= bottom
    fun inset(v: Float) = Rect(x + v, y + v, width - v * 2f, height - v * 2f)
    fun expand(v: Float) = Rect(x - v, y - v, width + v * 2f, height + v * 2f)
    fun moved(dx: Float = 0f, dy: Float = 0f) = copy(x = x + dx, y = y + dy)
    fun scaled(scale: Float) = Rect(centerX - width * scale * .5f, centerY - height * scale * .5f, width * scale, height * scale)
}

class Spring(initial: Float = 0f, private val stiffness: Float = 330f, private val damping: Float = 31f) {
    var value = initial; private set
    private var velocity = 0f
    fun snap(next: Float) { value = next; velocity = 0f }
    fun to(target: Float): Float {
        if (!AetherOSSettings.animations.value || AetherOSSettings.reducedMotion.value) {
            snap(target)
            return value
        }
        var remaining = FrameClock.delta * AetherOSSettings.animationSpeed.value
        while (remaining > 0f) {
            val dt = minOf(remaining, 1f / 120f)
            velocity += ((target - value) * stiffness - velocity * damping) * dt
            value += velocity * dt
            remaining -= dt
        }
        if (abs(value - target) < .0004f && abs(velocity) < .004f) snap(target)
        return value
    }
}

fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
fun mix(a: Color, b: Color, t: Float) = Color(
    mix(a.red.toFloat(), b.red.toFloat(), t).toInt().coerceIn(0, 255),
    mix(a.green.toFloat(), b.green.toFloat(), t).toInt().coerceIn(0, 255),
    mix(a.blue.toFloat(), b.blue.toFloat(), t).toInt().coerceIn(0, 255),
    mix(a.alpha.toFloat(), b.alpha.toFloat(), t).toInt().coerceIn(0, 255),
)
fun Color.alpha(value: Number) = Color(red, green, blue, value.toInt().coerceIn(0, 255))
