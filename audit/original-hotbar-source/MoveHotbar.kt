package dev.eppopkaaddons.features.impl.visual

import dev.eppopkaaddons.config.ConfigManager
import dev.eppopkaaddons.config.types.ButtonSetting
import dev.eppopkaaddons.config.types.ColorSetting
import dev.eppopkaaddons.config.types.DropdownSetting
import dev.eppopkaaddons.config.types.SliderSetting
import dev.eppopkaaddons.config.types.ToggleSetting
import dev.eppopkaaddons.features.Feature
import dev.eppopkaaddons.features.impl.general.AetherOSSettings
import dev.eppopkaaddons.ui.clickgui.ClickGuis
import dev.eppopkaaddons.ui.clickgui.foundation.GlassRenderer.rounded
import dev.eppopkaaddons.ui.clickgui.foundation.Rect
import dev.eppopkaaddons.ui.hud.HudEditorScreen
import dev.eppopkaaddons.ui.hud.HudElement
import dev.eppopkaaddons.ui.hud.hotbar.HotbarBox
import dev.eppopkaaddons.ui.hud.hotbar.HotbarLayout
import dev.eppopkaaddons.ui.hud.hotbar.HotbarPainter
import dev.eppopkaaddons.ui.hud.hotbar.HotbarSelection
import dev.eppopkaaddons.ui.hud.hotbar.HotbarViewport
import dev.eppopkaaddons.utils.GuiUtils
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.AttackIndicatorStatus
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.min
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement as FabricHudElement

