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
    override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) { w,h -> UIContainers.verticalFlow(w,h) }
    override fun build(root:FlowLayout) {
        root.surface(Surface.VANILLA_TRANSLUCENT)
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
                    if(failure!=null) ConfigUi.error({ NoFrillsClickGui(previous,query) },failure)
                },
                settings=if(info.id=="core.diagnostics") null else ({ ConfigUi.openSettings(info.id) { NoFrillsClickGui(previous,query) } })
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
    override fun extractRenderState(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        super.extractRenderState(g,mx,my,delta);cursorX=mx;cursorY=my
        g.text(font,"solyanka • Minecraft 26.2",3,height-43,GuiKit.BLUE)
        g.text(font,"Left click: toggle  |  Right click: settings",3,height-32,GuiKit.WHITE)
    }
    override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean {
        if(!::mainScroll.isInitialized) return super.mouseScrolled(x,y,horizontal,vertical)
        val hovered=categoryScrolls.firstOrNull { it.isInBoundingBox(x,y) }
        return hovered?.onMouseScroll(x,y,vertical*2) ?: mainScroll.onMouseScroll(x,y,vertical*2)
    }
    override fun keyPressed(event:KeyEvent):Boolean {
        if(event.key()==GLFW.GLFW_KEY_PAGE_UP || event.key()==GLFW.GLFW_KEY_PAGE_DOWN) {
            return mouseScrolled(cursorX.toDouble(),cursorY.toDouble(),0.0,if(event.key()==GLFW.GLFW_KEY_PAGE_UP) 4.0 else -4.0)
        }
        return super.keyPressed(event)
    }
    // Navigation returns fresh screen instances; release owned native cursor handles.
    override fun dispose() { super.dispose();uiAdapter=null }
    override fun removed() { super.removed();dispose() }
    override fun onClose() {
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
