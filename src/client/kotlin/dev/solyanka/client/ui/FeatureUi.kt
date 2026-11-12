/* SPDX-License-Identifier: GPL-3.0-only
 * solyanka instant-save embedded editors for Odin/NoFrills-derived settings. */
package dev.solyanka.client.ui

import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.InstantSettings
import dev.solyanka.model.SettingsNumbers
import dev.solyanka.utility.*
import io.wispforest.owo.ui.core.UIComponent

object FeatureUi {
  private class View(val host: NoFrillsClickGui, val model: InstantSettings) {
    val errors = linkedMapOf<String, String>()

    fun error() = errors.values.firstOrNull() ?: model.error

    fun change(panel: SettingsPanel, block: (FeatureSettings) -> FeatureSettings) {
      model.updateGeneral { it.copy(features = block(it.features)) }
      panel.refreshStatus()
    }

    fun number(
      panel: SettingsPanel,
      w: Int,
      key: String,
      label: String,
      initial: String,
      help: String = "",
      apply: (FeatureSettings, Double) -> FeatureSettings,
    ): UIComponent =
      GuiKit.sliderRow(
        w,
        label,
        initial.toDouble(),
        SettingsNumbers.range(
          if (key == "cursor.expiry") "fixes" else "hud",
          if (key == "cursor.expiry") "cursorClearTicks" else key.substringAfterLast('.'),
          host.width,
          host.height,
          initial.toDouble(),
        ),
        help,
      ) { number ->
        try {
          val candidate = apply(model.general.features, number)
          candidate.validate()
          errors.remove(key)
          change(panel) { candidate }
        } catch (e: IllegalArgumentException) {
          errors[key] = e.message ?: "Invalid $label"
          panel.refreshStatus()
        }
      }
  }

  private val routes: Map<String, (String, NoFrillsClickGui, InstantSettings) -> Unit> = buildMap {
    fun route(vararg ids: String, handler: (String, NoFrillsClickGui, InstantSettings) -> Unit) {
      ids.forEach { id -> check(put(id, handler) == null) { "Duplicate settings route: $id" } }
    }
    route(ModuleIds.ITEM_AURA, ModuleIds.CS2_KNIFE) { id, host, model ->
      FinalCosmeticsUi.open(id, host, model)
    }
    route(
      ModuleIds.BLOCKLIST,
      ModuleIds.MOVE_HOTBAR,
      ModuleIds.PIP,
      ModuleIds.PET,
      ModuleIds.SIMON,
      ModuleIds.BESTIARY,
      ModuleIds.INVISIBUG,
      ModuleIds.TIKI,
      ModuleIds.CHINA_HAT,
      ModuleIds.HALO,
    ) { id, host, model ->
      HuntingUi.open(id, host, model)
    }
    route(ModuleIds.SPREAD_GUESSER) { _, host, model -> spread(host, model) }
    route(ModuleIds.MUSIC) { _, host, model -> MusicUi.open(host, model) }
    route(ModuleIds.CHAT_COMMANDS) { _, host, model -> ChatCommandsUi.open(host, model) }
    route(ModuleIds.LEAP_MESSAGE) { _, host, model -> LeapMessageUi.open(host, model) }
    route(ModuleIds.SECRET_COUNTER) { _, host, model -> ArchiveDungeonUi.open(host, model) }
    route(ModuleIds.AMBIENT_PARTICLES, ModuleIds.ROTATING_ITEM, ModuleIds.MOVE_HEARTS) {
      id,
      host,
      model ->
      ArchiveVisualUi.open(id, host, model)
    }
    route(ModuleIds.MOTION_BLUR, ModuleIds.STOPWATCH, ModuleIds.DVD) { id, host, model ->
      ReleaseOneUi.open(id, host, model)
    }
    route(ModuleIds.SKYBOX) { _, host, model -> SkyboxUi.open(host, model) }
    route(ModuleIds.HELD_ITEM) { _, host, model -> HeldItemUi.open(host, model) }
    route(ModuleIds.SCOREBOARD, ModuleIds.THEMED_CHAT) { id, host, model ->
      HudStyleUi.open(id, host, model)
    }
    route(ModuleIds.PLAYER_GLOW, ModuleIds.NAMETAGS, ModuleIds.BLOCK_OUTLINE, ModuleIds.TITANIUM) {
      id,
      host,
      model ->
      WorldVisualUi.open(id, host, model)
    }
    route(ModuleIds.INVENTORY_HUD, ModuleIds.ITEM_NAME_HUD, ModuleIds.SKILL_HUD) { id, host, model
      ->
      ExtraHudUi.open(id, host, model)
    }
    route(ModuleIds.PUZZLE_SOLVERS) { _, host, model -> PuzzleSolversUi.open(host, model) }
    route(ModuleIds.WAYPOINTS) { _, host, model -> WaypointUi.open(host, model) }
    route(
      ModuleIds.AMBIENCE,
      ModuleIds.ASPECT,
      ModuleIds.FOG,
      ModuleIds.DOORS,
      ModuleIds.MAP_INFO,
      ModuleIds.ICE,
      ModuleIds.BLAZE,
      ModuleIds.BEAMS,
      ModuleIds.MIMIC,
      ModuleIds.MIMIC_CHEST,
      ModuleIds.PRICES,
      ModuleIds.EXPERIMENTS,
      ModuleIds.SUPPRESSION,
    ) { id, host, model ->
      AdvancedUi.open(id, host, model)
    }
    route(ModuleIds.PERFORMANCE) { _, host, model -> PerformanceUi.open(host, model) }
    route(ModuleIds.TICK_TIMERS) { _, host, model -> timers(View(host, model)) }
    route(ModuleIds.PLAYER_DISPLAY) { _, host, model -> player(View(host, model)) }
    route(
      ModuleIds.ANIMATION,
      ModuleIds.MIDDLE_FIX,
      ModuleIds.MIDDLE_OVERRIDE,
      ModuleIds.CURSOR,
      ModuleIds.LOADING,
    ) { id, host, model ->
      fixes(View(host, model), id)
    }
  }

