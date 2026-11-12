/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class WaypointNameScreen(private val initial: String, private val save: (String) -> Unit) :
  Screen(Component.literal("Waypoint Name")) {
  private lateinit var name: EditBox

  override fun init() {
    val x = width / 2 - 100
    addRenderableWidget(StringWidget(x, height / 2 - 55, 200, 20, title, font))
    name =
      addRenderableWidget(
        EditBox(font, x, height / 2 - 25, 200, 20, Component.literal("Waypoint Name")).apply {
          setMaxLength(128)
          setValue(initial)
        },
      )
    addRenderableWidget(
      Button.builder(Component.literal("Save")) { commit() }
        .bounds(x, height / 2 + 5, 95, 20)
        .build(),
    )
    addRenderableWidget(
      Button.builder(Component.literal("Cancel")) { onClose() }
        .bounds(x + 105, height / 2 + 5, 95, 20)
        .build(),
    )
    setInitialFocus(name)
  }

  private fun commit() {
    val value = name.value
    onClose()
    save(value)
  }

  override fun keyPressed(event: KeyEvent): Boolean {
    if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
      commit()
      return true
    }
    return super.keyPressed(event)
  }

  override fun onClose() {
    Minecraft.getInstance().gui.setScreen(null)
  }

  override fun isPauseScreen() = false
}
