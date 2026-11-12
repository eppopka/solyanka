/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Shared defaults for the slate ClickGUI palette. Saved user colors remain unchanged. */
object ThemeColors {
  const val ACCENT = 0xff7dd3fc.toInt()
  const val TEXT = 0xffedf4fd.toInt()
  const val DARK = 0xff16202e.toInt()
  const val PANEL = 0xf51a2332.toInt()
  const val RAISED = 0xff263448.toInt()
  const val LINE = 0xff435870.toInt()
  const val MUTED = 0xffb2c2d8.toInt()
  const val DISABLED_TEXT = 0xffc3cede.toInt()

  data class Palette(
    val name: String,
    val dark: Int,
    val panel: Int,
    val raised: Int,
    val line: Int,
    val accent: Int,
    val text: Int,
    val muted: Int,
  )

  val palettes =
    listOf(
      Palette("Slate", DARK, PANEL, RAISED, LINE, ACCENT, TEXT, MUTED),
      Palette(
        "Stray Classic",
        0xff0b0e14.toInt(),
        0xf50b0e14.toInt(),
        0xff1a2330.toInt(),
        0xff435870.toInt(),
        0xff7dd3fc.toInt(),
        0xfff2f4f7.toInt(),
        MUTED,
      ),
      Palette(
        "Amber",
        0xff251d14.toInt(),
        0xf52d2419.toInt(),
        0xff423222.toInt(),
        0xff75583b.toInt(),
        0xffffc568.toInt(),
        0xfffff2df.toInt(),
        0xffd7c3a8.toInt(),
      ),
      Palette(
        "Mono",
        0xff181818.toInt(),
        0xf5232323.toInt(),
        0xff333333.toInt(),
        0xff626262.toInt(),
        0xffdddddd.toInt(),
        0xfff3f3f3.toInt(),
        0xffb8b8b8.toInt(),
      ),
    )

  fun palette(name: String) = palettes.firstOrNull { it.name == name } ?: palettes.first()

  /**
   * Explicitly selecting a preset changes shared accents and HUD colors; positions remain
   * untouched.
   */
  fun apply(f: FeatureSettings, name: String): FeatureSettings {
    val p = palette(name)
    return f.copy(
      clickGui =
        f.clickGui.copy(
          theme = p.name,
          enabledColor = p.accent,
          disabledColor = p.muted,
          disabledBorderColor = p.line,
        ),
      hudLighting = f.hudLighting.copy(color = p.accent),
      scoreboardHud =
        f.scoreboardHud.copy(
          backgroundColor = p.panel,
          borderColor = p.line,
          accentColor = p.accent,
        ),
      themedChat =
        f.themedChat.copy(backgroundColor = p.panel, borderColor = p.line, inputColor = p.dark),
      inventoryHud = f.inventoryHud.copy(backgroundColor = p.panel),
      itemNameHud = f.itemNameHud.copy(backgroundColor = p.panel),
      skillHud = f.skillHud.copy(backgroundColor = p.panel),
    )
  }
}
