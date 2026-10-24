/* SPDX-License-Identifier: GPL-3.0-only
 * Port/adaptation of NoFrills ClickGui, Category and Module.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui.nofrills

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.ConfigUi
import dev.solyanka.ui.*
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.*
import io.wispforest.owo.ui.core.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class NoFrillsClickGui(private val previous:Screen?,private var query:String=""):
    BaseOwoScreen<FlowLayout>(Component.literal("solyanka")) {
    private lateinit var columns:FlowLayout
    private lateinit var mainScroll:ScrollContainer<FlowLayout>
    private val categoryScrolls=mutableListOf<ScrollContainer<FlowLayout>>()
    private var cursorX=0;private var cursorY=0
    private var panel:SettingsPanel?=null
    private var toggleError:String?=null
    fun showPanel(next:SettingsPanel) {
        uiAdapter.rootComponent.focusHandler()?.focus(null,UIComponent.FocusSource.MOUSE_CLICK)
        panel?.dispose();panel=next;next.mount(width,height)
    }
    fun closePanel() { panel?.dispose();panel=null;populate() }
    override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) { w,h -> UIContainers.verticalFlow(w,h) }
    override fun build(root:FlowLayout) {
        // Transparent root: opening the GUI or a settings panel does not dim the world.
        columns=UIContainers.horizontalFlow(Sizing.content(),Sizing.fill(100))
        mainScroll=UIContainers.horizontalScroll(Sizing.fill(100),Sizing.fill(78),columns)
        mainScroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.WHITE)))
        root.child(mainScroll)
        val search=FlatTextbox(Sizing.fixed(minOf(200,(width-100).coerceAtLeast(80))))
        search.text(query);search.setSuggestion("Search...")
        search.positioning(Positioning.relative(50,100));search.margins(Insets.of(0,3,0,0))
        search.onChanged().subscribe { query=it;populate() };root.child(search)
        root.child(GuiKit.button("Done",55) { onClose() }.apply {
            positioning(Positioning.relative(100,100));margins(Insets.of(0,3,0,3))
        })
        populate()
    }
    private fun populate() {
        columns.clearChildren();categoryScrolls.clear()
        val groups=GuiCatalog.search(query)
        if(groups.isEmpty()) columns.child(GuiKit.label("No matching features").apply { margins(Insets.of(8)) })
        groups.forEach { (name,features) ->
            val w=maxOf(100,features.maxOf { Minecraft.getInstance().font.width(it.name)+12 })
            val cards=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content())
            features.forEach { info -> cards.child(FeatureCard(info,w,
                toggle={
                    val states=SolyankaClient.states().toMutableMap();states[info.id]=!SolyankaClient.isEnabled(info.id)
                    val failure=SolyankaClient.applySettings(SolyankaClient.currentProfile(),states)
                    toggleError=failure
                },
                settings=if(info.id=="core.diagnostics") null else ({ ConfigUi.openSettings(info.id,this) })
            )) }
            val scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fill(90),cards)
            scroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.BLUE)))
            categoryScrolls.add(scroll)
            val header=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
                alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);padding(Insets.of(3));surface(Surface.flat(GuiKit.BLUE))
                child(GuiKit.label(name).apply { horizontalTextAlignment(HorizontalAlignment.CENTER) })
            }
            columns.child(UIContainers.verticalFlow(Sizing.fixed(w),Sizing.fill(100)).apply {
                margins(Insets.of(5,0,3,0));child(header);child(scroll)
            })
        }
    }
    override fun drawComponentTooltip(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        if(panel==null) super.drawComponentTooltip(g,mx,my,delta)
    }
    override fun getFocused():net.minecraft.client.gui.components.events.GuiEventListener? = panel?.adapter ?: super.getFocused()
    override fun extractRenderState(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        super.extractRenderState(g,mx,my,delta);cursorX=mx;cursorY=my
        g.text(font,"solyanka • Minecraft 26.2",3,height-43,GuiKit.BLUE)
        g.text(font,"Left click: toggle  |  Right click: settings",3,height-32,GuiKit.WHITE)
        toggleError?.let { g.text(font,"Not saved: $it",3,height-54,0xffff5555.toInt()) }
        panel?.adapter?.let { it.extractRenderState(g,mx,my,delta);it.drawTooltip(g,mx,my,delta) }
    }
    override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean {
        panel?.let { it.adapter.mouseScrolled(x,y,horizontal,vertical);return true }
        if(!::mainScroll.isInitialized) return super.mouseScrolled(x,y,horizontal,vertical)
        val hovered=categoryScrolls.firstOrNull { it.isInBoundingBox(x,y) }
        return hovered?.onMouseScroll(x,y,vertical*2) ?: mainScroll.onMouseScroll(x,y,vertical*2)
    }
    override fun keyPressed(event:KeyEvent):Boolean {
        panel?.let { it.keyPressed(event);return true }
        if(event.key()==GLFW.GLFW_KEY_PAGE_UP || event.key()==GLFW.GLFW_KEY_PAGE_DOWN) {
            return mouseScrolled(cursorX.toDouble(),cursorY.toDouble(),0.0,if(event.key()==GLFW.GLFW_KEY_PAGE_UP) 4.0 else -4.0)
        }
        return super.keyPressed(event)
    }
    override fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
        panel?.let { it.mouseClicked(event,doubled);return true }
        return super.mouseClicked(event,doubled)
    }
    override fun mouseReleased(event:MouseButtonEvent):Boolean {
        panel?.let { it.adapter.mouseReleased(event);return true }
        return super.mouseReleased(event)
    }
    override fun mouseDragged(event:MouseButtonEvent,dx:Double,dy:Double):Boolean {
        panel?.let { it.adapter.mouseDragged(event,dx,dy);return true }
        return super.mouseDragged(event,dx,dy)
    }
    override fun charTyped(event:CharacterEvent):Boolean {
        panel?.let { it.charTyped(event);return true }
        return super.charTyped(event)
    }
    override fun resize(width:Int,height:Int) { super.resize(width,height);panel?.mount(width,height) }
    // Navigation returns fresh screen instances; release owned native cursor handles.
    override fun dispose() { panel?.dispose();panel=null;super.dispose();uiAdapter=null }
    override fun removed() { super.removed();dispose() }
    override fun onClose() {
        if(panel!=null) { closePanel();return }
        val client=Minecraft.getInstance()
        if(previous!=null) client.setScreenAndShow(previous) else client.gui.setScreen(null)
    }
}

private class FeatureCard(private val info:FeatureCardInfo,width:Int,private val toggle:()->Unit,private val settings:(()->Unit)?):
    FlowLayout(Sizing.fixed(width),Sizing.content(),Algorithm.VERTICAL) {
    private val title=GuiKit.label(info.name)
    init {
        title.margins(Insets.of(3,2,5,5));title.tooltip(Component.literal(info.description));child(title);paint()
    }
    private fun paint() {
        val active=SolyankaClient.isEnabled(info.id)
        title.text(Component.literal(info.name).withColor(if(active) 0x5ca0bf else 0xdddddd))
        if(active) surface(Surface.flat(0xaa101010.toInt()).and { g,c -> g.fill(c.x(),c.y(),c.x()+2,c.y()+c.height(),GuiKit.WHITE) })
        else surface(Surface.flat(0xaa000000.toInt()))
    }
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean):Boolean {
        return when(click.button()) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> { toggle();paint();true }
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> { settings?.invoke();true }
            else -> false
        }
    }
}
