/* SPDX-License-Identifier: GPL-3.0-only; Theme.java classic palette CC0. */
package dev.solyanka.utility
object StrayClassicStyle {
 const val WINDOW:Int=0xff0b0e14.toInt()
 const val LINE:Int=0xff1c2430.toInt()
 const val TEXT:Int=0xfff2f4f7.toInt()
 fun scoreboard(s:ScoreboardHudSettings)=s.copy(hud=s.hud.copy(color=TEXT),backgroundColor=WINDOW,borderColor=LINE)
 fun chat(s:ThemedChatSettings)=s.copy(backgroundColor=WINDOW,borderColor=LINE,inputColor=WINDOW)
}