/** Styled hotbar, using native item extraction/decorations rather than custom item batches. */
object MoveHotbar : Feature(
    description = "Customize the hotbar's style, layout, colors, effects and position.",
    name = "Move Hotbar",
) {
    private val style by DropdownSetting("Style", 2, listOf("Vanilla", "Minimal", "Glass", "Neon"))
        .section("Appearance")
    private val layout by DropdownSetting("Layout", 0, listOf("Horizontal", "Vertical"))
        .onChange { selection.reset(); lastFrame = 0L }
    private val slotGap by SliderSetting("Slot Spacing", 2f, 0f, 10f, 1f)
        .showIf { style.value != 0 }
    private val padding by SliderSetting("Padding", 3f, 0f, 12f, 1f)
        .showIf { style.value != 0 }
    private val cornerRadius by SliderSetting("Corner Radius", 6f, 0f, 16f, .5f)
        .showIf { style.value != 0 }
    private val borderWidth by SliderSetting("Border Width", 1f, 0f, 3f, .5f)
        .showIf { style.value != 0 }

    private val backgroundColor by ColorSetting("Background Color", Color(12, 17, 25), false)
        .section("Background").showIf { style.value != 0 }
    private val backgroundOpacity by SliderSetting("Background Opacity", .7f, 0f, 1f, .01f)
        .showIf { style.value != 0 }
    private val gradient by ToggleSetting("Gradient", true).showIf { style.value != 0 }
    private val gradientColor by ColorSetting("Gradient Color", Color(26, 34, 48), false)
        .showIf { style.value != 0 && gradient.value }
    private val gradientDirection by DropdownSetting("Gradient Direction", 0, listOf("Along Hotbar", "Across Hotbar"))
        .showIf { style.value != 0 && gradient.value }

    private val accentColor by ColorSetting("Accent Color", Color(94, 159, 232), false)
        .section("Selection")
    private val selectionOpacity by SliderSetting("Selection Opacity", .22f, 0f, 1f, .01f)
    private val selectionRadius by SliderSetting("Selection Radius", 4f, 0f, 12f, .5f)
    private val smoothSelection by ToggleSetting("Smooth Selection", true)
    private val slideSpeed by SliderSetting("Slide Speed", 16f, 4f, 40f, 1f)
        .withDescription("Higher values move the selection highlight faster.")
        .showIf { smoothSelection.value }

    private val shadow by ToggleSetting("Shadow", true).section("Effects")
    private val shadowStrength by SliderSetting("Shadow Strength", 65f, 0f, 100f, 5f)
        .showIf { shadow.value }
    private val glow by ToggleSetting("Glow", true)
    private val glowStrength by SliderSetting("Glow Strength", 45f, 0f, 100f, 5f)
        .showIf { glow.value }
    private val effectRadius by SliderSetting("Effect Radius", 6f, 1f, 12f, 1f)
        .showIf { shadow.value || glow.value }
    private val shimmer by ToggleSetting("Animated Shimmer", false)
        .withDescription("A subtle moving highlight, clipped to the rounded background.")
        .showIf { style.value != 0 }
    private val rainbowAccent by ToggleSetting("Rainbow Accent", false)
    private val effectSpeed by SliderSetting("Effect Speed", 1f, .15f, 3f, .05f)
        .showIf { shimmer.value || rainbowAccent.value }

    private val itemScale by SliderSetting("Item Scale", 1f, .5f, 1.5f, .05f).section("Items")
        .withDescription("Fits native item icons and their decorations inside each slot.")
    private val pickupAnimation by ToggleSetting("Item Pickup Animation", true)
    private val showOffhand by ToggleSetting("Show Offhand", true)
    private val moveItemName by ToggleSetting("Move Item Name", false).section("Position")
        .withDescription("Move the native held-item name; choose Independent to drag it in /ea hud.")
    private val itemNamePosition by DropdownSetting("Item Name Position", 1, listOf("Follow Hotbar", "Independent"))
        .showIf { moveItemName.value }
    private val editItemName by ButtonSetting("Edit Item Name Position") {
        moveItemName.value = true
        itemNamePosition.value = 1
        ensureNamePosition()
        ClickGuis.closeOpen()
        GuiUtils.setScreen(HudEditorScreen())
    }
    private val moveInfoBar by ToggleSetting("Move XP Bar", false)
        .withDescription("The native XP/info bar and level follow above the hotbar; they stay horizontal.")
    private val keepOnScreen by ToggleSetting("Keep On Screen", true)
    private val editButton by ButtonSetting("Edit Position (HUD Editor)") {
        ensurePosition()
        ClickGuis.closeOpen()
        GuiUtils.setScreen(HudEditorScreen())
    }
    private val resetButton by ButtonSetting("Reset Position") {
        hud.x = 0f
        hud.y = 0f
        hud.scale = 1f
        referencePosition.value = true
        positionInitialized = false
        selection.reset()
        ensurePosition()
        ConfigManager.markDirty()
    }

    // Hidden migration marker: older MoveHotbar saved raw vanilla GUI coordinates.
    private val referencePosition by ToggleSetting("Reference Position V2", false).showIf { false }
    private val selection = HotbarSelection()
    private var positionInitialized = false
    private var namePositionInitialized = false
    private var lastFrame = 0L
    private val effectEpoch = System.nanoTime()

    // Preserve the existing HUD/config key. Only the editor draws this element;
    // gameplay is rendered exactly once by the replacement HOTBAR callback.
    private val hud = object : HudElement() {
        override val name = "Vanilla Hotbar"
        override val toggle get() = enabled
        override val shouldDraw = false

        override fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
            ensurePosition()
            super.drawBackground(ctx, mx, my)
        }

        override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Number, Number> {
            val geometry = geometry()
            ctx.pose().pushMatrix()
            try {
                val localScale = totalScale() / this.scale.coerceAtLeast(.001f)
                ctx.pose().scale(localScale, localScale)
                drawHotbar(ctx, geometry, DeltaTracker.ZERO, true)
            } finally { ctx.pose().popMatrix() }
            val localScale = totalScale() / this.scale.coerceAtLeast(.001f)
            return geometry.width * localScale to geometry.height * localScale
        }
    }.also(hudElements::add) defaults {
        x = 0f
        y = 0f
        scale = 1f
        positionInitialized = false
    }

    private val nameHud = object : HudElement() {
        override val name = "Held Item Name"
        override val centered = true
        override val toggle get() = enabled && moveItemName.value && itemNamePosition.value == 1
        override val shouldDraw = false

        override fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
            ensureNamePosition()
            super.drawBackground(ctx, mx, my)
        }

        override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Number, Number> {
            val label = itemNameLabel()
            val width = nameWidth()
            val height = nameHeight()
            val localScale = nameScale() / this.scale.coerceAtLeast(.001f)
            ctx.pose().pushMatrix()
            try {
                ctx.pose().scale(localScale, localScale)
                ctx.rounded(Rect(-width / 2f, 0f, width, height), 3f, Color(12, 17, 25, 170))
                ctx.text(mc.font, label, (-mc.font.width(label) / 2f).toInt(), 3, Color.WHITE.rgb, false)
            } finally { ctx.pose().popMatrix() }
            return width * localScale to height * localScale
        }
    }.also(hudElements::add) defaults {
        x = 0f
        y = 0f
        scale = 1f
        namePositionInitialized = false
    }

    init {
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR) { original ->
            FabricHudElement { graphics, delta ->
                val cameraPlayer = cameraPlayer()
                if (!enabled || cameraPlayer == null || cameraPlayer.isSpectator) {
                    original.extractRenderState(graphics, delta)
                } else if (!mc.gui.hud.isHidden) {
                    ensurePosition()
                    // The HUD editor draws its own correctly scaled preview.
                    if (mc.gui.screen() !is HudEditorScreen) {
                        val viewport = viewport()
                        val pose = graphics.pose()
                        pose.pushMatrix()
                        try {
                            pose.scale(viewport.scale, viewport.scale)
                            pose.translate(hud.x, hud.y)
                            pose.scale(totalScale(), totalScale())
                            drawHotbar(graphics, geometry(), delta, false)
                        } finally { pose.popMatrix() }
                    }
                }
            }
        }
        moveVanillaElement(VanillaHudElements.HELD_ITEM_TOOLTIP) { moveItemName.value }
        moveVanillaElement(VanillaHudElements.INFO_BAR) { moveInfoBar.value }
        moveVanillaElement(VanillaHudElements.EXPERIENCE_LEVEL) { moveInfoBar.value }
    }

    override fun onEnable() {
        positionInitialized = false
        lastFrame = 0L
        selection.reset()
        super.onEnable()
    }

    override fun onDisable() {
        super.onDisable()
        lastFrame = 0L
        selection.reset()
    }

    private fun cameraPlayer() = mc.cameraEntity as? Player
    private fun viewport() = HotbarViewport.fromGui(mc.window.guiScaledWidth, mc.window.guiScaledHeight)
    private fun effectMargin() = if (shadow.value || glow.value) effectRadius.value else 0f
    private fun topMargin() = maxOf(effectMargin(), when {
        moveItemName.value && itemNamePosition.value == 0 -> 42f
        moveInfoBar.value -> 18f
        else -> 0f
    })
    private fun bottomMargin() = effectMargin() + if (shadow.value) 2f else 0f
    private fun totalScale(): Float {
        val requested = hud.scale.coerceAtLeast(.05f)
        if (!keepOnScreen.value) return requested
        val geometry = geometry()
        val viewport = viewport()
        return viewport.fittedScale(requested, geometry, effectMargin(), topMargin(), bottomMargin())
    }
    private fun itemNameLabel() = cameraPlayer()?.mainHandItem?.takeUnless { it.isEmpty }?.hoverName
        ?: net.minecraft.network.chat.Component.literal("Held Item Name")
    private fun nameWidth() = mc.font.width(itemNameLabel()).toFloat() + 12f
    private fun nameHeight() = mc.font.lineHeight.toFloat() + 6f
    private fun nameScale(): Float {
        val requested = nameHud.scale.coerceAtLeast(.05f)
        if (!keepOnScreen.value) return requested
        val v = viewport()
        return min(requested, min(v.width / nameWidth(), v.height / nameHeight()))
    }
    private fun ensureNamePosition() {
        ensurePosition()
        val v = viewport()
        val s = nameScale()
        if (!namePositionInitialized) {
            namePositionInitialized = true
            if (nameHud.x == 0f && nameHud.y == 0f) {
                nameHud.x = hud.x + geometry().mainBounds.centerX * totalScale()
                nameHud.y = (hud.y - 37f * totalScale()).coerceAtLeast(0f)
            }
        }
        val half = nameWidth() * s / 2f
        if (keepOnScreen.value) {
            nameHud.x = nameHud.x.coerceIn(half, (v.width - half).coerceAtLeast(half))
            nameHud.y = nameHud.y.coerceIn(0f, (v.height - nameHeight() * s).coerceAtLeast(0f))
        }
        nameHud.width = nameWidth() * s / nameHud.scale.coerceAtLeast(.001f)
        nameHud.height = nameHeight() * s / nameHud.scale.coerceAtLeast(.001f)
    }

    private fun motionEnabled() = AetherOSSettings.animations.value && !AetherOSSettings.reducedMotion.value

    private fun geometry(): HotbarLayout {
        val player = cameraPlayer()
        return HotbarLayout(
            vertical = layout.value == 1,
            slotSize = if (style.value == 0) 20f else 22f,
            gap = if (style.value == 0) 0f else slotGap.value,
            padding = if (style.value == 0) 1f else padding.value,
            hasOffhand = showOffhand.value && player?.offhandItem?.isEmpty == false,
            offhandBefore = player?.mainArm != HumanoidArm.LEFT,
            hasAttackIndicator = player != null && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR,
        )
    }

    private fun ensurePosition() {
        val viewport = viewport()
        val geometry = geometry()
        if (!positionInitialized) {
            positionInitialized = true
            if (!referencePosition.value) {
                // Match the old native-pixel position/size once, then use the
                // same 960x540 reference coordinates as the rest of the HUD editor.
                if (hud.x != 0f || hud.y != 0f) {
                    hud.x /= viewport.scale
                    hud.y /= viewport.scale
                    hud.scale /= viewport.scale
                }
                referencePosition.value = true
            }
            if (hud.x == 0f && hud.y == 0f) {
                hud.x = ((viewport.width - geometry.width * totalScale()) / 2f).coerceAtLeast(0f)
                hud.y = (viewport.height - geometry.height * totalScale() - 8f).coerceAtLeast(0f)
            }
        }
        val width = geometry.width * totalScale()
        val height = geometry.height * totalScale()
        if (keepOnScreen.value) {
            val left = effectMargin() * totalScale()
            val top = topMargin() * totalScale()
            hud.x = hud.x.coerceIn(left, (viewport.width - width - left).coerceAtLeast(left))
            hud.y = hud.y.coerceIn(top,
                (viewport.height - height - bottomMargin() * totalScale()).coerceAtLeast(top))
        }
        // Hit-testing/editor outlines include offhand and the attack indicator.
        hud.width = geometry.width * totalScale() / hud.scale.coerceAtLeast(.001f)
        hud.height = geometry.height * totalScale() / hud.scale.coerceAtLeast(.001f)
    }

    private fun drawHotbar(ctx: GuiGraphicsExtractor, geometry: HotbarLayout, delta: DeltaTracker, example: Boolean) {
        val player = cameraPlayer()
        val now = System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0f else (now - lastFrame) / 1_000_000_000f
        if (elapsed > .5f) selection.reset()
        lastFrame = now
        val motion = motionEnabled()
        val seconds = (now - effectEpoch) / 1_000_000_000f
        val accent = if (rainbowAccent.value && motion) {
            Color(Color.HSBtoRGB((seconds * .08f * effectSpeed.value) % 1f, .65f, 1f))
        } else accentColor.value
        val look = HotbarPainter.Appearance(
            style = HotbarPainter.Style.entries[style.value.coerceIn(0, 3)],
            background = backgroundColor.value,
            secondary = gradientColor.value,
            accent = accent,
            opacity = backgroundOpacity.value,
            radius = if (style.value == 0) 1f else cornerRadius.value,
            borderWidth = if (style.value == 0) 1f else borderWidth.value,
            gradient = gradient.value,
            gradientVertical = if (gradientDirection.value == 0) geometry.vertical else !geometry.vertical,
            shadow = shadow.value,
            shadowStrength = shadowStrength.value / 100f,
            glow = glow.value,
            glowStrength = glowStrength.value / 100f,
            effectRadius = effectRadius.value,
            shimmer = shimmer.value && motion,
            shimmerSpeed = effectSpeed.value * .15f,
            selectionOpacity = selectionOpacity.value,
            selectionRadius = selectionRadius.value,
        )
        HotbarPainter.background(ctx, geometry, look, seconds)
        val selectedSlot = player?.inventory?.selectedSlot ?: 0
        val animatedSlot = selection.update(selectedSlot, elapsed, slideSpeed.value * AetherOSSettings.animationSpeed.value,
            smoothSelection.value && motion)
        HotbarPainter.selection(ctx, geometry.slot(animatedSlot), look)
        for (i in 0..8) {
            val stack = player?.inventory?.getItem(i) ?: if (example) previewStack(i) else ItemStack.EMPTY
            drawItem(ctx, geometry.slot(i.toFloat()), stack, player, delta, i + 1, motion && !example)
        }
        geometry.offhand?.let { cell ->
            drawItem(ctx, cell, player?.offhandItem ?: ItemStack.EMPTY, player, delta, 10, motion && !example)
        }
        geometry.attackIndicator?.let { cell -> drawAttackIndicator(ctx, cell, accent, look.style) }
    }

    private fun drawItem(ctx: GuiGraphicsExtractor, cell: HotbarBox, stack: ItemStack,
                         player: Player?, delta: DeltaTracker, seed: Int, animate: Boolean) {
        if (stack.isEmpty) return
        val iconSize = min(16f * itemScale.value, cell.width - 4f).coerceAtLeast(1f)
        val pose = ctx.pose()
        pose.pushMatrix()
        try {
            pose.translate(cell.centerX - iconSize / 2f, cell.centerY - iconSize / 2f)
            pose.scale(iconSize / 16f, iconSize / 16f)
            pose.pushMatrix()
            try {
                val pop = if (animate && pickupAnimation.value) stack.popTime - delta.getGameTimeDeltaPartialTick(false) else 0f
                if (pop > 0f) {
                    val factor = 1f + pop / 5f
                    pose.translate(8f, 12f)
                    pose.scale(1f / factor, (factor + 1f) / 2f)
                    pose.translate(-8f, -12f)
                }
                if (player == null) ctx.item(stack, 0, 0, seed)
                else ctx.item(player, stack, 0, 0, seed)
            } finally { pose.popMatrix() }
            // Vanilla adds counts, durability and cooldowns here, outside the
            // temporary pickup-pop matrix, exactly as its extractSlot method does.
            ctx.itemDecorations(mc.font, stack, 0, 0)
        } finally { pose.popMatrix() }
    }

    private fun drawAttackIndicator(ctx: GuiGraphicsExtractor, cell: HotbarBox, accent: Color, style: HotbarPainter.Style) {
        val strength = mc.player?.getAttackStrengthScale(0f)?.coerceIn(0f, 1f) ?: return
        if (style == HotbarPainter.Style.VANILLA) {
            if (strength >= 1f) return
            val x = (cell.centerX - 9f).toInt()
            val y = (cell.centerY - 9f).toInt()
            val progress = (strength * 19f).toInt().coerceIn(0, 18)
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, attackBackground, x, y, 18, 18)
            if (progress > 0) ctx.blitSprite(RenderPipelines.GUI_TEXTURED, attackProgress,
                18, 18, 0, 18 - progress, x, y + 18 - progress, 18, progress)
        } else {
            val track = Rect(cell.centerX - 2f, cell.y + 3f, 4f, cell.height - 6f)
            ctx.rounded(track, 2f, Color(255, 255, 255, 28))
            val fillHeight = track.height * strength
            ctx.rounded(Rect(track.x, track.bottom - fillHeight, track.width, fillHeight), 2f, accent)
        }
    }

    /** Keep the original tooltip and contextual bar logic, including other mods' wrappers. */
    private fun moveVanillaElement(id: Identifier, active: () -> Boolean) {
        HudElementRegistry.replaceElement(id) { original ->
            FabricHudElement { graphics, delta ->
                if (!enabled || !active() || cameraPlayer()?.isSpectator != false) {
                    original.extractRenderState(graphics, delta)
                } else if (id == VanillaHudElements.HELD_ITEM_TOOLTIP && itemNamePosition.value == 1 && mc.gui.screen() is HudEditorScreen) {
                    ensureNamePosition() // Editor preview replaces the native fading label.
                } else if (id == VanillaHudElements.HELD_ITEM_TOOLTIP && itemNamePosition.value == 1) {
                    ensureNamePosition()
                    // Preserve the native fade, rarity/italic styling and backdrop.
                    // Its text baseline is GUI height - 59 (+14 in creative mode).
                    val nativeY = mc.window.guiScaledHeight - 59 +
                        if (mc.gameMode?.canHurtPlayer() == false) 14 else 0
                    val s = nameScale()
                    val pose = graphics.pose()
                    pose.pushMatrix()
                    try {
                        val r = viewport().scale
                        pose.scale(r, r)
                        pose.translate(nameHud.x - s * (mc.window.guiScaledWidth / 2),
                            nameHud.y + 3f * s - s * nativeY)
                        pose.scale(s, s)
                        original.extractRenderState(graphics, delta)
                    } finally { pose.popMatrix() }
                } else {
                    ensurePosition()
                    val viewport = viewport()
                    val core = geometry().mainBounds
                    val s = totalScale()
                    val nameWidth = if (id == VanillaHudElements.HELD_ITEM_TOOLTIP) {
                        cameraPlayer()?.mainHandItem?.hoverName?.let { mc.font.width(it) } ?: 182
                    } else 182
                    val nativeWidth = maxOf(182, nameWidth).toFloat()
                    val followerScale = if (keepOnScreen.value) min(s, viewport.width / nativeWidth) else s
                    val halfWidth = nativeWidth * followerScale * .5f
                    val desiredCenter = hud.x + s * core.centerX
                    val center = if (keepOnScreen.value) desiredCenter.coerceIn(halfWidth,
                        (viewport.width - halfWidth).coerceAtLeast(halfWidth)) else desiredCenter
                    val anchorY = hud.y
                    val vanillaCenter = mc.window.guiScaledWidth / 2
                    val vanillaY = mc.window.guiScaledHeight - 22
                    val pose = graphics.pose()
                    pose.pushMatrix()
                    try {
                        pose.scale(viewport.scale, viewport.scale)
                        pose.translate(center - followerScale * vanillaCenter, anchorY - followerScale * vanillaY)
                        pose.scale(followerScale, followerScale)
                        original.extractRenderState(graphics, delta)
                    } finally { pose.popMatrix() }
                }
            }
        }
    }

    private fun previewStack(index: Int): ItemStack = ItemStack(when (index) {
        0 -> Items.DIAMOND_SWORD
        1 -> Items.BOW
        2 -> Items.DIAMOND_PICKAXE
        3 -> Items.GOLDEN_APPLE
        4 -> Items.ENDER_PEARL
        5 -> Items.TORCH
        6 -> Items.POTION
        7 -> Items.COOKED_BEEF
        else -> Items.NETHER_STAR
    })

    private val attackBackground = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background")
    private val attackProgress = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress")
}
