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
import dev.solyanka.model.NumberRange

object GuiKit {
    private val palette get()=dev.solyanka.utility.ThemeColors.palette(dev.solyanka.client.SolyankaClient.currentGeneral().features.clickGui.theme)
    val BLUE get() = palette.accent
    val WHITE get() = palette.text
    val DARK get() = palette.dark
    val PANEL get() = palette.panel
    val RAISED get() = palette.raised
    val LINE get() = palette.line
    val MUTED get() = palette.muted
    fun border(g: GuiGraphicsExtractor,x:Int,y:Int,w:Int,h:Int,color:Int) {
        g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color)
        g.fill(x,y+1,x+1,y+h-1,color);g.fill(x+w-1,y+1,x+w,y+h-1,color)
    }
    fun button(text:String,width:Int=70,white:Boolean=false,action:()->Unit):ButtonComponent = button(GuiText.component(text),width,white,action)
    fun button(text:Component,width:Int=70,white:Boolean=false,action:()->Unit):ButtonComponent =
        styleButton(UIComponents.button(text) { action() },width,white)
    private fun styleButton(button:ButtonComponent,width:Int,white:Boolean=false):ButtonComponent = button.apply {
            sizing(Sizing.fixed(width),Sizing.fixed(20))
            textShadow(false)
            var hover=0.0;var last=System.nanoTime()
            renderer { g,b,_ ->
                val now=GuiFrameClock.now;val seconds=(now-last).coerceAtLeast(0L)/1_000_000_000.0;last=now
                hover=dev.solyanka.gfx.ToggleAnimation.step(hover,b.isHovered,seconds,dev.solyanka.client.SolyankaClient.currentGeneral().features.clickGui.toggleAnimations)
                val fill=dev.solyanka.gfx.ToggleAnimation.mix(RAISED,0xff354b64.toInt(),hover)
                val edge=dev.solyanka.gfx.ToggleAnimation.mix(if(white) LINE else BLUE,WHITE,hover*.45)
                dev.solyanka.client.feature.ChromeDraw.panel(g,b.x,b.y,b.width,b.height,fill,edge,4)
            }
        }
    fun presetButton(text:String,width:Int,color:Int,action:(Int)->Unit):ButtonComponent = styleButton(DirectionalButton(text,action),width).apply {
        message=Component.literal(text).withColor(color and 0xffffff)
        tooltip(Component.literal("Left click: apply | Right click: save current color (#%08X)".format(color)))
    }
    fun label(text:String) = label(GuiText.component(text))
    fun label(text:Component) = PlainLabel(text).apply { color(Color.ofArgb(WHITE)) }
    fun row(width:Int): FlowLayout = UIContainers.horizontalFlow(Sizing.fixed(width),Sizing.content()).apply {
        padding(Insets.of(5));verticalAlignment(VerticalAlignment.CENTER);gap(6)
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
        child(label(name).apply { horizontalSizing(Sizing.fixed((width-56).coerceAtLeast(20)));tooltip(Component.literal(help)) })
        child(toggle(current,update))
    }
    fun toggle(initial:Boolean,update:(Boolean)->Unit):ButtonComponent = ToggleSwitch(initial,update)
    /** NoFrills-style color swatch and owo palette, embedded in the current panel. */
    fun colorRow(width:Int,name:String,current:Int,update:(Int)->Unit):FlowLayout {
        val container=UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply { padding(Insets.of(5));gap(4) }
        val swatch=UIComponents.box(Sizing.fixed(16),Sizing.fixed(16)).color(Color.ofArgb(current)).fill(true)
        // Do not initialize an unused native HSV widget while opening module settings.
        var picker:ColorPickerComponent?=null
        var selected=current
        var expanded=false
        val edit=button("Edit color",105) {}
        edit.onPress {
            if(expanded) {
                picker?.let {container.removeChild(it)};expanded=false;edit.message=GuiText.component("Edit color")
            } else try {
                val palette=picker ?: ColorPickerComponent().showAlpha(true).apply {
                    sizing(Sizing.fixed(minOf(220,(width-20).coerceAtLeast(80))),Sizing.fixed(110))
                    selectedColor(Color.ofArgb(selected))
                    onChanged().subscribe {color->selected=color.argb();swatch.color(color);update(selected)}
                }.also {picker=it}
                container.child(palette);expanded=true;edit.message=GuiText.component("Close palette")
            } catch(e:Exception) {
                org.slf4j.LoggerFactory.getLogger("solyanka/gui").error("Cannot open color palette for $name",e)
                edit.message=GuiText.component("Palette failed")
            }
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
            selected=dev.solyanka.model.SettingsCatalog.nextChoice(readCurrent?.invoke() ?: selected,choices.size,direction)
            choice.message=Component.literal(choices[selected]);update(selected)
        },width-10)
        choice.tooltip(Component.literal("Left click: next | Right click: previous"))
        return UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
            padding(Insets.of(5));gap(3);child(label(name));child(choice)
        }
    }
    fun sliderRow(width:Int,name:String,current:Double,range:NumberRange,help:String="",update:(Double)->Unit):FlowLayout {
        val caption=label(GuiText.component(name).append(": ${range.label(current)}"))
        val slider=FlatSlider().apply {
            sizing(Sizing.fixed(width-14),Sizing.fixed(12))
            min(range.min);max(range.max);stepSize(range.step);value(current)
            tooltipSupplier { Component.literal(range.label(it)) }
        }
        var last=current
        slider.onChanged().subscribe { raw ->
            val value=range.snap(raw)
            caption.text(GuiText.component(name).append(": ${range.label(value)}"))
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
        val needed=net.minecraft.client.Minecraft.getInstance().font.width(GuiText.component(name))+34+editWidth+25
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
        padding(Insets.of(6));child(label(text).apply { color(Color.ofArgb(MUTED));horizontalSizing(Sizing.fixed((width-12).coerceAtLeast(1))) })
    }
    /** Compact section card used to keep long feature panels scannable. */
    fun section(width:Int,title:String,subtitle:String="") = UIContainers.verticalFlow(Sizing.fixed(width),Sizing.content()).apply {
        padding(Insets.of(7));gap(2)
        surface {g,c->
            val x=c.x().toInt();val y=c.y().toInt();val w=c.width().toInt();val h=c.height().toInt()
            dev.solyanka.client.feature.ChromeDraw.panel(g,x,y,w,h,0x6b263448.toInt(),0x8a435870.toInt(),6)
            g.fill(x,y+6,x+2,y+h-6,BLUE)
        }
        child(label(title).apply {color(Color.ofArgb(BLUE));horizontalSizing(Sizing.fixed((width-14).coerceAtLeast(1)))})
        if(subtitle.isNotBlank()) child(label(subtitle).apply {color(Color.ofArgb(MUTED));horizontalSizing(Sizing.fixed((width-14).coerceAtLeast(1)))})
    }
}

