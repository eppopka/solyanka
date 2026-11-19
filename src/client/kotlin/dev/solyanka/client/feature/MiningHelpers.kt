/* SPDX-License-Identifier: GPL-3.0-only
 * Corpse classification/TAB semantics adapted from the user-supplied SkyHanni
 * CorpseFinder.kt and CorpseType.kt. No SkyHanni runtime dependency. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.*

/** Loaded data only: no chunk loads, automatic clicks, teleports or server requests. */
object MiningHelpers {
    private data class Corpse(val stand:ArmorStand,val kind:CorpseKind) {val id get()=stand.uuid;val pos get()=stand.position()}
    private val chests=linkedMapOf<BlockPos,Long>()
    private var corpses=emptyList<Corpse>()
    private val printed=mutableSetOf<java.util.UUID>()
    private val clicked=mutableSetOf<java.util.UUID>()
    private var scanTick=0
    private var scope=""
    private var announcedType=false
    private var announcedCorpses=false
    private var tabSignature=""
    private var stableSince=0L
    private var nextPartyAt=0L
    private val party=java.util.ArrayDeque<String>()
    fun reset() {
        chests.clear();corpses=emptyList();printed.clear();clicked.clear();scanTick=0;scope="";announcedType=false;announcedCorpses=false
        tabSignature="";stableSince=0L;party.clear();nextPartyAt=0L;WorldOverlay.clear("mining-helpers")
    }
    private fun chest(s:BlockState)=s.`is`(Blocks.CHEST) || s.`is`(Blocks.TRAPPED_CHEST)
    /** Called before vanilla applies individual/section block updates, on the client thread. */
    fun blockUpdate(pos:BlockPos,state:BlockState) {
        if(!S.isEnabled(ModuleIds.POWDER_CHEST) || !RngAnnounce.hypixel())return
        val ctx=FeatureRuntime.context
        if(!ctx.skyblock || !ctx.area.equals("Crystal Hollows",true))return
        if(!chest(state)) {chests.remove(pos);return}
        val c=Minecraft.getInstance();val level=c.level ?: return;val player=c.player ?: return
        if(!level.hasChunkAt(pos) || player.position().distanceToSqr(Vec3.atCenterOf(pos))>32.0*32.0)return
        // Chunk contents are not treated as new treasure. Only an observed non-chest -> chest transition qualifies.
        if(chest(level.getBlockState(pos)))return
        if(chests.size>=64)chests.remove(chests.keys.first())
        chests[pos.immutable()]=System.nanoTime()/1000000L+120000L
    }
    private fun partyMessage(text:String) {if(party.size<4)party.add(RngAnnounceLogic.safeMessage(text))}
    fun tick(c:Minecraft) {
        if(!S.isEnabled(ModuleIds.POWDER_CHEST) && !S.isEnabled(ModuleIds.MINESHAFT)) {reset();return}
        val player=c.player;val level=c.level;val ctx=FeatureRuntime.context
        if(player==null || level==null || !ctx.skyblock || !RngAnnounce.hypixel()) {reset();return}
        val now=System.nanoTime()/1000000L
        val shaft=S.isEnabled(ModuleIds.MINESHAFT) && ctx.area.contains("mineshaft",true)
        val newScope=if(shaft) ctx.area else ""
        if(scope!=newScope) {
            scope=newScope;corpses=emptyList();printed.clear();clicked.clear();announcedType=false;announcedCorpses=false
            tabSignature="";stableSince=now;party.clear()
        }
        val lines=mutableListOf<WorldOverlay.Segment>();val labels=mutableListOf<WorldOverlay.Label>()
        if(S.isEnabled(ModuleIds.POWDER_CHEST) && ctx.area.equals("Crystal Hollows",true)) {
            chests.entries.removeIf {(pos,expiry)->expiry<now || !level.hasChunkAt(pos) || !chest(level.getBlockState(pos))}
            val s=S.currentGeneral().features.powderChest
            chests.keys.forEach {pos->
                WorldOverlay.box(lines,AABB(pos).inflate(.005),s.color,false)
                val point=Vec3.atCenterOf(pos)
                labels.add(WorldOverlay.Label(point.add(0.0,.8,0.0),"Powder Chest",s.color,false))
                // Tracer origin is resolved by collect() using the current render camera.
            }
        } else chests.clear()
        if(shaft) {
            val s=S.currentGeneral().features.mineshaft
            val variant=ctx.mineshaftVariant
            if(s.announceType && !announcedType && variant!=null) {
                partyMessage("[solyanka] Mineshaft: ${MineshaftLogic.typeName(variant)} ($variant)");announcedType=true
            }
            val tab=c.connection?.listedOnlinePlayers?.mapNotNull {it.tabListDisplayName?.string}.orEmpty()
            val counts=MineshaftLogic.corpses(tab)
            val signature=CorpseKind.entries.joinToString {"${it.name}:${counts[it] ?: 0}"}
            if(signature!=tabSignature) {tabSignature=signature;stableSince=now}
            if(s.announceCorpses && !announcedCorpses && counts.isNotEmpty() && now-stableSince>=2000) {
                partyMessage(MineshaftLogic.summary(counts));announcedCorpses=true
            }
            // Recheck late equipment metadata every five ticks; share classification with click handling.
            if(++scanTick%5==0 || corpses.isEmpty())corpses=level.entitiesForRendering().asSequence()
                .filterIsInstance<ArmorStand>().take(4096).mapNotNull {stand->classify(stand)?.let {Corpse(stand,it)}}.take(64).toList()
            corpses.forEach {corpse->
                if(s.coordinates && corpse.stand.isAlive && printed.add(corpse.id)) {
                    val b=BlockPos.containing(corpse.pos)
                    player.sendSystemMessage(Component.literal("[solyanka] ${corpse.kind.label} Corpse: x: ${b.x}, y: ${b.y}, z: ${b.z}"))
                }
            }
        }
        if(now>=nextPartyAt && party.isNotEmpty()) {
            player.connection.sendCommand("pc ${party.remove()}");nextPartyAt=now+1500
        }
        WorldOverlay.publish(lines,text=labels,owner="mining-helpers")
    }
    private fun classify(stand:ArmorStand):CorpseKind? {
        if(!stand.isAlive)return null
        val data=stand.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return null
        val nested=data.getCompoundOrEmpty("ExtraAttributes").getStringOr("id","")
        return CorpseKind.fromHelmet(nested) ?: CorpseKind.fromHelmet(data.getStringOr("id",""))
    }
    /** Called by ordinary mouse input; observes the hit only and never consumes or creates an interaction. */
    fun click(button:Int) {
        val mc=Minecraft.getInstance()
        if(button !in 0..1 || mc.gui.screen()!=null || !S.isEnabled(ModuleIds.MINESHAFT) || !S.currentGeneral().features.mineshaft.hideOnClick)return
        if(!FeatureRuntime.context.area.contains("mineshaft",true) || !RngAnnounce.hypixel())return
        val stand=(mc.hitResult as? EntityHitResult)?.entity as? ArmorStand ?: return
        if(classify(stand)!=null && clicked.size<256)clicked.add(stand.uuid)
    }
    fun collect(context:net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext,
        lines:MutableList<WorldOverlay.Segment>,labels:MutableList<WorldOverlay.Label>) {
        val mc=Minecraft.getInstance();val level=mc.level ?: return;val player=mc.player ?: return
        if(!RngAnnounce.hypixel() || !FeatureRuntime.context.skyblock)return
        val camera=context.levelState().cameraRenderState
        val forward=camera.orientation.transform(org.joml.Vector3f(0f,0f,-1f))
        val origin=camera.pos.add(forward.x.toDouble()*.5,forward.y.toDouble()*.5,forward.z.toDouble()*.5)
        val partial=mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val playerPos=player.getPosition(partial)
        if(S.isEnabled(ModuleIds.POWDER_CHEST) && FeatureRuntime.context.area.equals("Crystal Hollows",true)) {
            val s=S.currentGeneral().features.powderChest
            if(s.tracer)chests.keys.forEach {lines.add(WorldOverlay.Segment(origin,Vec3.atCenterOf(it),s.color,false,2f))}
        }
        if(!S.isEnabled(ModuleIds.MINESHAFT) || !FeatureRuntime.context.area.contains("mineshaft",true))return
        val s=S.currentGeneral().features.mineshaft
        corpses.forEach {corpse->
            val stand=corpse.stand
            if(!stand.isAlive || level.getEntity(stand.id)!==stand)return@forEach
            val p=stand.getPosition(partial)
            if(!CorpseVisibility.visible(s.hideOnClick && corpse.id in clicked,playerPos.distanceToSqr(p),s.tracerRadius))return@forEach
            val style=s.style(corpse.kind)
            WorldOverlay.box(lines,AABB(p.x-.4,p.y,p.z-.4,p.x+.4,p.y+1.8,p.z+.4),style.color,false)
            labels.add(WorldOverlay.Label(p.add(0.0,2.0,0.0),"${corpse.kind.label} Corpse",style.color,false))
            val target=p.add(0.0,.7,0.0)
            if(style.tracer && origin.distanceToSqr(target)>1e-8)lines.add(WorldOverlay.Segment(origin,target,style.color,false,2f))
        }
    }
    /** Explicit export, never overwrites an existing pack. The Waypoints editor owns subsequent edits. */
    fun exportCorpses():String {
        val ctx=FeatureRuntime.context
        require(S.isEnabled(ModuleIds.MINESHAFT) && ctx.area.contains("mineshaft",true)) {"Enter a Mineshaft first"}
        val area=WaypointLogic.areaKey(ctx) ?: error("Wait for the Mineshaft variant to be detected")
        require(corpses.isNotEmpty()) {"No loaded corpses to export"}
        val ways=corpses.map {corpse->val b=BlockPos.containing(corpse.pos)
            Waypoint(GridPos(b.x,b.y,b.z),S.currentGeneral().features.mineshaft.style(corpse.kind).color,
                title="${corpse.kind.label} Corpse",depth=false)
        }
        val name="mineshaft-${System.currentTimeMillis()}"
        DungeonWaypoints.import(name,WaypointCodec.encode(mapOf(area to ways),false))
        return name
    }
}
