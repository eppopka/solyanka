package dev.eppopkaaddons.features.impl.visual

import dev.eppopkaaddons.config.types.SliderSetting
import dev.eppopkaaddons.config.types.ToggleSetting
import dev.eppopkaaddons.config.types.ColorSetting
import dev.eppopkaaddons.features.Feature
import dev.eppopkaaddons.EppopkaAddons.mc
import dev.eppopkaaddons.ui.clickgui.foundation.FontSystem
import dev.eppopkaaddons.ui.clickgui.foundation.GlassRenderer.rounded
import dev.eppopkaaddons.ui.clickgui.foundation.Rect
import dev.eppopkaaddons.utils.render.world.NoammRenderPipelines
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import dev.eppopkaaddons.utils.render.withLineBuffer
import com.mojang.math.Axis
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.universal.vertex.UBuiltBuffer
import gg.essential.universal.vertex.UVertexConsumer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Item Aura - glowing VFX welded onto the held item model.
 *
 * Render path (see MixinItemAuraInHand for the full story): the item's pose is
 * snapshotted while the arm transform is active, and the aura itself is drawn at
 * the very end of the first person hand pass, after the hand batch has been
 * flushed. That is the only place where the aura can actually end up in front of
 * the item model on this MC version.
 *
 * Because the aura has to be drawn inside the hand pass (and not with the rest
 * of the world geometry), it does NOT go through RenderBatcher - that one is
 * flushed after translucent terrain, long before the hand is rendered. Instead
 * the aura builds and draws its own buffers immediately, using the same
 * UniversalCraft pipelines the rest of the mod's world renderer uses.
 *
 * Styles: 1 Orbit Rings, 2 DNA Helix, 3 Comet Swarm, 4 Vortex, 5 Fireflies.
 */