class PlainLabel(text:Component):LabelComponent(text) {
    // owo 0.13.1 passes styleAt() straight to MC 26.2's non-null hover API.
    // Padding, blank labels and gaps between wrapped lines legitimately have no style.
    // EMPTY preserves the normal configured tooltip and rich-text hover path.
    override fun styleAt(mouseX:Int,mouseY:Int):net.minecraft.network.chat.Style =
        super.styleAt(mouseX,mouseY) ?: net.minecraft.network.chat.Style.EMPTY
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean)=false
}

class FlatTextbox(size:Sizing):TextBoxComponent(size) {
    init { verticalSizing(Sizing.fixed(18));setMaxLength(256) }
    override fun extractWidgetRenderState(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int,delta:Float) {
        g.fill(x(),y(),x()+width()+4,y()+height(),GuiKit.DARK)
        GuiKit.border(g,x(),y(),width()+4,height(),GuiKit.LINE)
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


/** Keeps owo input and tooltip behavior, but matches the rest of the chrome. */
class FlatSlider:SlimSliderComponent(Axis.HORIZONTAL) {
    override fun draw(g:OwoUIGraphics,mouseX:Int,mouseY:Int,partialTicks:Float,delta:Float) {
        val trackX=x()+2;val trackY=y()+height()/2-2;val trackW=(width()-4).coerceAtLeast(1)
        val progress=((value()-min())/(max()-min())).coerceIn(0.0,1.0)
        val knobX=trackX+(progress*(trackW-1)).toInt()
        dev.solyanka.client.feature.ChromeDraw.panel(g,trackX,trackY,trackW,4,GuiKit.DARK,GuiKit.LINE,2)
        val fill=(knobX-trackX).coerceAtLeast(0)
        if(fill>0) dev.solyanka.client.feature.ChromeDraw.panel(g,trackX,trackY,fill,4,GuiKit.BLUE,GuiKit.BLUE,2)
        if(isInBoundingBox(mouseX.toDouble(),mouseY.toDouble())) dev.solyanka.client.feature.RoundedGlow.draw(g,knobX-4,trackY-3,9,10,4,GuiKit.BLUE,.5,4)
        dev.solyanka.client.feature.ChromeDraw.panel(g,knobX-4,trackY-3,9,10,GuiKit.WHITE,GuiKit.BLUE,4)
    }
}

/** A real button with state in its narration and tooltip; owo retains keyboard activation. */
class ToggleSwitch(initial:Boolean,private val update:(Boolean)->Unit):ButtonComponent(GuiText.component(if(initial) "Enabled" else "Disabled"),{}) {
    private var current=initial
    private var progress=if(initial) 1.0 else 0.0
    private var last=System.nanoTime()
    init {
        sizing(Sizing.fixed(34),Sizing.fixed(16));cursorStyle(CursorStyle.HAND)
        tooltip(GuiText.component(if(current) "Enabled" else "Disabled"))
        onPress {
            current=!current;message=GuiText.component(if(current) "Enabled" else "Disabled")
            tooltip(message);update(current);UiSounds.toggle(current)
        }
    }
    override fun extractContents(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int,delta:Float) {
        val now=GuiFrameClock.now;val elapsed=(now-last).coerceAtLeast(0L)/1_000_000_000.0;last=now
        val animations=dev.solyanka.client.SolyankaClient.currentGeneral().features.clickGui.toggleAnimations
        progress=dev.solyanka.gfx.ToggleAnimation.step(progress,current,elapsed,animations)
        val track=dev.solyanka.gfx.ToggleAnimation.mix(0xff2a3647.toInt(),GuiKit.BLUE,progress)
        dev.solyanka.client.feature.ChromeDraw.panel(g,x,y,width,height,track,GuiKit.LINE,8)
        val knobX=x+2+(progress*18).toInt()
        dev.solyanka.client.feature.ChromeDraw.panel(g,knobX,y+2,12,12,GuiKit.WHITE,GuiKit.LINE,6)
    }
}
