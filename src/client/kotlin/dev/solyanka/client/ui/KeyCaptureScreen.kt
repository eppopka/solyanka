/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import dev.solyanka.utility.InputDevice
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class KeyCaptureScreen(private val parent: Screen, private val captured: (InputDevice,Int) -> Unit)
    : Screen(Component.translatable("solyanka.config.capture")) {
    override fun init() {
        addRenderableWidget(StringWidget(10,height/2-40,width-20,20,title,font))
        addRenderableWidget(StringWidget(10,height/2-15,width-20,20,
            Component.translatable("solyanka.config.capture_help"),font))
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }
            .bounds(width/2-60,height/2+20,120,20).build())
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        when (event.key()) {
            GLFW.GLFW_KEY_ESCAPE -> onClose()
            GLFW.GLFW_KEY_BACKSPACE -> finish(InputDevice.KEYBOARD,-1)
            else -> if (event.key() in 32..348) finish(InputDevice.KEYBOARD,event.key())
        }
        return true
    }
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event,doubleClick)) return true
        if (event.button() in 0..7) finish(InputDevice.MOUSE,event.button())
        return true
    }
    private fun finish(device: InputDevice,key: Int) { captured(device,key); onClose() }
    override fun onClose() { Minecraft.getInstance().setScreenAndShow(parent) }
}
