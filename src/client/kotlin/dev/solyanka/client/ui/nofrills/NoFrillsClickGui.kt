/* SPDX-License-Identifier: GPL-3.0-only
 * Port/adaptation of NoFrills ClickGui, Category and Module.
 * Original: WhatYouThing and NoFrills contributors; see NOTICE. */
package dev.solyanka.client.ui.nofrills

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.ui.ConfigUi
import dev.solyanka.model.*
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
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW

class NoFrillsClickGui(private val previous:Screen?,private var query:String=""):
    BaseOwoScreen<FlowLayout>(Component.literal("solyanka")) {
    private lateinit var columns:FlowLayout
    private lateinit var mainScroll:ScrollContainer<FlowLayout>
    private val categoryScrolls=mutableListOf<ScrollContainer<FlowLayout>>()
    private val featureCards=mutableListOf<FeatureCard>()
    private var cursorX=0;private var cursorY=0
    private var searchBox:FlatTextbox?=null
    private var focusSearch=false
    private var refreshPending=false
    private var recentRefreshAt=0L
    private var panel:EmbeddedPanel?=null
    private var toggleError:String?=null
    private val navigation=PanelNavigation<EmbeddedPanel>()
    private var panelPosition:PanelDrag.Point?=null
    private val logger=org.slf4j.LoggerFactory.getLogger("solyanka/gui")
    fun showPanel(next:EmbeddedPanel) {navigation.open(next)}
    fun closePanel() {navigation.close()}
    fun refreshCards() {if(::columns.isInitialized) populate()}
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
            panel?.dispose();panel=next;toggleError=null;UiSounds.open()
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
        search.text(query);search.setSuggestion(if(query.isEmpty()) GuiText.component("Search... (Ctrl+F)").string else null)
        search.tooltip(GuiText.component("Ctrl+F: focus feature search"))
        search.positioning(Positioning.relative(50,100));search.margins(Insets.of(0,3,0,0))
        search.onChanged().subscribe { query=it;search.setSuggestion(if(it.isEmpty()) GuiText.component("Search... (Ctrl+F)").string else null);populate() };root.child(search)
        root.child(GuiKit.button("Done",55) { onClose() }.apply {
            positioning(Positioning.relative(100,100));margins(Insets.of(0,3,0,3))
        })
        populate()
    }
    private fun populate() {
        columns.clearChildren();categoryScrolls.clear();featureCards.clear()
        val config=SolyankaClient.currentGeneral().features.clickGui
        val groups=GuiCatalog.search(
            query,config.sortByLength,config.favorites,SolyankaClient.recentModules(),
            translatedName={ GuiText.component(it.name).string },
            translatedDescription={ GuiText.featureDescription(it).string },
        )
        if(groups.isEmpty()) columns.child(GuiKit.label("No matching features").apply { margins(Insets.of(8)) })
        groups.forEach { (name,features) ->
            val w=maxOf(136,features.maxOf { Minecraft.getInstance().font.width(GuiText.component(it.name))+26 })
            val cards=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content())
            features.forEach { info -> val card=FeatureCard(info,w,query,
                toggle={
                    if(info.id==GuiCatalog.GUI_SETTINGS) dev.solyanka.client.ui.ClickGuiSettingsUi.open(this)
                    else {
                        val states=SolyankaClient.states().toMutableMap();states[info.id]=!SolyankaClient.isEnabled(info.id)
                        toggleError=SolyankaClient.applySettings(SolyankaClient.currentProfile(),states)
                        // Let the toggle animation finish before rebuilding recent cards.
                        recentRefreshAt=System.nanoTime()+300_000_000L
                    }
                },
                favorite={
                    val general=SolyankaClient.currentGeneral();val settings=general.features.clickGui
                    val favorites=if(info.id in settings.favorites) settings.favorites-info.id else settings.favorites+info.id
                    toggleError=SolyankaClient.applySettings(SolyankaClient.currentProfile(),SolyankaClient.states(),general.copy(features=general.features.copy(clickGui=settings.copy(favorites=favorites))))
                    refreshPending=true
                },
                settings=({ try {if(info.id==GuiCatalog.GUI_SETTINGS) dev.solyanka.client.ui.ClickGuiSettingsUi.open(this) else ConfigUi.openSettings(info.id,this)} catch(e:Exception) {logger.error("Cannot prepare settings for ${info.id}",e);toggleError="Settings unavailable — see logs/latest.log"} })
            );featureCards.add(card);cards.child(card) }
            val scroll=UIContainers.verticalScroll(Sizing.fixed(w),Sizing.fill(90),cards)
            scroll.scrollbarThiccness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(GuiKit.BLUE)))
            categoryScrolls.add(scroll)
            val header=UIContainers.verticalFlow(Sizing.fixed(w),Sizing.content()).apply {
                alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);padding(Insets.of(6));surface {g,c->dev.solyanka.client.feature.ChromeDraw.gradientPanel(g,c.x(),c.y(),c.width(),c.height(),dev.solyanka.gfx.ToggleAnimation.mix(GuiKit.RAISED,GuiKit.WHITE,.08),GuiKit.RAISED,GuiKit.LINE,5)}
                child(GuiKit.label(GuiText.component(name.uppercase()).append(" · ${features.size}")).apply { horizontalTextAlignment(HorizontalAlignment.CENTER) })
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
        GuiFrameClock.beginFrame()
        commitNavigation()
        if(refreshPending || recentRefreshAt!=0L && GuiFrameClock.now>=recentRefreshAt) {refreshPending=false;recentRefreshAt=0L;populate()}
        if(focusSearch && panel==null) {
            searchBox?.let {box->uiAdapter.rootComponent.focusHandler()?.focus(box,UIComponent.FocusSource.MOUSE_CLICK);box.setFocused(true);box.moveCursorToEnd(false);box.setHighlightPos(0)}
            focusSearch=false
        }
        val now=GuiFrameClock.now;val seconds=now/1_000_000_000.0
        // Each card uses the same monotonic frame time.
        featureCards.forEach {it.advance(now)}
        // Keep the feature list visible behind modal settings; input/tooltips still belong to the panel.
        vignette(g)
        super.extractRenderState(g,mx,my,delta)
        cursorX=mx;cursorY=my
        g.text(font,ClickGradient.text("solyanka",seconds),12,12,GuiKit.WHITE)
        if(width>360)g.text(font,"CLIENT UTILITIES  /  26.2",83,12,GuiKit.MUTED)
        g.text(font,GuiText.component(if(panel==null) "Right-click: settings | Ctrl+F: search" else "Drag the settings title bar to move it"),8,height-43,GuiKit.MUTED)
        g.text(font,GuiText.component("Left: toggle  |  Right: settings  |  Middle: favorite"),3,height-32,GuiKit.WHITE)
        toggleError?.let { g.text(font,it,3,height-54,0xffff5555.toInt()) }
        val current=panel
        if(current!=null)try {current.render(g,mx,my,delta)} catch(e:Exception) {
            logger.error("Settings panel rendering failed",e);navigation.close()
            toggleError="Settings rendering failed — see logs/latest.log"
        }
    }
    private fun vignette(g:GuiGraphicsExtractor) {
        val depth=minOf(48,width/2,height/2)
        for(i in 0 until depth step 4) {
            val a=(((1.0-i.toDouble()/depth)*0x55).toInt() shl 24)
            val end=minOf(i+4,depth)
            g.fill(0,i,width,end,a);g.fill(0,height-end,width,height-i,a)
            g.fill(i,0,end,height,a);g.fill(width-end,0,width-i,height,a)
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

private class FeatureCard(private val info:FeatureCardInfo,width:Int,private val query:String,private val toggle:()->Unit,private val favorite:()->Unit,private val settings:(()->Unit)?):
    FlowLayout(Sizing.fixed(width),Sizing.content(),Algorithm.VERTICAL) {
    private val title=GuiKit.label(GuiText.component(info.name))
    private fun active()=info.id==GuiCatalog.GUI_SETTINGS || SolyankaClient.isEnabled(info.id)
    private var progress=if(active()) 1.0 else 0.0
    private var lastNanos=System.nanoTime()
    private var lastColor:Int?=null
    private var lastMatchColor:Int?=null
    private var hoverTarget=false
    private var hoverProgress=0.0
    private var pressedAt=0L
    init {
        cursorStyle(CursorStyle.HAND)
        mouseEnter().subscribe { hoverTarget=true };mouseLeave().subscribe { hoverTarget=false }
        title.margins(Insets.of(6,6,10,10));title.tooltip(GuiText.featureDescription(info));child(title)
        surface {g,c->
            val palette=SolyankaClient.currentGeneral().features.clickGui
            val mix=dev.solyanka.gfx.ToggleAnimation::mix
            val flash=(1.0-(GuiFrameClock.now-pressedAt)/180_000_000.0).coerceIn(0.0,1.0)
            val background=mix(mix(GuiKit.PANEL,0xf0243948.toInt(),progress),0xff354b64.toInt(),(hoverProgress*.45+flash*.3).coerceAtMost(1.0))
            val border=mix(palette.disabledBorderColor,palette.enabledColor or 0xff000000.toInt(),progress)
            if(hoverProgress>.01) dev.solyanka.client.feature.RoundedGlow.draw(g,c.x(),c.y(),c.width(),c.height(),4,palette.enabledColor,hoverProgress*.28,4)
            dev.solyanka.client.feature.ChromeDraw.panel(g,c.x(),c.y(),c.width(),c.height(),background,border,4)
            val bar=mix(0, palette.enabledColor or 0xff000000.toInt(),progress)
            if(bar ushr 24>0) g.fill(c.x()+4,c.y()+5,c.x()+6,c.y()+c.height()-5,bar)
        }
        advance(lastNanos)
    }
    fun advance(now:Long) {
        val palette=SolyankaClient.currentGeneral().features.clickGui
        val seconds=((now-lastNanos).coerceAtLeast(0L))/1_000_000_000.0;lastNanos=now
        progress=dev.solyanka.gfx.ToggleAnimation.step(progress,active(),seconds,palette.toggleAnimations)
        hoverProgress=dev.solyanka.gfx.ToggleAnimation.step(hoverProgress,hoverTarget,seconds,palette.toggleAnimations)
        val color=dev.solyanka.gfx.ToggleAnimation.mix(palette.disabledColor,palette.enabledColor,progress) and 0xffffff
        val highlight=palette.enabledColor and 0xffffff
        if(color!=lastColor) {
            lastColor=color
            title.color(Color.ofArgb(color or 0xff000000.toInt()))
        }
        if(highlight!=lastMatchColor) {
            lastMatchColor=highlight
            val localized=GuiText.component(info.name)
            title.text(featureCardTitle(localized,localized.string,query,highlight))
        }
    }
    override fun onMouseDown(click:MouseButtonEvent,doubled:Boolean):Boolean {
        return when(click.button()) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> { pressedAt=System.nanoTime();toggle();UiSounds.toggle(active());advance(pressedAt);true }
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> { settings?.invoke();true }
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> {favorite();true}
            else -> false
        }
    }
}

internal fun featureCardTitle(localized:Component,localizedName:String,query:String,highlightColor:Int):Component {
    val term=query.trim()
    if(term.isEmpty()) return localized
    val index=localizedName.indexOf(term,ignoreCase=true)
    if(index<0) return localized
    return Component.empty()
        .append(Component.literal(localizedName.substring(0,index)))
        .append(Component.literal(localizedName.substring(index,index+term.length)).withStyle(Style.EMPTY.withColor(highlightColor)))
        .append(Component.literal(localizedName.substring(index+term.length)))
}
