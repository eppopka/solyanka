/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

data class ClickGuiSettings(
 val enabledColor:Int=ThemeColors.ACCENT,
 val disabledColor:Int=ThemeColors.DISABLED_TEXT,
 val sortByLength:Boolean=false,
 val disabledBorderColor:Int=ThemeColors.LINE,
 val toggleAnimations:Boolean=true,
 val theme:String="Slate",
 val favorites:List<String> = emptyList()
) {
 fun validate() { require(theme in ThemeColors.palettes.map {it.name});require(favorites.size<=128 && favorites.distinct().size==favorites.size && favorites.all {it.matches(Regex("[a-z][a-z0-9_.-]*"))}) }
}
