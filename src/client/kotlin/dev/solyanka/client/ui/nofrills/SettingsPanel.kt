/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills Settings, InnerSettingsScreen and KeybindButton.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui.nofrills

import dev.solyanka.utility.InputDevice
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

/** An embedded UI tree, never a Minecraft Screen. The owner routes all modal input here. */
class SettingsPanel(
    private val title:String,
    private val back:()->Unit,
    private val status:()->String?,
    private val content:(SettingsPanel,Int)->List<UIComponent>
) {
    lateinit var adapter:OwoUIAdapter<FlowLayout>;private set
    private lateinit var scroll:ScrollContainer<FlowLayout>
    private lateinit var statusLabel:LabelComponent
    private data class Capture(val button:ButtonComponent,val device:InputDevice,val key:Int,val accept:(InputDevice,Int)->Unit)
    private var capture:Capture?=null
    private var blockCharacter=false
    fun mount(width:Int,height:Int) {
        if(::adapter.isInitialized) adapter.dispose()
        capture=null
        adapter=OwoUIAdapter.createWithoutScreen(0,0,width,height) { w,h -> UIContainers.verticalFlow(w,h) }
        val root=adapter.rootComponent
        root.alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
        val w=minOf(340,(width-20).coerceAtLeast(150))
        val panel=UIContainers.verticalFlow(Sizing.content(),Sizing.content()).apply { padding(Insets.of(5));surface(Surface.flat(GuiKit.DARK)) }
        val header=UIContainers.horizontalFlow(Sizing.fixed(w),Sizing.content()).apply {
            verticalAlignment(VerticalAlignment.CENTER);padding(Insets.of(3));gap(5);surface(Surface.flat(GuiKit.BLUE))
            child(GuiKit.label(title).apply { horizontalSizing(Sizing.fixed(w-65)) })
            child(GuiKit.button("Back",50,true) { capture=null;back() })
        }
        val rows=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content())
        content(this,w).forEach { rows.child(it) }
        scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fixed((height*0.60).toInt().coerceAtLeast(35)),rows)
        scroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.WHITE)))
        statusLabel=GuiKit.label("").apply { horizontalSizing(Sizing.fixed(w-10)) }
        panel.child(header);panel.child(scroll)
        panel.child(UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply { padding(Insets.of(5));child(statusLabel) })
        root.child(panel);refreshStatus();adapter.inflateAndMount()
    }
    fun refreshStatus() {
        if(!::statusLabel.isInitialized) return
        val error=status()
        statusLabel.text(Component.literal(if(error==null) "" else "Not saved: $error"))
        statusLabel.color(Color.ofArgb(if(error==null) 0xff55ff55.toInt() else 0xffff5555.toInt()))
    }
    fun dispose() { capture=null;if(::adapter.isInitialized) adapter.dispose() }
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
    fun charTyped(event:CharacterEvent):Boolean = if(capture!=null || blockCharacter) true else adapter.charTyped(event)
    fun keyPressed(event:KeyEvent):Boolean {
        blockCharacter=false
        capture?.let { pending ->
            blockCharacter=true
            when(event.key()) {
                GLFW.GLFW_KEY_ESCAPE -> { pending.button.message=Component.literal(keyName(pending.device,pending.key));capture=null }
                GLFW.GLFW_KEY_BACKSPACE -> accept(InputDevice.KEYBOARD,-1)
                in 32..348 -> accept(InputDevice.KEYBOARD,event.key())
            }
            return true
        }
        if(event.key()==GLFW.GLFW_KEY_ESCAPE) { back();return true }
        if(::scroll.isInitialized && (event.key()==GLFW.GLFW_KEY_PAGE_UP || event.key()==GLFW.GLFW_KEY_PAGE_DOWN)) {
            return scroll.onMouseScroll(0.0,0.0,if(event.key()==GLFW.GLFW_KEY_PAGE_UP) 4.0 else -4.0)
        }
        return adapter.keyPressed(event)
    }
    fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
        if(capture!=null && event.button() in 0..7) { accept(InputDevice.MOUSE,event.button());return true }
        adapter.mouseClicked(event,doubled);return true
    }
}
