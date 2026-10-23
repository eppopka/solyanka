/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills FlatTextbox, PlainLabel, ToggleButton, Settings and Rendering.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE and docs/GUI_PORT.md. */
package dev.solyanka.client.ui.nofrills

import io.wispforest.owo.ui.component.*
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

object GuiKit {
    const val BLUE = 0xff5ca0bf.toInt()
    const val WHITE = 0xffffffff.toInt()
    const val DARK = 0xff101010.toInt()
    fun border(g: GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,color:Int) {
        g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color)
        g.fill(x,y+1,x+1,y+h-1,color);g.fill(x+w-1,y+1,x+w,y+h-1,color)
    }
    fun button(text:String,width:Int=70,white:Boolean=false,action:()->Unit): ButtonComponent =
        UIComponents.button(Component.literal(text)) { action() }.apply {
            sizing(Sizing.fixed(width),Sizing.fixed(18))
            textShadow(false)
            renderer { g,b,_ ->
                g.fill(b.x,b.y,b.x+b.width,b.y+b.height,DARK)
                border(g,b.x,b.y,b.width,b.height,if(white) WHITE else BLUE)
            }
        }
    fun label(text:String) = PlainLabel(Component.literal(text)).apply { color(Color.ofArgb(WHITE)) }
    fun row(width:Int): FlowLayout = UIContainers.horizontalFlow(Sizing.fixed(width),Sizing.content()).apply {
        padding(Insets.of(5));verticalAlignment(VerticalAlignment.CENTER);gap(5)
    }
    fun textRow(width:Int,name:String,value:String,help:String="",update:(String)->Unit): FlowLayout {
        val box=FlatTextbox(Sizing.fixed(width-18)).apply {
            text(value);tooltip(Component.literal(help));onChanged().subscribe { update(it) }
        }
        return UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
            padding(Insets.of(5));gap(3);child(label(name));child(box)
        }
    }
    fun toggleRow(width:Int,name:String,current:Boolean,help:String="",update:(Boolean)->Unit): FlowLayout = row(width).apply {
        child(label(name).apply { tooltip(Component.literal(help)) })
        child(toggle(current,update))
    }
    fun toggle(initial:Boolean,update:(Boolean)->Unit):ButtonComponent {
        var current=initial
        fun text()=Component.literal(if(current) "Enabled" else "Disabled").withColor(if(current) 0x55ff55 else 0xff5555)
        return button("",58) {}.apply {
            message=text()
            onPress {
                current=!current;message=text();update(current)
            }
        }
    }
    fun description(width:Int,text:String) = UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
        padding(Insets.of(5));child(label(text).apply { horizontalSizing(Sizing.fixed(width-10)) })
    }
}

class PlainLabel(text:Component):LabelComponent(text) {
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean)=false
}

class FlatTextbox(size:Sizing):TextBoxComponent(size) {
    init { verticalSizing(Sizing.fixed(18));setMaxLength(256) }
    override fun extractWidgetRenderState(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int,delta:Float) {
        g.fill(x(),y(),x()+width()+4,y()+height(),GuiKit.DARK)
        GuiKit.border(g,x(),y(),width()+4,height(),GuiKit.BLUE)
        g.scissorStack.push(ScreenRectangle(x(),y(),width(),height()))
        try { super.extractWidgetRenderState(g,mouseX,mouseY,delta) }
        finally { g.scissorStack.pop() }
    }
    override fun isBordered()=false
    override fun getInnerWidth()=getWidth()-8
}
