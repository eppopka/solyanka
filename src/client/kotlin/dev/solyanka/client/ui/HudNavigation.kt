/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.ui.nofrills.GuiKit
object HudNavigation {
 fun button(width:Int)=GuiKit.button("Open HUD Editor",width) {net.minecraft.client.Minecraft.getInstance().setScreenAndShow(HudEditorScreen())}
}
