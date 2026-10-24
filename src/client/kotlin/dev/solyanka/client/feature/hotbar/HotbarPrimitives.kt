/* SPDX-License-Identifier: CC0-1.0; Rect and drawRect from eppopkaaddons; unrelated clocks excluded. */
package dev.solyanka.client.feature.hotbar
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
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

object HotbarPrimitives {
    fun GuiGraphicsExtractor.drawRect(x: Number, y: Number, width: Number, height: Number, color: Color = Color.WHITE) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = width.toFloat()
        val fh = height.toFloat()

        pose().pushMatrix()
        pose().translate(fx, fy)
        pose().scale(fw, fh)
        fill(0, 0, 1, 1, color.rgb)
        pose().popMatrix()
    }

}