  fun open(id: String, host: NoFrillsClickGui, model: InstantSettings) {
    val handler = routes[id] ?: error("No settings panel registered for $id")
    handler(id, host, model)
  }

  private fun spread(host: NoFrillsClickGui, model: InstantSettings) {
    host.showPanel(
      SettingsPanel("Spread Guesser", { host.closePanel() }, { model.error }) { panel, w ->
        val s = model.general.features.spreadGuesser
        fun change(block: (SpreadGuesserSettings) -> SpreadGuesserSettings) {
          model.updateGeneral {
            it.copy(features = it.features.copy(spreadGuesser = block(it.features.spreadGuesser)))
          }
          panel.refreshStatus()
        }
        listOf(
          GuiKit.description(
            w,
            "Add Mining Spread to the Stats widget in /widgets. Hints mark nearby matching or grouped blocks; server targets are not guaranteed. Gemstone Spread is separate.",
          ),
          GuiKit.description(
            w,
            "Mining Spread: " +
              (dev.solyanka.client.feature.SpreadGuesser.spread?.toString()
                ?: "unavailable in tab"),
          ),
          GuiKit.colorRow(w, "Outline Color", s.color) { color ->
            change { it.copy(color = color) }
          },
          GuiKit.toggleRow(w, "Filled Overlay", s.filled) { filled ->
            change { it.copy(filled = filled) }
          },
          GuiKit.colorRow(w, "Fill Color", s.fillColor) { color ->
            change { it.copy(fillColor = color) }
          },
        )
      },
    )
  }

  private fun whole(n: Double, label: String): Int {
    require(n % 1.0 == 0.0 && n in 0.0..10000.0) { "$label must be a whole number (0–10000)" }
    return n.toInt()
  }

