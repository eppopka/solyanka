package dev.eppopkaaddons.ui.hud.hotbar

import kotlin.math.exp
import kotlin.math.min

/** Pure geometry, shared by gameplay, the HUD editor, and regression tests. */
data class HotbarBox(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right get() = x + width
    val bottom get() = y + height
    val centerX get() = x + width / 2f
    val centerY get() = y + height / 2f
}

data class HotbarLayout(
    val vertical: Boolean,
    val slotSize: Float,
    val gap: Float,
    val padding: Float,
    val hasOffhand: Boolean,
    val offhandBefore: Boolean,
    val hasAttackIndicator: Boolean,
) {
    private val auxiliaryGap = 4f
    private val before = (hasOffhand && offhandBefore) || (hasAttackIndicator && !offhandBefore)
    private val after = (hasOffhand && !offhandBefore) || (hasAttackIndicator && offhandBefore)
    private val beforeSpan = if (before) slotSize + auxiliaryGap else 0f
    private val afterSpan = if (after) slotSize + auxiliaryGap else 0f
    val mainLength = slotSize * 9f + gap * 8f
    private val length = mainLength + padding * 2f + beforeSpan + afterSpan
    val width = if (vertical) slotSize + padding * 2f else length
    val height = if (vertical) length else slotSize + padding * 2f
    val bounds get() = HotbarBox(0f, 0f, width, height)
    val mainBounds get() = if (vertical) {
        HotbarBox(padding, padding + beforeSpan, slotSize, mainLength)
    } else {
        HotbarBox(padding + beforeSpan, padding, mainLength, slotSize)
    }
    val mainPanel get() = if (vertical) {
        HotbarBox(0f, beforeSpan, width, mainLength + padding * 2f)
    } else {
        HotbarBox(beforeSpan, 0f, mainLength + padding * 2f, height)
    }

    fun slot(index: Float): HotbarBox {
        val offset = index.coerceIn(0f, 8f) * (slotSize + gap)
        return if (vertical) HotbarBox(padding, padding + beforeSpan + offset, slotSize, slotSize)
        else HotbarBox(padding + beforeSpan + offset, padding, slotSize, slotSize)
    }

    private fun auxiliary(atStart: Boolean): HotbarBox {
        val offset = if (atStart) padding else padding + beforeSpan + mainLength + auxiliaryGap
        return if (vertical) HotbarBox(padding, offset, slotSize, slotSize)
        else HotbarBox(offset, padding, slotSize, slotSize)
    }

    val offhand get() = if (hasOffhand) auxiliary(offhandBefore) else null
    val attackIndicator get() = if (hasAttackIndicator) auxiliary(!offhandBefore) else null
}

data class HotbarViewport(val scale: Float, val width: Float, val height: Float) {
    fun fittedScale(requested: Float, layout: HotbarLayout, sideMargin: Float,
                    topMargin: Float, bottomMargin: Float): Float = min(requested, min(
        width / (layout.width + sideMargin * 2f),
        height / (layout.height + topMargin + bottomMargin),
    ))

    companion object {
        fun fromGui(width: Int, height: Int): HotbarViewport {
            val scale = min(width / 960f, height / 540f).coerceAtLeast(.001f)
            return HotbarViewport(scale, width / scale, height / scale)
        }
    }
}

/** Frame-rate-independent smoothing without overshoot or a ClickGUI frame clock. */
class HotbarSelection {
    var value = 0f
        private set
    private var initialized = false

    fun reset() { initialized = false }

    fun update(target: Int, seconds: Float, speed: Float, animate: Boolean): Float {
        val next = target.coerceIn(0, 8).toFloat()
        if (!initialized || !animate) {
            value = next
            initialized = true
        } else {
            val blend = (1.0 - exp(-speed.coerceAtLeast(0f) * seconds.coerceIn(0f, .1f))).toFloat()
            value += (next - value) * blend
            if (kotlin.math.abs(next - value) < .001f) value = next
        }
        return value
    }
}
