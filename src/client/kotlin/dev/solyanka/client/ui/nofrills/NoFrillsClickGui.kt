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
    private var searchBox:FlatTextbox?=null
    private var focusSearch=false
    private var panel:EmbeddedPanel?=null
    private var toggleError:String?=null
    private val navigation=PanelNavigation<EmbeddedPanel>()
    private var panelPosition:PanelDrag.Point?=null
    private val logger=org.slf4j.LoggerFactory.getLogger("solyanka/gui")
    fun showPanel(next:EmbeddedPanel) {navigation.open(next)}
    fun closePanel() {navigation.close()}
    private fun commitNavigation() {
        val request=navigation.take() ?: return
        val next=request.panel
        if(next==null) {
            panelPosition=panel?.position() ?: panelPosition
            panel?.dispose();panel=null;populate();return
        }
        try {
            // Build first; a failing settings constructor leaves the previous UI usable.
            next.placeAt(panel?.position() ?: panelPosition);next.mount(width,height)
            uiAdapter.rootComponent.focusHandler()?.focus(null,UIComponent.FocusSource.MOUSE_CLICK)
            panel?.dispose();panel=next;toggleError=null
        } catch(e:Exception) {
            runCatching {next.dispose()};logger.error("Cannot open settings panel",e)
            toggleError="Settings could not open — details in logs/latest.log"
        }
    }
    private fun panelInput(action:(EmbeddedPanel)->Unit):Boolean {
        val current=panel ?: return false
        try {action(current)} catch(e:Exception) {
            logger.error("Settings panel input failed",e);navigation.close()
            toggleError="Settings input failed — see logs/latest.log"
        }
        return true
    }
    override fun createAdapter():OwoUIAdapter<FlowLayout> = OwoUIAdapter.create(this) { w,h -> UIContainers.verticalFlow(w,h) }
    override fun build(root:FlowLayout) {
        // Transparent root: opening the GUI or a settings panel does not dim the world.
        columns=UIContainers.horizontalFlow(Sizing.content(),Sizing.fill(100))
        mainScroll=UIContainers.horizontalScroll(Sizing.fixed((width-16).coerceAtLeast(1)),Sizing.fixed((height-92).coerceAtLeast(35)),columns)
        mainScroll.positioning(Positioning.absolute(8,32))
        mainScroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.WHITE)))
        root.child(mainScroll)
        val search=FlatTextbox(Sizing.fixed(minOf(200,(width-100).coerceAtLeast(80))))
        searchBox=search
        search.text(query);search.setSuggestion(if(query.isEmpty()) "Search... (Ctrl+F)" else null)
        search.tooltip(Component.literal("Ctrl+F: focus feature search"))
        search.positioning(Positioning.relative(50,100));search.margins(Insets.of(0,3,0,0))
        search.onChanged().subscribe { query=it;search.setSuggestion(if(it.isEmpty()) "Search... (Ctrl+F)" else null);populate() };root.child(search)
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
            val w=maxOf(136,features.maxOf { Minecraft.getInstance().font.width(it.name)+26 })
            val cards=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content())
            features.forEach { info -> cards.child(FeatureCard(info,w,
                toggle={
                    val states=SolyankaClient.states().toMutableMap();states[info.id]=!SolyankaClient.isEnabled(info.id)
                    val failure=SolyankaClient.applySettings(SolyankaClient.currentProfile(),states)
                    toggleError=failure
                },
                settings=if(info.id=="core.diagnostics") null else ({ try {ConfigUi.openSettings(info.id,this)} catch(e:Exception) {logger.error("Cannot prepare settings for ${info.id}",e);toggleError="Settings unavailable — see logs/latest.log"} })
            )) }
            val scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fill(90),cards)
            scroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.BLUE)))
            categoryScrolls.add(scroll)
            val header=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
                alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);padding(Insets.of(6));surface {g,c->dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),GuiKit.RAISED,GuiKit.LINE,5)}
                child(GuiKit.label(name.uppercase()).apply { horizontalTextAlignment(HorizontalAlignment.CENTER) })
            }
            columns.child(UIContainers.verticalFlow(Sizing.fixed(w),Sizing.fill(100)).apply {
                margins(Insets.of(3,0,4,4));gap(4);child(header);child(scroll)
            })
        }
    }
    override fun drawComponentTooltip(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        if(panel==null) super.drawComponentTooltip(g,mx,my,delta)
    }
    override fun getFocused():net.minecraft.client.gui.components.events.GuiEventListener? = panel?.focused() ?: super.getFocused()
    override fun extractRenderState(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
        commitNavigation()
        if(focusSearch && panel==null) {
            searchBox?.let {box->uiAdapter.rootComponent.focusHandler()?.focus(box,UIComponent.FocusSource.MOUSE_CLICK);box.setFocused(true);box.moveCursorToEnd(false);box.setHighlightPos(0)}
            focusSearch=false
        }
        // Keep the feature list visible behind modal settings; input/tooltips still belong to the panel.
        super.extractRenderState(g,mx,my,delta)
        cursorX=mx;cursorY=my
        g.text(font,"solyanka",12,12,GuiKit.WHITE)
        if(width>290)g.text(font,"CLIENT UTILITIES  /  26.2",83,12,GuiKit.MUTED)
        g.text(font,if(panel==null) "Right-click: settings | Ctrl+F: search" else "Drag the settings title bar to move it",8,height-43,GuiKit.MUTED)
        g.text(font,"Left click: toggle  |  Right click: settings",3,height-32,GuiKit.WHITE)
        toggleError?.let { g.text(font,"Not saved: $it",3,height-54,0xffff5555.toInt()) }
        val current=panel
        if(current!=null)try {current.render(g,mx,my,delta)} catch(e:Exception) {
            logger.error("Settings panel rendering failed",e);navigation.close()
            toggleError="Settings rendering failed — see logs/latest.log"
        }
    }
    override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean {
        if(panelInput {it.mouseScrolled(x,y,horizontal,vertical)})return true
        if(!::mainScroll.isInitialized) return super.mouseScrolled(x,y,horizontal,vertical)
        val hovered=categoryScrolls.firstOrNull { it.isInBoundingBox(x,y) }
        return hovered?.onMouseScroll(x,y,vertical*2) ?: mainScroll.onMouseScroll(x,y,vertical*2)
    }
    override fun keyPressed(event:KeyEvent):Boolean {
        if(event.key()==GLFW.GLFW_KEY_F && event.modifiers() and GLFW.GLFW_MOD_CONTROL != 0) {
            (panel as? dev.solyanka.client.ui.KnifeBrowserPanel)?.let {it.focusSearch();return true}
            navigation.close();focusSearch=true;return true
        }
        if(panelInput {it.keyPressed(event)})return true
        if(event.key()==GLFW.GLFW_KEY_PAGE_UP || event.key()==GLFW.GLFW_KEY_PAGE_DOWN) {
            return mouseScrolled(cursorX.toDouble(),cursorY.toDouble(),0.0,if(event.key()==GLFW.GLFW_KEY_PAGE_UP) 4.0 else -4.0)
        }
        return super.keyPressed(event)
    }
    override fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
        if(panelInput {it.mouseClicked(event,doubled)})return true
        return super.mouseClicked(event,doubled)
    }
    override fun mouseReleased(event:MouseButtonEvent):Boolean {
        if(panelInput {it.mouseReleased(event)})return true
        return super.mouseReleased(event)
    }
    override fun mouseDragged(event:MouseButtonEvent,dx:Double,dy:Double):Boolean {
        if(panelInput {it.mouseDragged(event,dx,dy)})return true
        return super.mouseDragged(event,dx,dy)
    }
    override fun charTyped(event:CharacterEvent):Boolean {
        if(panelInput {it.charTyped(event)})return true
        return super.charTyped(event)
    }
    override fun resize(width:Int,height:Int) { super.resize(width,height);panel?.mount(width,height) }
    // Navigation returns fresh screen instances; release owned native cursor handles.
    override fun dispose() { navigation.clear();panel?.dispose();panel=null;super.dispose();uiAdapter=null }
    override fun removed() { super.removed();dispose() }
    override fun onClose() {
        if(panel!=null || navigation.pending()) { closePanel();return }
        val client=Minecraft.getInstance()
        if(previous!=null) client.setScreenAndShow(previous) else client.gui.setScreen(null)
    }
}

private class FeatureCard(private val info:FeatureCardInfo,width:Int,private val toggle:()->Unit,private val settings:(()->Unit)?):
    FlowLayout(Sizing.fixed(width),Sizing.content(),Algorithm.VERTICAL) {
    private val title=GuiKit.label(info.name)
    init {
        title.margins(Insets.of(6,6,10,10));title.tooltip(Component.literal(info.description));child(title);paint()
    }
    private fun paint() {
        val active=SolyankaClient.isEnabled(info.id)
        title.text(Component.literal(info.name).withColor(if(active) 0xb8e9ff else 0xc3cede))
        if(active) surface {g,c->dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),0xf0243948.toInt(),0xff527c96.toInt(),4);g.fill(c.x()+4,c.y()+5,c.x()+6,c.y()+c.height()-5,GuiKit.BLUE)}
        else surface {g,c->dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),GuiKit.PANEL,GuiKit.LINE,4)}
    }
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean):Boolean {
        return when(click.button()) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> { toggle();paint();true }
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> { settings?.invoke();true }
            else -> false
        }
    }
}
