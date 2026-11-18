/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.ArchiveHuds
import dev.solyanka.client.feature.ExtraHuds
import dev.solyanka.client.feature.ScoreboardHud
import dev.solyanka.client.feature.VanillaHearts
import dev.solyanka.client.feature.VanillaHotbar
import dev.solyanka.client.feature.YandexMusic
import dev.solyanka.model.HudLayout
import dev.solyanka.model.HudPreviewRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Ordered preview painters. A painter returns false when it does not own the key. */
object HudPreviewRegistry {
 private data class Painter(val renderer:HudPreviewRenderer,val draw:(GuiGraphicsExtractor,String,Int,Int)->Boolean)
 private val painters = listOf(
  Painter(HudPreviewRenderer.YANDEX_MUSIC,YandexMusic::editor),
  Painter(HudPreviewRenderer.VANILLA_HOTBAR,VanillaHotbar::editor),
  Painter(HudPreviewRenderer.VANILLA_HEARTS,VanillaHearts::editor),
  Painter(HudPreviewRenderer.ARCHIVE_HUDS,ArchiveHuds::editor),
  Painter(HudPreviewRenderer.EXTRA_HUDS,ExtraHuds::editor),
  Painter(HudPreviewRenderer.SCOREBOARD,ScoreboardHud::editor),
 )
 fun draw(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
  val renderer=HudLayout.previewRenderer(key) ?: return false
  return painters.firstOrNull {it.renderer==renderer}?.draw?.invoke(g,key,x,y) ?: false
 }
}
