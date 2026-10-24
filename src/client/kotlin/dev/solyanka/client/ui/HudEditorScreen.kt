/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills HudEditorScreen interaction concepts adapted for solyanka's existing Odin HUDs.
 * Original WhatYouThing and NoFrills contributors; pinned revision and license in NOTICE. */
package dev.solyanka.client.ui

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.nofrills.GuiKit
import dev.solyanka.ui.*
import dev.solyanka.utility.HudSpec
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.min

/** Dedicated layout canvas. Samples never enable modules or alter timer/stat state. */
class HudEditorScreen:BaseOwoScreen<FlowLayout>(Component.literal("solyanka HUD editor")) {
    private val model=InstantSettings(SolyankaClient.currentProfile(),SolyankaClient.currentGeneral()) { p,g ->
        SolyankaClient.applySettings(p,SolyankaClient.states(),g)
    }
    private var selected="timer.goldor"
    private var selectionButton:io.wispforest.owo.ui.component.ButtonComponent?=null
    private fun select(key:String) {
        selected=key
        selectionButton?.message=Component.literal(entries().first {it.key==key}.name)
    }
    private var dragging:String?=null
    private var offsetX=0.0;private var offsetY=0.0
    private fun entries()=HudLayout.entries(SolyankaClient.currentGeneral().features,width,height)
    private fun ordered()=entries().sortedBy {it.key==selected}
    private fun bounds(entry:HudEntry)=HudLayout.bounds(entry,entry.lines.maxOf {font.width(it)},width,height)
    private fun at(x:Double,y:Double)=ordered().asReversed().firstOrNull {bounds(it).contains(x,y)}
    private fun change(key:String,block:(HudSpec)->HudSpec) {
        // Always start from live state: an earlier failed write must not leak into a later drag.
        model.updateGeneral { SolyankaClient.currentGeneral().copy(features=HudLayout.update(SolyankaClient.currentGeneral().features,key,width,height,block)) }
    }
    override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) {w,h->UIContainers.verticalFlow(w,h)}
    override fun build(root:FlowLayout) {
        dragging=null
        val list=entries()
        val controls=UIContainers.horizontalFlow(Sizing.content(),Sizing.content()).apply {
            gap(5);verticalAlignment(VerticalAlignment.BOTTOM)
            positioning(Positioning.relative(50,100))
            val selection=GuiKit.choiceRow(min(195,(width-75).coerceAtLeast(100)),"Selected HUD",list.map {it.name},list.indexOfFirst {it.key==selected}.coerceAtLeast(0),
                readCurrent={list.indexOfFirst {it.key==selected}.coerceAtLeast(0)}) { index ->select(list[index].key)}
            selectionButton=selection.children().last() as io.wispforest.owo.ui.component.ButtonComponent
            child(selection)
            child(GuiKit.button("Done",55) {onClose()})
        }
        root.child(controls)
    }
    override fun extractRenderState(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        val hovered=at(mx.toDouble(),my.toDouble())?.key
        ordered().forEach {entry ->
            val b=bounds(entry);val active=entry.hud.enabled
            g.fill(b.x,b.y,b.x+b.width,b.y+b.height,if(entry.key==selected) 0x88213946.toInt() else 0x66101010)
            GuiKit.border(g,b.x,b.y,b.width,b.height,if(entry.key==selected || entry.key==hovered) GuiKit.BLUE else 0xff666666.toInt())
            if(dev.solyanka.client.feature.VanillaHearts.editor(g,entry.key,b.x,b.y) || dev.solyanka.client.feature.ArchiveHuds.editor(g,entry.key,b.x,b.y) || dev.solyanka.client.feature.ExtraHuds.editor(g,entry.key,b.x,b.y) || dev.solyanka.client.feature.ScoreboardHud.editor(g,entry.key,b.x,b.y)) {
                GuiKit.border(g,b.x,b.y,b.width,b.height,if(entry.key==selected || entry.key==hovered) GuiKit.BLUE else 0xff666666.toInt())
                return@forEach
            }
            g.pose().pushMatrix()
            try {
                g.pose().translate(b.x+2f,b.y+1f);g.pose().scale(entry.hud.scale.toFloat(),entry.hud.scale.toFloat())
                entry.lines.forEachIndexed {i,line ->g.text(font,Component.literal(line),0,i*10,if(active) entry.hud.color else 0xff888888.toInt(),true)}
            } finally {g.pose().popMatrix()}
        }
        g.text(font,"HUD editor: drag LMB | scroll: scale | RMB: visibility",5,5,GuiKit.WHITE,true)
        g.text(font,"Samples only; master module switches stay unchanged.",5,16,GuiKit.WHITE,true)
        val entry=entries().first {it.key==selected}
        g.text(font,"${entry.name}: ${entry.hud.x}, ${entry.hud.y} | ${entry.hud.scale}x | ${if(entry.hud.enabled) "shown" else "hidden"}",5,(height-57).coerceAtLeast(28),GuiKit.BLUE,true)
        model.error?.let {g.text(font,"Not saved: $it",5,(height-68).coerceAtLeast(39),0xffff5555.toInt(),true)}
        super.extractRenderState(g,mx,my,delta)
    }
    override fun mouseClicked(click:MouseButtonEvent,doubled:Boolean):Boolean {
        if(click.y()>=height-43) return super.mouseClicked(click,doubled)
        uiAdapter.rootComponent.focusHandler()?.focus(null,UIComponent.FocusSource.MOUSE_CLICK)
        val entry=at(click.x(),click.y()) ?: return true
        select(entry.key)
        if(click.button()==0) {
            val b=bounds(entry);dragging=entry.key;offsetX=click.x()-b.x;offsetY=click.y()-b.y
        } else if(click.button()==1 && entry.resizable) change(entry.key) {it.copy(enabled=!it.enabled)}
        return true
    }
    override fun mouseDragged(click:MouseButtonEvent,dx:Double,dy:Double):Boolean {
        val key=dragging ?: return super.mouseDragged(click,dx,dy)
        val entry=entries().first {it.key==key}
        change(key) { HudLayout.drag(it,click.x(),click.y(),offsetX,offsetY,bounds(entry),width,height) }
        return true
    }
    override fun mouseReleased(click:MouseButtonEvent):Boolean {
        val wasDragging=dragging!=null;dragging=null
        return if(wasDragging) true else super.mouseReleased(click)
    }
    override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean {
        if(y>=height-43) return super.mouseScrolled(x,y,horizontal,vertical)
        val entry=at(x,y) ?: return true
        if(entry.resizable && vertical!=0.0) {
            select(entry.key)
            change(entry.key) {it.copy(scale=NumberRange(.5,4.0,.05).snap(it.scale+if(vertical>0) .05 else -.05))}
        }
        return true
    }
    override fun keyPressed(event:KeyEvent):Boolean {
        if(uiAdapter.rootComponent.focusHandler()?.focused()!=null) return super.keyPressed(event)
        val delta=when(event.key()) {GLFW.GLFW_KEY_LEFT->-1 to 0;GLFW.GLFW_KEY_RIGHT->1 to 0;GLFW.GLFW_KEY_UP->0 to -1;GLFW.GLFW_KEY_DOWN->0 to 1;else->return super.keyPressed(event)}
        change(selected) {it.copy(x=(it.x+delta.first).coerceIn(0,10000),y=(it.y+delta.second).coerceIn(0,10000))}
        return true
    }
    override fun resize(width:Int,height:Int) {dragging=null;super.resize(width,height)}
    override fun removed() {dragging=null;super.removed();dispose();uiAdapter=null}
    override fun onClose() {dragging=null;Minecraft.getInstance().gui.setScreen(null)}
}
