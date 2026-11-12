/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.client.feature.ArchiveHuds
import dev.solyanka.client.feature.ExtraHuds
import dev.solyanka.client.feature.ScoreboardHud
import dev.solyanka.client.feature.VanillaHearts
import dev.solyanka.client.feature.VanillaHotbar
import dev.solyanka.client.feature.YandexMusic
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Ordered preview painters. A painter returns false when it does not own the key. */
object HudPreviewRegistry {
 private val painters:List<(GuiGraphicsExtractor,String,Int,Int)->Boolean> = listOf(
  YandexMusic::editor,VanillaHotbar::editor,VanillaHearts::editor,
  ArchiveHuds::editor,ExtraHuds::editor,ScoreboardHud::editor
 )
 fun draw(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean = painters.any {it(g,key,x,y)}
}
