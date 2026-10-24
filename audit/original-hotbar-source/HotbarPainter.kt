package dev.eppopkaaddons.ui.hud.hotbar

import dev.eppopkaaddons.ui.clickgui.foundation.GlassRenderer.rounded
import dev.eppopkaaddons.ui.clickgui.foundation.Rect
import dev.eppopkaaddons.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.awt.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** GUI-only effects; no fullscreen post-processing or custom shader assets. */
object HotbarPainter {
    enum class Style { VANILLA, MINIMAL, GLASS, NEON }

    data class Appearance(
        val style: Style,
        val background: Color,
        val secondary: Color,
        val accent: Color,
        val opacity: Float,
        val radius: Float,
        val borderWidth: Float,
        val gradient: Boolean,
        val gradientVertical: Boolean,
        val shadow: Boolean,
        val shadowStrength: Float,
        val glow: Boolean,
        val glowStrength: Float,
        val effectRadius: Float,
        val shimmer: Boolean,
        val shimmerSpeed: Float,
        val selectionOpacity: Float,
        val selectionRadius: Float,
    )

    private val hotbar = Identifier.withDefaultNamespace("hud/hotbar")

    fun background(ctx: GuiGraphicsExtractor, layout: HotbarLayout, look: Appearance, seconds: Float) {
        surface(ctx, layout.bounds, look, seconds)
        if (look.style == Style.VANILLA) {
            val main = layout.mainPanel
            if (layout.vertical) {
                // Do not squash the horizontal sprite into a vertical strip:
                // crop a native cell for each upright item instead.
                for (i in 0..8) vanillaCell(ctx, layout.slot(i.toFloat()))
            } else {
                ctx.blitSprite(RenderPipelines.GUI_TEXTURED, hotbar, main.x.toInt(), main.y.toInt(),
                    main.width.toInt(), main.height.toInt())
            }
            layout.offhand?.let { vanillaCell(ctx, it) }
            return
        }

        if (look.style != Style.MINIMAL) {
            for (i in 0..8) {
                val cell = layout.slot(i.toFloat()).rect()
                ctx.rounded(cell.inset(1f), min(look.selectionRadius, cell.height / 2f),
                    Color(255, 255, 255, if (look.style == Style.GLASS) 11 else 7))
            }
        }
    }

    fun surface(ctx: GuiGraphicsExtractor, box: HotbarBox, look: Appearance, seconds: Float) {
        val panel = box.rect()
        if (look.shadow) halo(ctx, panel.moved(dy = 2f), look.radius, Color.BLACK,
            look.shadowStrength * .38f, look.effectRadius)
        if (look.glow) halo(ctx, panel, look.radius, look.accent,
            look.glowStrength * if (look.style == Style.NEON) .35f else .19f, look.effectRadius)

        if (look.style == Style.VANILLA) return

        val alpha = (look.opacity * 255f).toInt().coerceIn(0, 255)
        val a = look.background.alpha(alpha)
        val b = if (look.gradient) look.secondary.alpha(alpha) else a
        gradient(ctx, panel, look.radius, a, b, look.gradientVertical,
            if (look.shimmer) seconds * look.shimmerSpeed else null)
        if (look.style == Style.GLASS) {
            // A soft translucent coating differentiates glass from the flat
            // Minimal preset without claiming to blur the world behind it.
            gradient(ctx, panel, look.radius,
                Color(255, 255, 255, (look.opacity * 20f).toInt()),
                Color(255, 255, 255, 0), true, null)
        }
        val borderColor = when (look.style) {
            Style.NEON -> look.accent.alpha(190)
            Style.GLASS -> Color(255, 255, 255, 40)
            else -> Color(255, 255, 255, 24)
        }
        border(ctx, panel, look.radius, look.borderWidth, borderColor)
        if (look.style == Style.GLASS) {
            // Restrained top sheen; deliberately translucent, not fake blur.
            val sheen = Rect(panel.x + look.radius + 1f, panel.y + 1f,
                max(0f, panel.width - 2f * look.radius - 2f), 1f)
            ctx.rounded(sheen, .5f, Color(255, 255, 255, 30))
        }
    }

