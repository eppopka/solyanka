/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.HudEntry
import dev.solyanka.utility.PerformanceText
import dev.solyanka.utility.ServerTpsEstimator
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier

object PerformanceHud {
  private val tps = ServerTpsEstimator()

  fun reset() = tps.reset()

  fun observe(ticks: Long) {
    tps.observe(ticks, System.nanoTime())
  }

  private fun text(): String {
    val c = Minecraft.getInstance()
    val s = S.currentGeneral().features.performance
    val ping =
      if (c.hasSingleplayerServer()) "Local"
      else
        c.player
          ?.let { c.connection?.getPlayerInfo(it.uuid)?.latency }
          ?.takeIf { it >= 0 }
          ?.let { "$it ms" } ?: "—"
    return PerformanceText.format(s, c.fps, ping, tps.value(System.nanoTime()))
  }

  fun editorEntry(entry: HudEntry): HudEntry =
    if (entry.key == "extra.performance")
      entry.copy(lines = listOf(text().ifEmpty { "No metrics selected" }))
    else entry

  private fun styled(text: String): net.minecraft.network.chat.Component {
    val result = net.minecraft.network.chat.Component.empty()
    text.split(" | ").forEachIndexed { i, part ->
      if (i > 0) result.append(net.minecraft.network.chat.Component.literal(" | "))
      val component = net.minecraft.network.chat.Component.literal(part)
      PerformanceText.metricColor(part)?.let { component.withColor(it) }
      result.append(component)
    }
    return result
  }

  private fun draw(g: GuiGraphicsExtractor) {
    val c = Minecraft.getInstance()
    val s = S.currentGeneral().features.performance
    val h = s.hud
    val text = text()
    if (text.isEmpty()) return
    val component = styled(text)
    val width = c.font.width(component) + 4
    val height = 11
    HudFrame.draw(g, h, width, height, glowModule = ModuleIds.PERFORMANCE, glowRadius = 0) {
      g.text(c.font, component, 2, 1, h.color, true)
    }
  }

  fun register() {
    HudElementRegistry.attachElementBefore(
      VanillaHudElements.CHAT,
      Identifier.fromNamespaceAndPath("solyanka", "performance"),
    ) { g, _ ->
      val c = Minecraft.getInstance()
      val screen = c.gui.screen()
      if (
        S.isEnabled(ModuleIds.PERFORMANCE) &&
          S.currentGeneral().features.performance.hud.enabled &&
          c.level != null &&
          c.player != null &&
          !c.gui.hud.isHidden &&
          screen !is HudEditorScreen &&
          (screen == null ||
            screen is ChatScreen ||
            screen is dev.solyanka.client.ui.nofrills.NoFrillsClickGui)
      )
        draw(g)
    }
  }
}
