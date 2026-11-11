/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

data class ClickGuiSettings(
 val enabledColor:Int=0xff7dd3fc.toInt(),
 val disabledColor:Int=0xffc3cede.toInt(),
 val sortByLength:Boolean=false,
 val disabledBorderColor:Int=0xff435870.toInt(),
 val toggleAnimations:Boolean=true
)
