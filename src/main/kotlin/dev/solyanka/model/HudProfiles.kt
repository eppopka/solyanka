/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.utility.FeatureSettings
import dev.solyanka.utility.SkyblockContext

/** Snapshots hold positions, never module states, commands, or credentials. */
data class HudProfile(val context: String, val layout: HudLayoutSnapshot)

data class HudProfilesSettings(
  val enabled: Boolean = false,
  val profiles: List<HudProfile> = emptyList(),
) {
  fun validate() {
    require(profiles.size <= 64 && profiles.map { it.context }.distinct().size == profiles.size)
    if (profiles.isEmpty()) return
    val legal = HudProfiles.legalKeys
    profiles.forEach { p ->
      require(p.context.isNotBlank() && p.context.length <= 100)
      val entries = p.layout.placements
      require(entries.size <= 64 && entries.map { it.key }.distinct().size == entries.size)
      require(
        entries.all {
          it.key in legal &&
            it.x in 0..10000 &&
            it.y in 0..10000 &&
            it.scale.isFinite() &&
            it.scale in 0.5..4.0
        },
      )
    }
  }
}

object HudProfiles {
  internal val legalKeys: Set<String> by lazy {
    HudLayouts.capture(FeatureSettings()).placements.map { it.key }.toSet()
  }

  fun context(c: SkyblockContext): String? =
    when {
      !c.skyblock -> "default"
      c.dungeon ->
        "dungeon:" + (if (c.floor == null) "other" else (if (c.master) "M" else "F") + c.floor)
      c.area.isNotBlank() -> "area:" + c.area.trim().take(80)
      else -> null // transiently unavailable scoreboard/tab data: do not switch
    }

  /** Save the outgoing layout, then load the destination if one exists. */
  fun transition(f: FeatureSettings, previous: String?, next: String?): FeatureSettings {
    if (previous == next) return f
    var profiles = f.hudProfiles.profiles
    if (previous != null) {
      val entry = HudProfile(previous, HudLayouts.capture(f))
      profiles = profiles.filterNot { it.context == previous } + entry
      require(profiles.size <= 64) { "Maximum 64 HUD profiles" }
    }
    var result = f.copy(hudProfiles = f.hudProfiles.copy(profiles = profiles))
    val target = profiles.firstOrNull { it.context == next }
    if (target != null) result = HudLayouts.apply(result, target.layout)
    result.validate()
    return result
  }
}
