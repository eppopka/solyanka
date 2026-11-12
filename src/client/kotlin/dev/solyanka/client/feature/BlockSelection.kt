/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 selection contour adaptation. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import net.fabricmc.fabric.api.client.rendering.v1.level.*
import net.minecraft.client.Minecraft

object BlockSelection {
  private fun enabled() =
    S.isEnabled(ModuleIds.BLOCK_OUTLINE) && !Minecraft.getInstance().gui.hud.isHidden

  fun register() {
    LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register {
      _: LevelRenderContext,
      outline: net.minecraft.client.renderer.state.level.BlockOutlineRenderState? ->
      !enabled() || outline == null || outline.shape().isEmpty
    }
  }

  fun collect(c: LevelRenderContext, fills: MutableList<WorldOverlay.Fill>) {
    if (!enabled()) return
    val outline = c.levelState().blockOutlineRenderState ?: return
    val shape = outline.shape()
    if (shape.isEmpty) return
    val s = S.currentGeneral().features.worldVisuals.blockOutline
    if (s.filled)
      shape.toAabbs().take(128).forEach {
        fills.add(
          WorldOverlay.Fill(it.move(outline.pos()).inflate(0.001), s.fillColor, !s.throughWalls),
        )
      }
  }

  /**
   * The camera render state is already interpolated for this exact render frame. Match vanilla's
   * shape-local transform: subtract once, in double precision.
   */
  fun submit(c: LevelRenderContext) {
    if (!enabled()) return
    val state = c.levelState()
    val outline = state.blockOutlineRenderState ?: return
    val shape = outline.shape()
    if (shape.isEmpty) return
    val camera = state.cameraRenderState.pos
    val pos = outline.pos()
    val s = S.currentGeneral().features.worldVisuals.blockOutline
    val pose = c.poseStack()
    pose.pushPose()
    try {
      pose.translate(
        pos.x.toDouble() - camera.x,
        pos.y.toDouble() - camera.y,
        pos.z.toDouble() - camera.z,
      )
      val type = WorldOverlay.lineType(!s.throughWalls)
      if (s.glow)
        dev.solyanka.utility.VisualChrome.halo(s.color, s.width, s.glowRadius, s.glowStrength)
          .forEach { (width, color) ->
            c.submitNodeCollector()
              .submitShapeOutline(pose, shape, type, color, width, outline.isTranslucent())
          }
      c.submitNodeCollector()
        .submitShapeOutline(pose, shape, type, s.color, s.width.toFloat(), outline.isTranslucent())
    } finally {
      pose.popPose()
    }
  }
}