  private fun timers(v: View) {
    v.host.showPanel(
      SettingsPanel("Tick Timers — Odin", { v.host.closePanel() }, { v.error() }) { panel, w ->
        val s = v.model.general.features.timers
        fun update(change: (TimerSettings) -> TimerSettings) =
          v.change(panel) { it.copy(timers = change(it.timers)) }
        buildList {
          add(
            GuiKit.description(
              w,
              "All six Odin timers use nonzero server pings, not client ticks. Needs a recognized dungeon floor/boss arena.",
            ),
          )
          add(
            GuiKit.toggleRow(w, "Display in Ticks", s.ticks) { value ->
              update { it.copy(ticks = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Display Symbol", s.symbols) { value ->
              update { it.copy(symbols = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Show Prefix", s.prefix) { value ->
              update { it.copy(prefix = value) }
            },
          )
          add(
            GuiKit.toggleRow(
              w,
              "Goldor Start Timer",
              s.startTimer,
              "64 server ticks after Storm's closing line.",
            ) { value ->
              update { it.copy(startTimer = value) }
            },
          )
          s.huds().forEach { (id, h) ->
            add(
              GuiKit.hudRow(
                w,
                timerName(id),
                h.enabled,
                "HUD Editor",
                100,
                { value ->
                  update { current ->
                    current.withHud(id, current.huds().getValue(id).copy(enabled = value))
                  }
                },
                {
                  net.minecraft.client.Minecraft.getInstance().setScreenAndShow(HudEditorScreen())
                },
              ),
            )
          }
          addAll(HudGlowUi.controls(ModuleIds.TICK_TIMERS, v.model, panel, w))
        }
      },
    )
  }

  private fun player(v: View) {
    v.host.showPanel(
      SettingsPanel("Player Display — Odin", { v.host.closePanel() }, { v.error() }) { panel, w ->
        val s = v.model.general.features.player
        fun update(change: (PlayerDisplaySettings) -> PlayerDisplaySettings) =
          v.change(panel) { it.copy(player = change(it.player)) }
        buildList {
          add(
            GuiKit.description(
              w,
              "SkyBlock sidebar required. Seven independent HUDs with position/scale/color. Action bar data is parsed before selected fields are hidden.",
            ),
          )
          add(
            GuiKit.toggleRow(w, "Hide Armor Bar", s.hideArmor) { value ->
              update { it.copy(hideArmor = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Food Bar", s.hideFood) { value ->
              update { it.copy(hideFood = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Hearts", s.hideHearts) { value ->
              update { it.copy(hideHearts = value) }
            },
          )
          add(
            GuiKit.toggleRow(
              w,
              "Hide XP Level",
              s.hideXp,
              "Hides the level number. XP Bar has a separate switch.",
            ) { value ->
              update { it.copy(hideXp = value) }
            },
          )
          add(
            GuiKit.toggleRow(
              w,
              "Hide XP Bar",
              s.hideXpBar,
              "XP only; preserves mount jump and locator bars. SkyBlock only.",
            ) { value ->
              update { it.copy(hideXpBar = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Action Health", s.hideHealth) { value ->
              update { it.copy(hideHealth = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Action Mana", s.hideMana) { value ->
              update { it.copy(hideMana = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Action Overflow", s.hideOverflow) { value ->
              update { it.copy(hideOverflow = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Action Defense", s.hideDefense) { value ->
              update { it.copy(hideDefense = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Action Vitality", s.hideVitality) { value ->
              update { it.copy(hideVitality = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Separate Overflow", s.separateOverflow) { value ->
              update { it.copy(separateOverflow = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Hide Zero Overflow", s.hideZeroOverflow) { value ->
              update { it.copy(hideZeroOverflow = value) }
            },
          )
          add(
            GuiKit.toggleRow(w, "Show Icons", s.icons) { value ->
              update { it.copy(icons = value) }
            },
          )
          s.huds().forEach { (id, h) ->
            add(
              GuiKit.hudRow(
                w,
                id.replaceFirstChar { it.uppercase() },
                h.enabled,
                "Layout / Color",
                110,
                { value ->
                  update { current ->
                    current.withHud(id, current.huds().getValue(id).copy(enabled = value))
                  }
                },
                { hud(v, false, id) },
              ),
            )
          }
          addAll(HudGlowUi.controls(ModuleIds.PLAYER_DISPLAY, v.model, panel, w))
        }
      },
    )
  }

  private fun hud(v: View, timer: Boolean, id: String) {
    val title = if (timer) timerName(id) else id.replaceFirstChar { it.uppercase() } + " HUD"
    v.host.showPanel(
      SettingsPanel(title, { if (timer) timers(v) else player(v) }, { v.error() }) { panel, w ->
        val h =
          if (timer) v.model.general.features.timers.huds().getValue(id)
          else v.model.general.features.player.huds().getValue(id)
        fun apply(f: FeatureSettings, block: (HudSpec) -> HudSpec) =
          if (timer) f.copy(timers = f.timers.withHud(id, block(f.timers.huds().getValue(id))))
          else f.copy(player = f.player.withHud(id, block(f.player.huds().getValue(id))))
        val key = (if (timer) "timer" else "player") + ".$id"
        buildList {
          add(
            GuiKit.description(
              w,
              "Drag and resize enabled HUDs with /solyanka gui. Color saves instantly.",
            ),
          )
          add(HudNavigation.button(w))
          if (!timer)
            add(
              GuiKit.colorRow(w, "Color", h.color) { color ->
                v.change(panel) { f -> apply(f) { it.copy(color = color) } }
              },
            )
        }
      },
    )
  }

  private fun fixes(v: View, id: String) {
    val title =
      when (id) {
        ModuleIds.ANIMATION -> "Animation Fix"
        ModuleIds.MIDDLE_FIX -> "Middle Click Fix"
        ModuleIds.MIDDLE_OVERRIDE -> "Middle Click Override"
        ModuleIds.CURSOR -> "No Cursor Reset"
        else -> "No Loading Screen"
      }
    v.host.showPanel(
      SettingsPanel(title, { v.host.closePanel() }, { v.error() }) { panel, w ->
        val f = v.model.general.features.fixes
        fun update(block: (UtilityFixSettings) -> UtilityFixSettings) =
          v.change(panel) { it.copy(fixes = block(it.fixes)) }
        when (id) {
          ModuleIds.ANIMATION ->
            listOf(
              GuiKit.description(
                w,
                "Filters the first server POSE metadata entry for your local player only; preserves every other metadata entry and other players. No outgoing packet automation.",
              ),
              GuiKit.toggleRow(w, "SkyBlock Only", f.animationSkyblockOnly) { value ->
                update { it.copy(animationSkyblockOnly = value) }
              },
            )
          ModuleIds.MIDDLE_FIX ->
            listOf(
              GuiKit.description(
                w,
                "Allows the configured pick-item click to reach vanilla's CLONE path in survival container screens. Does not grant creative items. Turn the feature card off to restore vanilla behavior.",
              ),
            )
          ModuleIds.MIDDLE_OVERRIDE ->
            listOf(
              GuiKit.description(
                w,
                "Only eligible manual left PICKUP clicks in SkyBlock chest menus become middle CLONE. Vanilla inventories, shift-clicks, carried items, transactions in blacklisted menus and loadout editing are protected. Review server rules; test harmless menus first.",
              ),
              GuiKit.toggleRow(w, "Debug Feedback", f.middleDebug) { value ->
                update { it.copy(middleDebug = value) }
              },
            )
          ModuleIds.CURSOR ->
            listOf(
              GuiKit.description(
                w,
                "Remembers the cursor when returning to gameplay and restores it on GUI reopen. Expiry counts only in-world ticks with no GUI. World changes/disable clear it; Spirit Leap remains centered.",
              ),
              v.number(
                panel,
                w,
                "cursor.expiry",
                "Clear After Ticks",
                f.cursorClearTicks.toString(),
                "0–1200; 0 retains until reset.",
              ) { features, n ->
                features.copy(
                  fixes = features.fixes.copy(cursorClearTicks = whole(n, "Clear After Ticks")),
                )
              },
            )
          else ->
            listOf(
              GuiKit.description(
                w,
                "Safety adaptation: hides LevelLoadingScreen foreground/background only. Vanilla loading ticks, readiness and input blocking remain intact. Does not hide disconnect/error/resource-pack screens or accelerate loading.",
              ),
              GuiKit.toggleRow(
                w,
                "Remote Server Only",
                f.loadingServerOnly,
                "Excludes singleplayer and LAN when enabled.",
              ) { value ->
                update { it.copy(loadingServerOnly = value) }
              },
            )
        }
      },
    )
  }

  private fun timerName(id: String) =
    when (id) {
      "goldor" -> "Goldor"
      "pad" -> "Storm Pad"
      "lightning" -> "Storm Lightning"
      "py" -> "Storm PY"
      "storm" -> "Storm Phase"
      else -> "M3 Fire Freeze"
    }
}
