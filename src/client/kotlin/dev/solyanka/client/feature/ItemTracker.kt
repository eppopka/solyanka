/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import dev.solyanka.model.HudEntry
import dev.solyanka.utility.*
import dev.solyanka.client.mixin.ChatComponentAccessor
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.nio.file.*
import org.slf4j.LoggerFactory

/** Read-only acquisition estimates. Uses inventory deltas + native Sacks hover details, never both
 * generic drop-chat and inventory gains (which would double-count the same drop). */
object ItemTracker {
    private data class Session(val clock:TrackerClock,val counts:MutableMap<String,Long>)
    private data class Gain(val key:String,val count:Long,val source:String,val time:Long)
    private val log=LoggerFactory.getLogger("solyanka/tracker")
    private val path by lazy {FabricLoader.getInstance().configDir.resolve("solyanka-tracker.json")}
    private var loaded=false;private var writable=true
    private val disk=linkedMapOf<String,TrackerSaved>()
    private val sessions=linkedMapOf<String,Session>()
    private var owner:String?=null;private var lastMode="session"
    private val inventory=TrackerInventoryDelta();private val losses=TrackerTransferLosses()
    private val pending=java.util.ArrayDeque<Gain>()
    private val ids=mutableMapOf<String,String>()
    private var position:Vec3?=null;private var clicked=Long.MIN_VALUE
    private var tick=0;private var ignoreUntil=0L;private var sackIgnoreUntil=0L
    private var definitions="";private var lastFlush=0L
    private data class StackMatch(val key:String?,val marketId:String)
    private val stackMatches=java.util.IdentityHashMap<ItemStack,StackMatch>()
    private var indexedItems:List<TrackerItem>?=null
    private var idIndex=emptyMap<String,TrackerItem>()
    private var nameIndex=emptyMap<String,TrackerItem>()
    private var lastChatParent:Any?=null
    private var lastChatSize=-1
    private var renderedAt=Long.MIN_VALUE
    private var renderedLines:List<String> = emptyList()
    private var renderedSettings:TrackerSettings?=null
    private var detail="Waiting for tracked gains";private var storageError:String?=null
    private var primed=false
    private val displayed=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<net.minecraft.client.multiplayer.chat.GuiMessage,Boolean>())
    private var lastSack="";private var lastSackTime=0L
    private val sackSnapshots=java.util.IdentityHashMap<Component,String>()
    private val recentSacks=linkedMapOf<String,Long>()
    private val localSackVersions=java.util.IdentityHashMap<net.minecraft.client.multiplayer.chat.GuiMessage,String>()
    private fun now()=System.nanoTime()/1_000_000L
    private fun cfg()=S.currentGeneral().features.tracker
    private fun session()=owner?.let {sessions[it]}
    private fun load() {
        if(loaded)return;loaded=true
        try {if(Files.exists(path)) {require(Files.size(path)<=1_048_576);disk.putAll(TrackerDataCodec.decode(Files.readString(path)))}}
        catch(e:Exception) {writable=false;storageError="Tracker data invalid; disk writes disabled. Back up/repair solyanka-tracker.json.";log.warn("Tracker data invalid; preserving original file",e)}
    }
    private fun write() {
        if(!writable)return
        var temp:Path?=null
        try {
            Files.createDirectories(path.parent);temp=Files.createTempFile(path.parent,"solyanka-tracker-",".tmp")
            Files.writeString(temp,TrackerDataCodec.encode(disk))
            try {Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE)}
            catch(e:AtomicMoveNotSupportedException) {Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING)}
            storageError=null
        } catch(e:Exception) {storageError="Tracker data could not be saved; see latest.log";log.warn("Tracker save failed",e)}
        finally {temp?.let {runCatching {Files.deleteIfExists(it)}}}
    }
    fun flush() {
        val key=owner ?: return;val s=session() ?: return
        if(lastMode=="never") {
            disk[key]=TrackerSaved(s.clock.committedMillis,s.counts.toMap())
            while(disk.size>16)disk.remove(disk.keys.first())
            write()
        } else if(disk.remove(key)!=null)write()
        lastFlush=now()
    }
    /** World/disconnect/module changes pause time, but don't erase a session. */
    fun suspend() {
        session()?.clock?.freeze();flush();owner=null
        inventory.reset();losses.clear();pending.clear();position=null;ids.clear();displayed.clear();primed=false
        definitions="";sackSnapshots.clear();recentSacks.clear();localSackVersions.clear();indexedItems=null;stackMatches.clear();lastChatParent=null;lastChatSize=-1;renderedAt=Long.MIN_VALUE;ignoreUntil=now()+2000;sackIgnoreUntil=0L;lastSack=""
    }
    fun worldChanged() {
        suspend()
        if(cfg().persistence=="world_change")sessions.clear()
    }
    fun resetTotals() {
        val key=owner ?: error("Join a world with Tracker enabled first")
        sessions[key]=Session(TrackerClock(),linkedMapOf());inventory.reset();pending.clear();losses.clear()
        ignoreUntil=now()+2000;detail="Totals and timer reset";flush()
    }
    fun inputActivity() {clicked=now()}
    fun addHeldItem():TrackerItem {
        val stack=Minecraft.getInstance().player?.mainHandItem ?: error("Join a world first")
        require(!stack.isEmpty) {"Hold an item first"}
        return TrackerItem(ChatLogic.plain(stack.hoverName.string).take(80),PriceTooltips.trackerItemId(stack))
    }
    private fun account() {
        val player=Minecraft.getInstance().player ?: return;val key=player.uuid.toString()
        if(owner==key)return
        suspend();load();owner=key;lastMode=cfg().persistence
        if(key !in sessions) {
            val saved=if(lastMode=="never")disk[key] ?: TrackerSaved() else TrackerSaved()
            if(sessions.size>=16)sessions.remove(sessions.keys.first())
            sessions[key]=Session(TrackerClock(saved.elapsedMillis),saved.counts.toMutableMap())
            if(lastMode=="session" && disk.remove(key)!=null)write()
        }
        detail="Inventory baseline initialized; pre-existing items are not counted"
    }
    private fun eligible():Boolean {
        val c=Minecraft.getInstance()
        return S.isEnabled(ModuleIds.TRACKER) && c.player!=null && c.level!=null && FeatureRuntime.context.skyblock && cfg().items.isNotEmpty()
    }
    private fun totals():Map<String,Long> {
        val player=Minecraft.getInstance().player ?: return emptyMap();val result=mutableMapOf<String,Long>()
        fun add(stack:ItemStack) {
            if(stack.isEmpty)return
            val match=stackMatches[stack] ?: run {
                // Exact-ID priority is retained. Names/market facts are decoded once per stack identity,
                // not once per item definition on every scan; in-place metadata is refreshed every second.
                val id=if(idIndex.isNotEmpty())PriceTooltips.trackerItemId(stack) else ""
                val item=idIndex[id.uppercase(java.util.Locale.ROOT)] ?: nameIndex[TrackerLogic.nameKey(stack.hoverName.string)]
                val actualId=if(item!=null && id.isBlank())PriceTooltips.trackerItemId(stack) else id
                StackMatch(item?.key(),actualId).also {
                    if(stackMatches.size>=128)stackMatches.clear()
                    stackMatches[stack]=it
                }
            }
            val key=match.key ?: return
            result[key]=(result[key] ?: 0)+stack.count // Quantity stays live, even for a cached identity.
            if(match.marketId.isNotBlank())ids[key]=match.marketId
        }
        for(i in 0 until 36)add(player.inventory.getItem(i))
        for(slot in EQUIPMENT)add(player.getItemBySlot(slot))
        return result
    }
    private val EQUIPMENT=arrayOf(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND)
    private fun delta(key:String,amount:Long,source:String,time:Long) {
        if(amount<0)losses.record(key,source,-amount,time)
        else if(amount>0 && pending.size<128)pending.add(Gain(key,amount,source,time))
    }
    private fun acceptPending(time:Long):Boolean {
        val s=session() ?: return false;var gained=false
        while(pending.isNotEmpty() && time-pending.peek().time>=750) {
            val p=pending.remove();if(cfg().items.none {it.key()==p.key})continue
            val count=losses.consume(p.key,p.source,p.count,time)
            if(count>0) {
                if(p.key !in s.counts && s.counts.size>=128) {detail="Counter history full; reset totals before tracking new names";continue}
                s.counts[p.key]=((s.counts[p.key] ?: 0)+count).coerceAtMost(1_000_000_000_000L)
                detail="Tracked +$count ${p.key} (${p.source})";gained=true
            }
        }
        return gained
    }
    private fun sack(component:Component) {
        val time=now()
        if(!eligible() || !cfg().countSacks || session()==null || time<sackIgnoreUntil)return
        if(!ChatLogic.plain(component.string).trimStart().startsWith("[Sacks]"))return
        val hoverTexts=linkedSetOf<String>();val seen=hashSetOf<Any>()
        // SHOW_TEXT is a record on modern Minecraft; discover only a no-argument Component getter.
        var points=0
        component.visualOrderText.accept {_,style,_->
            val hover=style.hoverEvent
            if(hover!=null && seen.add(hover))runCatching {
                val getter=hover.javaClass.methods.firstOrNull {it.parameterCount==0 && Component::class.java.isAssignableFrom(it.returnType)}
                val content=getter?.invoke(hover) as? Component
                content?.string?.takeIf {it.length<=32768}?.let {hoverTexts.add(ChatLogic.plain(it))}
            }
            ++points<16384
        }
        val joined=hoverTexts.joinToString("\n")
        if(joined.isEmpty()) {detail="Sacks summary seen, but item hover details unavailable";return}
        if(sackSnapshots[component]==joined)return
        if(sackSnapshots.size>=128)sackSnapshots.clear()
        sackSnapshots[component]=joined
        // Identical GAME/GAME_CANCELED/local echoes are coalesced only briefly, not for 30 s.
        recentSacks.entries.removeIf {time-it.value>250 || time<it.value}
        if(recentSacks.containsKey(joined))return
        if(recentSacks.size>=64)recentSacks.remove(recentSacks.keys.first())
        recentSacks[joined]=time
        lastSack=joined;lastSackTime=time
        val changes=TrackerLogic.sackChanges(joined.lines())
        for((name,amount) in changes) {
            val id=PriceTooltips.trackerIdByName(name)
            val item=cfg().items.firstOrNull {id!=null && it.itemId.isNotBlank() && it.itemId.equals(id,true)}
                ?: cfg().items.firstOrNull {it.key()==name}
            if(item!=null)delta(item.key(),amount,"sacks",time)
        }
        if(joined.contains("other items",true))detail="Sacks hover omits some items; hidden details cannot be counted"
    }
    private fun pollSacks() {
        if(!cfg().countSacks || !eligible()) {primed=false;lastChatParent=null;return}
        val all=(Minecraft.getInstance().gui.hud.chat as ChatComponentAccessor).solyankaLines()
        val latest=all.firstOrNull()?.parent()
        // Re-read the small recent window: Sacks overlays may edit the same parent in place.
        lastChatParent=latest;lastChatSize=all.size
        val parents=all.take(16).map {it.parent()}.distinct()
        parents.asReversed().forEach {parent->
            val content=parent.content()
            if(content.string.length<=32768 && ChatLogic.plain(content.string).trimStart().startsWith("[Sacks]")) {
                val version=content.toString() // Includes hover styles, not just visible '+N items'.
                val previous=localSackVersions.put(parent,version)
                if(primed && (parent !in displayed || previous!=version))sack(content)
            }
        }
        localSackVersions.keys.retainAll(parents.toSet())
        displayed.clear();displayed.addAll(parents);primed=true
    }
    fun tick() {
        val c=Minecraft.getInstance()
        if(!S.isEnabled(ModuleIds.TRACKER) || c.player==null || c.level==null) {if(owner!=null)suspend();return}
        account();val record=session() ?: return;val time=now();val s=cfg()
        if(lastMode!=s.persistence) {lastMode=s.persistence;flush()}
        if(indexedItems!==s.items) {
            val nextDefinitions=s.items.joinToString("|") {it.name+":"+it.itemId}
            if(definitions!=nextDefinitions) {definitions=nextDefinitions;inventory.reset();losses.clear();pending.clear();ids.clear();ignoreUntil=time+2000}
            indexedItems=s.items
            idIndex=s.items.filter {it.itemId.isNotBlank()}.associateBy {it.itemId.uppercase(java.util.Locale.ROOT)}
            nameIndex=s.items.filter {it.itemId.isBlank()}.associateBy {it.key()}
            stackMatches.clear()
        }
        val container=c.gui.screen() is AbstractContainerScreen<*>
        if(container) {
            ignoreUntil=time+1500
            pending.removeIf {it.source=="inventory"}
            // Only a Sacks transfer screen suppresses Sacks, not every menu in the game.
            val title=ChatLogic.plain(c.gui.screen()?.title?.string.orEmpty())
            if(title.contains("sack",true)) {sackIgnoreUntil=time+1000;pending.removeIf {it.source=="sacks"}}
        }
        val active=eligible() && !container && time>=ignoreUntil
        ++tick
        if(tick%20==0)stackMatches.clear()
        if(!active)inventory.reset()
        else if(tick%2==0) {
            val changes=inventory.observe(totals(),false)
            changes.forEach {(key,n)->delta(key,n,"inventory",time)}
        }
        if(tick%2==0)pollSacks()
        val gained=if(eligible())acceptPending(time) else {pending.clear();false}
        val pos=c.player?.position();val old=position
        val moved=pos!=null && old!=null && pos.distanceToSqr(old)>0.000001
        position=pos
        val click=time-clicked in 0..250 || c.options.keyAttack.isDown || c.options.keyUse.isDown
        record.clock.step(time,eligible(),c.isWindowActive && (moved || click),gained,s.activityMode=="item")
        if(time-lastFlush>=10_000)flush()
    }
    fun status():String {
        val state=when { !S.isEnabled(ModuleIds.TRACKER)->"Tracker OFF";!eligible()->"Waiting for a world/context and tracked items";session()?.clock?.paused!=false->"Paused / AFK";else->"Tracking" }
        return (storageError?.let {"$it | "} ?: "")+state+" | "+detail+" | "+PriceTooltips.trackerPriceStatus()
    }
    private fun lines(sample:Boolean=false):List<String> {
        if(sample) {val value=TrackerValues.format(194293.0,cfg().compactValues);return listOf("Tracker · 00:12:30","Flint x13,943 ($value)","Total: $value","Profit/h*: "+TrackerValues.format(932606.0,cfg().compactValues))}
        val s=cfg();val time=now()
        if(s===renderedSettings && renderedAt!=Long.MIN_VALUE && time-renderedAt in 0..99)return renderedLines
        val record=session();val counts=record?.counts.orEmpty();val elapsed=record?.clock?.elapsedMillis ?: 0
        var total=0.0;var unknown=0
        val rows=s.items.map {item->
            val count=counts[item.key()] ?: 0
            val id=item.itemId.ifBlank {ids[item.key()] ?: PriceTooltips.trackerIdByName(item.name) ?: ""}
            val unit=TrackerLogic.unitPrice(item,s.priceMode,PriceTooltips.trackerPrice(id))
            if(unit==null && count>0)unknown++
            val price=unit?.times(count);if(price!=null)total+=price
            item.name+" x"+"%,d".format(java.util.Locale.US,count)+" ("+TrackerValues.format(price,s.compactValues)+")"
        }
        val result=buildList {
            add("Tracker · "+TrackerLogic.duration(elapsed)+(if(record?.clock?.paused!=false) " [paused]" else ""))
            if(rows.isEmpty())add("Add items in Tracker settings") else {
                val capacity=((Minecraft.getInstance().window.guiScaledHeight/s.hud.scale-67)/11).toInt().coerceIn(1,32)
                addAll(rows.take(capacity))
                if(rows.size>capacity)add("+${rows.size-capacity} more tracked items")
            }
            add("Total: "+TrackerValues.format(total,s.compactValues)+(if(unknown>0) " + ?" else ""))
            add("Profit/h*: "+TrackerValues.format(TrackerLogic.perHour(total,elapsed),s.compactValues)+(if(unknown>0) " + ?" else ""))
            if(unknown>0)add("? = unavailable price; totals incomplete")
        }
        renderedAt=time;renderedSettings=s;renderedLines=result
        return result
    }
    private fun dimensions(lines:List<String>):Pair<Int,Int> = (minOf(400,lines.maxOfOrNull {Minecraft.getInstance().font.width(it)} ?: 100)+14) to (lines.size*11+12)
    fun editorEntry(entry:HudEntry):HudEntry {
        if(entry.key!="extra.tracker")return entry
        val list=lines(true);val (w,h)=dimensions(list)
        return entry.copy(lines=list,baseWidth=w,baseHeight=h)
    }
    fun preview(g:GuiGraphicsExtractor,x:Int,y:Int) {draw(g,true,x,y)}
    private fun draw(g:GuiGraphicsExtractor,sample:Boolean=false,x:Int?=null,y:Int?=null) {
        val s=cfg();val list=lines(sample);val (w,h)=dimensions(list);val c=Minecraft.getInstance()
        HudFrame.draw(g,s.hud,w,h,x,y,ModuleIds.TRACKER) {
            if(s.background)ChromeDraw.panel(g,0,0,w,h,0xc9151b24.toInt(),0x405e7791,5)
            list.forEachIndexed {i,line->
                var cursor=7
                for(span in TrackerHudStyle.spans(line,s.hud.color)) {
                    val room=w-7-cursor
                    if(room<=0)break
                    val text=c.font.plainSubstrByWidth(span.text,room)
                    g.text(c.font,text,cursor,6+i*11,span.color,true)
                    cursor+=c.font.width(text)
                    if(text.length<span.text.length)break
                }
            }
        }
    }
    fun register() {
        ClientReceiveMessageEvents.MODIFY_GAME.register {message,overlay->
            if(!overlay && message.string.length<=32768)sack(message)
            message
        }
        ClientReceiveMessageEvents.GAME.register {message,overlay->if(!overlay && message.string.length<=32768)Minecraft.getInstance().execute {sack(message)}}
        ClientReceiveMessageEvents.GAME_CANCELED.register {message,overlay->if(!overlay && message.string.length<=32768)Minecraft.getInstance().execute {sack(message)}}
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","tracker")) {g,_->
            val c=Minecraft.getInstance();val screen=c.gui.screen()
            if(S.isEnabled(ModuleIds.TRACKER) && c.player!=null && c.level!=null && !c.gui.hud.isHidden &&
                FeatureRuntime.context.skyblock && (screen==null || screen is ChatScreen || screen is dev.solyanka.client.ui.nofrills.NoFrillsClickGui))draw(g)
        }
    }
}
