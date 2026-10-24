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
import dev.solyanka.ui.NumberRange

object GuiKit {
    const val BLUE = 0xff5ca0bf.toInt()
    const val WHITE = 0xffffffff.toInt()
    const val DARK = 0xff101010.toInt()
    fun border(g: GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,color:Int) {
        g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color)
        g.fill(x,y+1,x+1,y+h-1,color);g.fill(x+w-1,y+1,x+w,y+h-1,color)
    }
    fun button(text:String,width:Int=70,white:Boolean=false,action:()->Unit): ButtonComponent =
        styleButton(UIComponents.button(Component.literal(text)) { action() },width,white)
    private fun styleButton(button:ButtonComponent,width:Int,white:Boolean=false):ButtonComponent = button.apply {
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
    /** NoFrills-style color swatch and owo palette, embedded in the current panel. */
    fun colorRow(width:Int,name:String,current:Int,update:(Int)->Unit):FlowLayout {
        val container=UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply { padding(Insets.of(5));gap(4) }
        val swatch=UIComponents.box(Sizing.fixed(16),Sizing.fixed(16)).color(Color.ofArgb(current)).fill(true)
        val picker=ColorPickerComponent().showAlpha(true).apply {
            sizing(Sizing.fixed(minOf(180,width-20)),Sizing.fixed(110))
            selectedColor(Color.ofArgb(current))
        }
        picker.onChanged().subscribe { color -> swatch.color(color);update(color.argb()) }
        var expanded=false
        val edit=button("Edit color",95) {}
        edit.onPress {
            expanded=!expanded
            edit.message=Component.literal(if(expanded) "Close palette" else "Edit color")
            if(expanded) container.child(picker) else container.removeChild(picker)
        }
        container.child(label(name).apply { horizontalSizing(Sizing.fixed(width-10)) })
        container.child(UIContainers.horizontalFlow(Sizing.fixed(width-10),Sizing.content()).apply {
            verticalAlignment(VerticalAlignment.CENTER);gap(5);child(swatch);child(edit)
        })
        return container
    }
    fun choiceRow(width:Int,name:String,choices:List<String>,initial:Int,readCurrent:(()->Int)?=null,update:(Int)->Unit):FlowLayout {
        require(choices.isNotEmpty())
        var selected=initial.coerceIn(choices.indices)
        lateinit var choice:ButtonComponent
        choice=styleButton(DirectionalButton(choices[selected]) { direction ->
            selected=dev.solyanka.ui.SettingsCatalog.nextChoice(readCurrent?.invoke() ?: selected,choices.size,direction)
            choice.message=Component.literal(choices[selected]);update(selected)
        },width-10)
        choice.tooltip(Component.literal("Left click: next | Right click: previous"))
        return UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
            padding(Insets.of(5));gap(3);child(label(name));child(choice)
        }
    }
    fun sliderRow(width:Int,name:String,current:Double,range:NumberRange,help:String="",update:(Double)->Unit):FlowLayout {
        val caption=label("$name: ${range.label(current)}")
        val slider=SlimSliderComponent(SlimSliderComponent.Axis.HORIZONTAL).apply {
            sizing(Sizing.fixed(width-14),Sizing.fixed(12))
            min(range.min);max(range.max);stepSize(range.step);value(current)
            tooltipSupplier { Component.literal(range.label(it)) }
        }
        var last=current
        slider.onChanged().subscribe { raw ->
            val value=range.snap(raw)
            caption.text(Component.literal("$name: ${range.label(value)}"))
            if(value!=last) {last=value;update(value)}
        }
        return UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
            padding(Insets.of(3));gap(3)
            child(caption.apply { horizontalSizing(Sizing.fixed(width-10));tooltip(Component.literal(help)) })
            child(slider)
        }
    }
    /** One compact line where space permits; narrow screens use a tightly stacked row. */
    fun hudRow(width:Int,name:String,current:Boolean,editLabel:String,editWidth:Int,update:(Boolean)->Unit,edit:()->Unit):FlowLayout {
        val label=label(name)
        val needed=net.minecraft.client.Minecraft.getInstance().font.width(name)+58+editWidth+25
        val compact=needed<=width
        return (if(compact) UIContainers.horizontalFlow(Sizing.fixed(width),Sizing.content())
            else UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content())).apply {
            padding(Insets.of(3));gap(3);verticalAlignment(VerticalAlignment.CENTER)
            if(compact) {child(label);child(toggle(current,update));child(button(editLabel,editWidth,action=edit))}
            else {
                child(UIContainers.horizontalFlow(Sizing.fixed(width-6),Sizing.content()).apply {gap(4);verticalAlignment(VerticalAlignment.CENTER);child(label);child(toggle(current,update))})
                child(button(editLabel,minOf(editWidth,width-6),action=edit))
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

/** Consume right-click before vanilla Button rejects it; keyboard activation still advances. */
class DirectionalButton(text:String,private val cycle:(Int)->Unit):ButtonComponent(Component.literal(text),{cycle(1)}) {
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean):Boolean {
        if(click.button()==1) {cycle(-1);return true}
        return super.onMouseDown(click,doubled)
    }
}