    private fun vanillaCell(ctx: GuiGraphicsExtractor, cell: HotbarBox) {
        ctx.pose().pushMatrix()
        try {
            ctx.pose().translate(cell.x, cell.y)
            ctx.pose().scale(cell.width / 22f, cell.height / 22f)
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, hotbar,
                182, 22, 0, 0, 0, 0, 22, 22)
        } finally { ctx.pose().popMatrix() }
    }

    fun selection(ctx: GuiGraphicsExtractor, cell: HotbarBox, look: Appearance) {
        val rect = cell.rect()
        val radius = look.selectionRadius.coerceAtMost(rect.width / 2f)
        ctx.rounded(rect, radius, look.accent.alpha((look.selectionOpacity * 255).toInt()))
        border(ctx, rect, radius, look.borderWidth.coerceAtLeast(1f), look.accent.alpha(230))
    }

    private fun halo(ctx: GuiGraphicsExtractor, rect: Rect, radius: Float,
                     color: Color, strength: Float, spread: Float) {
        if (strength <= 0f || spread <= 0f) return
        val layers = ceil(spread).toInt().coerceIn(1, 12)
        for (i in layers downTo 1) {
            val distance = spread * i / layers
            val alpha = (strength * 255f / layers * (1f - i.toFloat() / (layers + 1))).toInt()
            ctx.rounded(rect.expand(distance), radius + distance, color.alpha(alpha))
        }
    }

    /** One clipped strip per pixel, so gradients/shimmer cannot escape rounded corners. */
    private fun gradient(ctx: GuiGraphicsExtractor, rect: Rect, radius: Float,
                         a: Color, b: Color, vertical: Boolean, shimmer: Float?) {
        if (a.alpha == 0 && b.alpha == 0) return
        val length = if (vertical) rect.height else rect.width
        val cross = if (vertical) rect.width else rect.height
        val r = radius.coerceIn(0f, min(length, cross) / 2f)
        val count = ceil(length).toInt()
        for (i in 0 until count) {
            val thickness = min(1f, length - i)
            val position = i + thickness / 2f
            val inset = cornerInset(position, length, r)
            val progress = (position / length).coerceIn(0f, 1f)
            var color = mix(a, b, progress)
            if (shimmer != null) {
                val head = ((shimmer % 1f) + 1f) % 1f * 1.5f - .25f
                val intensity = max(0f, 1f - abs(progress - head) / .14f) * .16f
                color = mix(color, Color(255, 255, 255, color.alpha), intensity)
            }
            if (vertical) ctx.drawRect(rect.x + inset, rect.y + i, cross - inset * 2f, thickness, color)
            else ctx.drawRect(rect.x + i, rect.y + inset, thickness, cross - inset * 2f, color)
        }
    }

    /** Draw a real rounded ring without repainting/flattening the interior gradient. */
    private fun border(ctx: GuiGraphicsExtractor, rect: Rect, radius: Float, width: Float, color: Color) {
        if (width <= 0f) return
        val r = radius.coerceIn(0f, min(rect.width, rect.height) / 2f)
        val inner = rect.inset(width)
        for (row in 0 until ceil(rect.height).toInt()) {
            val h = min(1f, rect.height - row)
            val y = row + h / 2f
            val outerInset = cornerInset(y, rect.height, r)
            val left = rect.x + outerInset
            val right = rect.right - outerInset
            if (y < width || y >= rect.height - width || inner.width <= 0f || inner.height <= 0f) {
                ctx.drawRect(left, rect.y + row, right - left, h, color)
            } else {
                val innerInset = cornerInset(y - width, inner.height, max(0f, r - width))
                val innerLeft = inner.x + innerInset
                val innerRight = inner.right - innerInset
                ctx.drawRect(left, rect.y + row, max(0f, innerLeft - left), h, color)
                ctx.drawRect(innerRight, rect.y + row, max(0f, right - innerRight), h, color)
            }
        }
    }

    private fun cornerInset(position: Float, length: Float, radius: Float): Float {
        if (radius <= 0f) return 0f
        val edge = min(position, length - position)
        if (edge >= radius) return 0f
        val dy = radius - edge
        return radius - sqrt(max(0f, radius * radius - dy * dy))
    }

    private fun HotbarBox.rect() = Rect(x, y, width, height)
    private fun Color.alpha(value: Int) = Color(red, green, blue, value.coerceIn(0, 255))
    private fun mix(a: Color, b: Color, t: Float): Color {
        fun blend(x: Int, y: Int) = (x + (y - x) * t).toInt().coerceIn(0, 255)
        return Color(blend(a.red, b.red), blend(a.green, b.green), blend(a.blue, b.blue), blend(a.alpha, b.alpha))
    }
}
