/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import com.mojang.brigadier.arguments.StringArgumentType
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.feature.DungeonWaypoints
import dev.solyanka.client.feature.HeldItemCosmetic
import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.PuzzleSolverModules
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/** Client-only commands. Screens are requested for the next tick to preserve chat behavior. */
object Commands {
  fun register() {
    ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
      dispatcher.register(
        literal("solyanka")
          .executes {
            // Open next tick, after vanilla has finished closing the chat screen.
            S.requestSettings()
            1
          }
          .then(
            literal("config").executes {
              // Defer opening until after chat submission has finished closing ChatScreen.
              S.requestSettings()
              1
            },
          )
          .then(
            literal("gui").executes {
              S.requestHud()
              1
            },
          )
          .then(
            literal("layout")
              .then(
                literal("copy").executes { ctx ->
                  Minecraft.getInstance().keyboardHandler.clipboard =
                    dev.solyanka.model.HudLayouts.encode(S.currentGeneral().features)
                  ctx.source.sendFeedback(
                    Component.literal("HUD layout and theme copied to clipboard."),
                  )
                  1
                },
              )
              .then(
                literal("paste").executes { ctx ->
                  try {
                    val next =
                      dev.solyanka.model.HudLayouts.decodeAndApply(
                        S.currentGeneral().features,
                        Minecraft.getInstance().keyboardHandler.clipboard,
                      )
                    val failure =
                      S.applySettings(
                        S.currentProfile(),
                        S.states(),
                        S.currentGeneral().copy(features = next),
                      )
                    if (failure != null) {
                      ctx.source.sendError(Component.literal(failure))
                      0
                    } else {
                      ctx.source.sendFeedback(Component.literal("HUD layout and theme imported."))
                      1
                    }
                  } catch (e: Exception) {
                    ctx.source.sendError(
                      Component.literal("Invalid or unsupported layout share string."),
                    )
                    0
                  }
                },
              ),
          )
          .then(
            literal("waypoint")
              .then(
                literal("add")
                  .executes { ctx ->
                    try {
                      DungeonWaypoints.addTarget()
                      ctx.source.sendFeedback(Component.literal("Waypoint saved"))
                      1
                    } catch (e: Exception) {
                      ctx.source.sendError(Component.literal(e.message ?: "Cannot add waypoint"))
                      0
                    }
                  }
                  .then(
                    argument("title", StringArgumentType.greedyString()).executes { ctx ->
                      try {
                        DungeonWaypoints.addTarget(StringArgumentType.getString(ctx, "title"))
                        ctx.source.sendFeedback(Component.literal("Waypoint saved"))
                        1
                      } catch (e: Exception) {
                        ctx.source.sendError(Component.literal(e.message ?: "Cannot add waypoint"))
                        0
                      }
                    },
                  ),
              ),
          )
          .then(
            literal("knifestatus").executes { ctx ->
              ctx.source.sendFeedback(Component.literal(dev.solyanka.client.cs2.Cs2Knives.status()))
              1
            },
          )
          .then(
            literal("helditemstatus").executes { ctx ->
              ctx.source.sendFeedback(Component.literal(HeldItemCosmetic.diagnostics()))
              1
            },
          )
          .then(
            literal("status").executes { ctx ->
              S.states().forEach { (id, enabled) ->
                ctx.source.sendFeedback(Component.literal("$id: ${if (enabled) "on" else "off"}"))
              }
              ctx.source.sendFeedback(
                Component.literal(
                  "${S.currentProfile().binds.size} binds, ${S.currentProfile().shortcuts.size} shortcuts. Other ports: pending.",
                ),
              )
              1
            },
          )
          .then(
            literal("toggle")
              .then(
                argument("module", StringArgumentType.word()).executes { ctx ->
                  val id = StringArgumentType.getString(ctx, "module")
                  if (!S.states().containsKey(id)) {
                    ctx.source.sendError(Component.literal("Unknown or unimplemented module: $id"))
                    0
                  } else {
                    val changed =
                      S.states().toMutableMap().also { it[id] = !(S.states()[id] ?: false) }
                    val failure = S.applySettings(S.currentProfile(), changed)
                    if (failure != null) {
                      ctx.source.sendError(Component.literal(failure))
                      0
                    } else {
                      ctx.source.sendFeedback(
                        Component.literal(
                          "$id: ${if (S.states()[id]==true) "on" else "off"}${if(id in PuzzleSolverModules.children && !S.isEnabled(ModuleIds.PUZZLE_SOLVERS)) " (Puzzle Solvers off)" else ""}",
                        ),
                      )
                      1
                    }
                  }
                },
              ),
          ),
      )
    }
  }
}
