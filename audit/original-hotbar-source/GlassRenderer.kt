package dev.eppopkaaddons.ui.clickgui.foundation

import dev.eppopkaaddons.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.sqrt

object GlassRenderer {
    fun GuiGraphicsExtractor.rounded(rect: Rect, radius: Float, color: Color) {
        if (rect.width <= 0f || rect.height <= 0f || color.alpha == 0) return
        val r = radius.coerceIn(0f, minOf(rect.width, rect.height) * .5f)
        if (r < 1f) { drawRect(rect.x, rect.y, rect.width, rect.height, color); return }
        drawRect(rect.x, rect.y + r, rect.width, rect.height - r * 2f, color)
        val rr = r * r
        repeat(ceil(r).toInt()) { row ->
            val sy = r - row - .5f
            val inset = r - sqrt((rr - sy * sy).coerceAtLeast(0f))
            drawRect(rect.x + inset, rect.y + row, rect.width - inset * 2f, 1f, color)
            drawRect(rect.x + inset, rect.bottom - row - 1f, rect.width - inset * 2f, 1f, color)
        }
    }

    fun GuiGraphicsExtractor.surface(rect: Rect, radius: Float, fill: Color, border: Color) {
        rounded(rect.expand(7f), radius + 7f, Color(0, 0, 0, 12))
        rounded(rect.expand(3f), radius + 3f, Color(0, 0, 0, 18))
        rounded(rect, radius, border)
        rounded(rect.inset(1f), (radius - 1f).coerceAtLeast(0f), fill)
        rounded(Rect(rect.x + radius, rect.y + 1f, rect.width - radius * 2f, 1f), .5f, Color(255, 255, 255, 17))
    }

    fun GuiGraphicsExtractor.pill(rect: Rect, color: Color) = rounded(rect, rect.height * .5f, color)
}