object ItemAura: Feature(
    name = "Item Aura",
    description = "Custom glowing VFX welded onto the item model in your hand."
) {
    private val style by SliderSetting("Style", 1, 1, 5, 1)
        .withDescription("1 = Orbit Rings, 2 = DNA Helix, 3 = Comet Swarm, 4 = Vortex, 5 = Fireflies.")
    private val shape by SliderSetting("Sprite", 3, 1, 3, 1)
        .withDescription("1 = Soft Orb, 2 = Sparkle Star, 3 = Orb + Star.")
    private val count by SliderSetting("Particles", 20, 4, 64, 1)
    private val radius by SliderSetting("Radius", 0.32f, 0.05f, 1.5f, 0.01f)
        .withDescription("Size of the aura around the item model.")
    private val size by SliderSetting("Particle Size", 0.04f, 0.004f, 0.2f, 0.002f)
    private val speed by SliderSetting("Speed", 1f, 0.1f, 4f, 0.1f)
    private val trail by SliderSetting("Trail Length", 6, 0, 16, 1)
        .withDescription("How many fading echoes follow each particle.")
    private val ribbon by ToggleSetting("Ribbon Lines")
        .withDescription("Connects each trail with a glowing streak line.")
    private val glow by ToggleSetting("Soft Glow")
        .withDescription("Extra wide faint halo layers. Turn off for a crisper, cheaper look.")
    private val quality by SliderSetting("Smoothness", 12, 5, 24, 1)
        .withDescription("Segments per glow disc. Lower it if the aura costs too many FPS.")

    private val rainbow by ToggleSetting("Rainbow")
    private val hueSpread by SliderSetting("Hue Spread", 0.35f, 0f, 1f, 0.05f).showIf { rainbow.value }
    private val auraColor by ColorSetting("Aura Color", Color(120, 220, 255, 180), true)
        .withDescription("RGB/HSV aura color and opacity. RGB is ignored while Rainbow is enabled.")
    private val depth by ToggleSetting("Depth Sorting")
        .withDescription("Lets the item model occlude particles that are behind it, so the aura reads as flying AROUND the item. Turn off to always draw the aura on top.")

    private val offsetX by SliderSetting("Offset X", 0.06f, - 0.6f, 0.6f, 0.01f)
        .withDescription("Aura center on the item model. Defaults are tuned to the first-person item position; tweak if your aura sits off-center.")
    private val offsetY by SliderSetting("Offset Y", 0.22f, - 0.6f, 0.6f, 0.01f)
    private val offsetZ by SliderSetting("Offset Z", 0.06f, - 0.6f, 0.6f, 0.01f)
    private val rotX by SliderSetting("Rotate X", 0f, - 180f, 180f, 1f)
        .withDescription("Tilts the whole aura, so rings and helixes can be lined up with the blade or tool head.")
    private val rotY by SliderSetting("Rotate Y", 0f, - 180f, 180f, 1f)
    private val rotZ by SliderSetting("Rotate Z", 0f, - 180f, 180f, 1f)
    private val stretch by SliderSetting("Stretch", 1f, 0.2f, 3f, 0.05f)
        .withDescription("Scales the aura along its own Y axis after rotation - useful to wrap it around a long sword.")
    private val spin by SliderSetting("Auto Spin", 0f, - 180f, 180f, 5f)
        .withDescription("Degrees per second the whole aura keeps rotating around its Y axis. 0 = off.")
    private val offHand by ToggleSetting("Off Hand Too")
        .withDescription("Also renders the aura around the off hand item.")
    private val onlyWithItem by ToggleSetting("Only With Item")
        .withDescription("Hides the aura when the hand is empty.")

    private class Captured(val matrix: Matrix4f)

    private val captured = ArrayList<Captured>(2)

    /**
     * Overlay for the live calibration mode.
     *
     * The preview itself is deliberately not re-created in GUI space. While this
     * inspector is open SpatialCameraController restores first person, so the
     * normal hand render pass behind this overlay is the exact preview (real arm,
     * current stack, transforms, lighting, aura and depth buffer).
     */
    fun renderGuiPreview(ctx: GuiGraphicsExtractor, area: Rect, alpha: Float, featureName: String = name) {
        val accent = if (rainbow.value) {
            val rgb = Color.HSBtoRGB(((System.nanoTime() / 1_000_000_000.0) * .08 % 1.0).toFloat(), .74f, 1f)
            Color((rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255)
        } else Color(auraColor.value.red, auraColor.value.green, auraColor.value.blue)
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()

        ctx.rounded(area, 13f, Color(2, 5, 10, (104 * alpha).toInt()))
        ctx.rounded(Rect(area.x + 1f, area.y + 1f, area.width - 2f, 1.5f), .75f, Color(accent.red, accent.green, accent.blue, (185 * alpha).toInt()))
        FontSystem.draw(ctx, "${featureName.uppercase()}  //  FIRST-PERSON PREVIEW", area.x + 12f, area.y + 9f, Color(229, 235, 247, a), .50f, true)
        FontSystem.draw(ctx, "LIVE", area.right - 38f, area.y + 9f, Color(accent.red, accent.green, accent.blue, a), .45f, true)

        val held = mc.player?.mainHandItem
        val status = if (held == null || held.isEmpty) "HOLD AN ITEM — GAMEPLAY VIEW IS LIVE" else "REAL HAND PASS  •  DEPTH + LIGHTING LIVE"
        FontSystem.draw(ctx, status, area.x + 12f, area.y + 36f, Color(157, 166, 184, a), .45f, true)
    }

    private fun now(): Double = (System.nanoTime() / 1_000_000_000.0) * speed.value

    /** Deterministic per-particle pseudo random in 0..1. */
    private fun hash(i: Int, salt: Int): Double {
        val x = sin(i * 127.1 + salt * 311.7) * 43758.5453
        return x - floor(x)
    }

    private fun colorFor(index: Int, fade: Double, t: Double): Color {
        val alpha = (auraColor.value.alpha * fade).toInt().coerceIn(0, 255)
        if (! rainbow.value) return Color(auraColor.value.red, auraColor.value.green, auraColor.value.blue, alpha)

        val hue = ((t * 0.08) + index * hueSpread.value / count.value.coerceAtLeast(1)).mod(1.0)
        val rgb = Color.HSBtoRGB(hue.toFloat(), 0.75f, 1f)
        return Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, alpha)
    }

    /** Particle offset in item model space at animation time [t]. */
    private fun offsetAt(i: Int, t: Double): Triple<Double, Double, Double> {
        val n = count.value.coerceAtLeast(1)
        val r = radius.value.toDouble()
        val phase = i * 2.0 * PI / n

        return when (style.value) {
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

    /** Collects triangle vertices, then draws them in one go. */
    private class TriBuffer(private val pose: UMatrixStack) {
        private val builder = UBufferBuilder.create(UGraphics.DrawMode.TRIANGLES, UGraphics.CommonVertexFormats.POSITION_COLOR)
        private var empty = true

        fun vertex(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float) {
            builder.pos(pose, x.toDouble(), y.toDouble(), z.toDouble())
            builder.color(r, g, b, a)
            builder.endVertex()
            empty = false
        }

        fun draw(phase: Boolean) {
            if (empty) return
            val pipeline = if (phase) NoammRenderPipelines.FILLED_THROUGH_WALLS else NoammRenderPipelines.FILLED
            builder.build()?.drawAndClose(pipeline) { noScissor() }
        }
    }

    /**
     * One gradient disc: bright in the middle, alpha 0 at the rim. Built as a
     * triangle fan expanded into triangles, because the FILLED pipeline is in
     * TRIANGLES mode.
     *
     * [ux]/[uy]/[uz] and [vx]/[vy]/[vz] are the two in-plane axes, so the same
     * helper can draw the XY / XZ / YZ slices of an orb.
     */
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
        val segments = if (simple) (quality.value / 2).coerceAtLeast(4) else quality.value

        if (shape.value != 2) orb(buf, x, y, z, s, c, segments)
        if (shape.value != 1 && ! simple) star(buf, x, y, z, s, c, hash(index, 41) * PI)

        if (! glow.value) return
        val halo1 = Color(c.red, c.green, c.blue, (c.alpha * 0.26).toInt().coerceIn(0, 255))
        val halo2 = Color(c.red, c.green, c.blue, (c.alpha * 0.10).toInt().coerceIn(0, 255))
        orb(buf, x, y, z, s * 2.1, halo1, (segments / 2).coerceAtLeast(4))
        if (! simple) orb(buf, x, y, z, s * 3.6, halo2, (segments / 2).coerceAtLeast(4))
    }

    /**
     * Step 1 of the render path: snapshot the item's transform (plus the user's
     * offset / rotation / stretch) while the arm transform is still applied.
     */
    @JvmStatic
    fun captureHand(poseStack: PoseStack, hand: InteractionHand, stack: ItemStack) {
        if (! enabled) return
        if (hand == InteractionHand.OFF_HAND && ! offHand.value) return
        if (onlyWithItem.value && stack.isEmpty) return

        // Mirror the sideways offset for the off hand so the aura stays centered there too.
        val handSign = if (hand == InteractionHand.MAIN_HAND) 1f else - 1f

        poseStack.pushPose()
        poseStack.translate(
            (offsetX.value * handSign).toDouble(),
            offsetY.value.toDouble(),
            offsetZ.value.toDouble()
        )

        if (rotX.value != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(rotX.value))
        if (rotY.value != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(rotY.value * handSign))
        if (rotZ.value != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ.value * handSign))
        if (spin.value != 0f) {
            val angle = (System.nanoTime() / 1_000_000_000.0 * spin.value).mod(360.0).toFloat()
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        }
        if (stretch.value != 1f) poseStack.scale(1f, stretch.value, 1f)

        captured.add(Captured(Matrix4f(poseStack.last().pose())))
        poseStack.popPose()
    }

    /**
     * Step 2: called at the very end of the first person hand pass, after the item
     * model has already been flushed to the screen. Drawing here is what makes the
     * aura land in front of the item instead of behind it.
     */
    @JvmStatic
    fun drawCaptured() {
        val pending = ArrayList(captured)
        captured.clear()
        if (pending.isEmpty() || ! enabled) return

        val phase = ! depth.value
        val t = now()
        val n = count.value.coerceAtLeast(1)
        val baseSize = size.value.toDouble()
        val trailLen = trail.value

        for (cap in pending) {
            // Rebuild a PoseStack holding exactly the captured transform.
            val ps = PoseStack()
            ps.mulPose(cap.matrix)
            val pose = UMatrixStack(ps.last())

            val fill = TriBuffer(pose)
            for (i in 0 until n) {
                for (k in 0 .. trailLen) {
                    val fade = if (trailLen == 0) 1.0 else (1.0 - k.toDouble() / (trailLen + 1)).let { it * it }
                    val (x, y, z) = offsetAt(i, t - k * 0.05)
                    sprite(fill, x, y, z, baseSize * (0.45 + 0.55 * fade), colorFor(i, fade, t), i, k > 0)
                }
            }
            fill.draw(phase)

            if (! ribbon.value || trailLen <= 0) continue

            withLineBuffer { mcBuffer ->
                val lines = UVertexConsumer.of(mcBuffer)
                var wroteLine = false

                for (i in 0 until n) {
                    var px = 0.0
                    var py = 0.0
                    var pz = 0.0
                    var has = false

                    for (k in 0 .. trailLen) {
                        val fade = (1.0 - k.toDouble() / (trailLen + 1)).let { it * it }
                        val (x, y, z) = offsetAt(i, t - k * 0.05)
                        if (has) {
                            val c = colorFor(i, fade, t)
                            val a = (c.alpha * 0.55f / 255f).coerceIn(0f, 1f)

                            var dx = (x - px).toFloat()
                            var dy = (y - py).toFloat()
                            var dz = (z - pz).toFloat()
                            val lenSq = dx * dx + dy * dy + dz * dz
                            if (lenSq > 0f) {
                                val inv = 1f / sqrt(lenSq)
                                dx *= inv
                                dy *= inv
                                dz *= inv
                            }

                            lines.pos(pose, px, py, pz)
                            lines.color(c.red / 255f, c.green / 255f, c.blue / 255f, a)
                            lines.norm(pose, dx, dy, dz)
                            mcBuffer.setLineWidth(2f)
                            lines.endVertex()

                            lines.pos(pose, x, y, z)
                            lines.color(c.red / 255f, c.green / 255f, c.blue / 255f, a)
                            lines.norm(pose, dx, dy, dz)
                            mcBuffer.setLineWidth(2f)
                            lines.endVertex()

                            wroteLine = true
                        }
                        px = x
                        py = y
                        pz = z
                        has = true
                    }
                }

                val built = mcBuffer.build()
                if (wroteLine && built != null) {
                    val pipeline = if (phase) NoammRenderPipelines.LINES_THROUGH_WALLS else NoammRenderPipelines.LINES
                    UBuiltBuffer.wrap(built).drawAndClose(pipeline) { noScissor() }
                }
            }
        }
    }
}
