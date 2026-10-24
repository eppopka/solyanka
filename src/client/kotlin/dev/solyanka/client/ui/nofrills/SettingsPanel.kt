/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills Settings, InnerSettingsScreen and KeybindButton.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui.nofrills

import dev.solyanka.utility.InputDevice
import dev.solyanka.ui.PanelDrag
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
    private val info:()->String?={null},
    private val content:(SettingsPanel,Int)->List<UIComponent>
):EmbeddedPanel {
    lateinit var adapter:OwoUIAdapter<FlowLayout>;private set
    private lateinit var scroll:ScrollContainer<FlowLayout>
    private lateinit var statusLabel:LabelComponent
    private var lastInfo:String?=null
    private data class Capture(val button:ButtonComponent,val device:InputDevice,val key:Int,val accept:(InputDevice,Int)->Unit)
    private var capture:Capture?=null
    private var blockCharacter=false
    private val drag=PanelDrag()
    private lateinit var frame:FlowLayout
    private lateinit var header:FlowLayout
    private var initialPosition:PanelDrag.Point?=null
    override fun position()=drag.position
    override fun placeAt(point:PanelDrag.Point?) {initialPosition=point}
    private fun reposition() {drag.position?.let {frame.positioning(Positioning.absolute(it.x,it.y))}}
    override fun render(g:net.minecraft.client.gui.GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        val currentInfo=info();if(currentInfo!=lastInfo){lastInfo=currentInfo;refreshStatus()}
        adapter.extractRenderState(g,mx,my,delta);adapter.drawTooltip(g,mx,my,delta)
    }
    override fun mouseDragged(event:MouseButtonEvent,dx:Double,dy:Double):Boolean {
        if(drag.dragging) {drag.move(event.x(),event.y());reposition();return true}
        return adapter.mouseDragged(event,dx,dy)
    }
    override fun mouseReleased(event:MouseButtonEvent):Boolean {
        val moved=drag.end();val handled=adapter.mouseReleased(event);return moved || handled
    }
    override fun mount(width:Int,height:Int) {
        if(::adapter.isInitialized) adapter.dispose()
        capture=null
        adapter=OwoUIAdapter.createWithoutScreen(0,0,width,height) { w,h -> UIContainers.verticalFlow(w,h) }
        val root=adapter.rootComponent
        val outerWidth=minOf(400,(width-24).coerceAtLeast(100))
        val w=(outerWidth-12).coerceAtLeast(1)
        val available=(height-24).coerceAtLeast(64)
        val frameHeight=minOf(420,(height*.80).toInt().coerceAtLeast(100),available)
        frame=UIContainers.verticalFlow(Sizing.fixed(outerWidth),Sizing.content()).apply {
            padding(Insets.of(6));gap(4)
            surface {g,c->
                dev.solyanka.client.feature.ChromeDraw.panel(g,c.x()+3,c.y()+4,c.width(),c.height(),0x50000000,0x10000000,7)
                dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),GuiKit.PANEL,GuiKit.LINE,7)
            }
        }
        header=UIContainers.horizontalFlow(Sizing.fixed(w),Sizing.fixed(28)).apply {
            verticalAlignment(VerticalAlignment.CENTER);padding(Insets.of(4));gap(6)
            surface {g,c->dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),GuiKit.RAISED,GuiKit.LINE,4)}
            child(GuiKit.label(title).apply {horizontalSizing(Sizing.fixed((w-76).coerceAtLeast(1)));tooltip(Component.literal("Drag this title bar to move settings"))})
            child(GuiKit.button("Back",54,true) {capture=null;back()})
        }
        val rows=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {gap(2)}
        content(this,w).forEach {rows.child(it)}
        scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fixed((frameHeight-72).coerceAtLeast(16)),rows)
        scroll.scrollbarThiccness(3).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.BLUE)))
        statusLabel=GuiKit.label("").apply {horizontalSizing(Sizing.fixed((w-10).coerceAtLeast(1)))}
        frame.child(header);frame.child(scroll)
        frame.child(UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {padding(Insets.of(3));child(statusLabel)})
        drag.layout(width,height,outerWidth,frameHeight)
        initialPosition?.let {drag.place(it)};initialPosition=null
        reposition();root.child(frame);refreshStatus();adapter.inflateAndMount()
        drag.layout(width,height,frame.width(),frame.height());reposition()
    }
    override fun focused():net.minecraft.client.gui.components.events.GuiEventListener?=if(::adapter.isInitialized)adapter else null
    override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double)=adapter.mouseScrolled(x,y,horizontal,vertical)
    fun refreshStatus() {
        if(!::statusLabel.isInitialized) return
        val error=status()
        statusLabel.text(Component.literal(if(error==null) info() ?: "" else "Not saved: $error"))
        statusLabel.color(Color.ofArgb(if(error==null) GuiKit.MUTED else 0xffff5555.toInt()))
    }
    override fun dispose() { capture=null;drag.end();if(::adapter.isInitialized) adapter.dispose() }
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
    override fun charTyped(event:CharacterEvent):Boolean = if(capture!=null || blockCharacter) true else adapter.charTyped(event)
    override fun keyPressed(event:KeyEvent):Boolean {
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
    override fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
        if(capture==null && ::header.isInitialized && drag.begin(event.x(),event.y(),event.button(),header.isInBoundingBox(event.x(),event.y()) && event.x()<header.x()+header.width()-68)) {
            adapter.rootComponent.focusHandler()?.focus(null,UIComponent.FocusSource.MOUSE_CLICK);return true
        }
        if(capture!=null && event.button() in 0..7) { accept(InputDevice.MOUSE,event.button());return true }
        adapter.mouseClicked(event,doubled);return true
    }
}
