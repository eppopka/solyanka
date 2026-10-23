/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills Settings, InnerSettingsScreen and KeybindButton.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui.nofrills

import dev.solyanka.utility.InputDevice
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

class SettingsPanel(
    title:String,
    private val back:()->Screen,
    private val primaryText:String,
    private val primary:()->Unit,
    private val content:(SettingsPanel,Int)->List<UIComponent>
):BaseOwoScreen<FlowLayout>(Component.literal(title)) {
    private lateinit var scroll:ScrollContainer<FlowLayout>
    private data class Capture(val button:ButtonComponent,val device:InputDevice,val key:Int,val accept:(InputDevice,Int)->Unit)
    private var capture:Capture?=null
    override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) { w,h -> UIContainers.verticalFlow(w,h) }
    override fun build(root:FlowLayout) {
        root.surface(Surface.VANILLA_TRANSLUCENT);root.alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
        capture=null
        val w=minOf(340,(width-20).coerceAtLeast(150))
        val panel=UIContainers.verticalFlow(Sizing.content(),Sizing.content()).apply { padding(Insets.of(5)) }
        val header=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
            alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);padding(Insets.of(3));surface(Surface.flat(GuiKit.BLUE))
            child(GuiKit.label(title.string).apply { horizontalSizing(Sizing.fill(100));horizontalTextAlignment(HorizontalAlignment.CENTER) })
        }
        val rows=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply { surface(Surface.flat(0xaa000000.toInt())) }
        content(this,w).forEach { rows.child(it) }
        scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fixed((height*0.62).toInt().coerceAtLeast(35)),rows)
        scroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.WHITE)))
        val footer=GuiKit.row(w).apply {
            surface(Surface.flat(0xaa000000.toInt()));child(GuiKit.button(primaryText,90) { primary() })
            child(GuiKit.button("Cancel / back",90,true) { onClose() })
        }
        panel.child(header);panel.child(scroll);panel.child(footer);root.child(panel)
    }
    fun inputRow(width:Int,device:InputDevice,key:Int,updated:(InputDevice,Int)->Unit):FlowLayout {
        var currentDevice=device;var currentKey=key
        val button=GuiKit.button(keyName(device,key),125) {}
        val updateCurrent:(InputDevice,Int)->Unit={ d,k -> currentDevice=d;currentKey=k;updated(d,k) }
        button.onPress { capture=Capture(button,currentDevice,currentKey,updateCurrent);button.message=Component.literal("Press key...") }
        return GuiKit.row(width).apply {
            child(GuiKit.label("Keybind"));child(button)
            child(GuiKit.button("Unbind",60,true) {
                capture=null;button.message=Component.literal("Not Bound");updateCurrent(InputDevice.KEYBOARD,-1)
            })
        }
    }
    private fun keyName(device:InputDevice,key:Int):String {
        if(key<0) return "Not Bound"
        return (if(device==InputDevice.MOUSE) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM)
            .getOrCreate(key).displayName.string
    }
    private fun accept(device:InputDevice,key:Int) {
        val pending=capture ?: return
        capture=null;pending.accept(device,key);pending.button.message=Component.literal(keyName(device,key))
    }
    override fun keyPressed(event:KeyEvent):Boolean {
        capture?.let { pending ->
            when(event.key()) {
                GLFW.GLFW_KEY_ESCAPE -> { pending.button.message=Component.literal(keyName(pending.device,pending.key));capture=null }
                GLFW.GLFW_KEY_BACKSPACE -> accept(InputDevice.KEYBOARD,-1)
                in 32..348 -> accept(InputDevice.KEYBOARD,event.key())
            }
            return true
        }
        if(::scroll.isInitialized && (event.key()==GLFW.GLFW_KEY_PAGE_UP || event.key()==GLFW.GLFW_KEY_PAGE_DOWN)) {
            return scroll.onMouseScroll(0.0,0.0,if(event.key()==GLFW.GLFW_KEY_PAGE_UP) 4.0 else -4.0)
        }
        return super.keyPressed(event)
    }
    override fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
        if(capture!=null && event.button() in 0..7) { accept(InputDevice.MOUSE,event.button());return true }
        return super.mouseClicked(event,doubled)
    }
    override fun dispose() { super.dispose();uiAdapter=null }
    override fun removed() { capture=null;super.removed();dispose() }
    override fun onClose() { capture=null;Minecraft.getInstance().setScreenAndShow(back()) }
}
