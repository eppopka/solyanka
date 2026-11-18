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
    private data class Corpse(val id:java.util.UUID,val kind:CorpseKind,val pos:Vec3)
    private val chests=linkedMapOf<BlockPos,Long>()
    private var corpses=emptyList<Corpse>()
    private val printed=mutableSetOf<java.util.UUID>()
    private var scope=""
    private var announcedType=false
    private var announcedCorpses=false
    private var tabSignature=""
    private var stableSince=0L
    private var nextPartyAt=0L
    private val party=java.util.ArrayDeque<String>()
    fun reset() {
        chests.clear();corpses=emptyList();printed.clear();scope="";announcedType=false;announcedCorpses=false
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
        val newScope=if(shaft) "${ctx.area}:${ctx.mineshaftVariant.orEmpty()}" else ""
        if(scope!=newScope) {
            scope=newScope;corpses=emptyList();printed.clear();announcedType=false;announcedCorpses=false
            tabSignature="";stableSince=now;party.clear()
        }
        val lines=mutableListOf<WorldOverlay.Segment>();val labels=mutableListOf<WorldOverlay.Label>()
        val origin=c.gameRenderer.mainCamera().position().add(player.lookAngle.scale(.5))
        if(S.isEnabled(ModuleIds.POWDER_CHEST) && ctx.area.equals("Crystal Hollows",true)) {
            chests.entries.removeIf {(pos,expiry)->expiry<now || !level.hasChunkAt(pos) || !chest(level.getBlockState(pos))}
            val s=S.currentGeneral().features.powderChest
            chests.keys.forEach {pos->
                WorldOverlay.box(lines,AABB(pos).inflate(.005),s.color,false)
                val point=Vec3.atCenterOf(pos)
                labels.add(WorldOverlay.Label(point.add(0.0,.8,0.0),"Powder Chest",s.color,false))
                if(s.tracer)lines.add(WorldOverlay.Segment(origin,point,s.color,false))
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
            // Re-evaluate equipment each tick; helmets often arrive after entity spawn metadata.
            corpses=level.entitiesForRendering().asSequence().filterIsInstance<ArmorStand>().take(4096).mapNotNull {stand->
                if(!stand.isAlive)return@mapNotNull null
                val data=stand.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.CUSTOM_DATA)?.copyTag() ?: return@mapNotNull null
                val attrs=data.getCompound("ExtraAttributes").orElse(null) ?: data
                val kind=CorpseKind.fromHelmet(attrs.getStringOr("id","")) ?: return@mapNotNull null
                Corpse(stand.uuid,kind,stand.position())
            }.take(64).toList()
            corpses.forEach {corpse->
                val style=s.style(corpse.kind);val p=corpse.pos
                WorldOverlay.box(lines,AABB(p.x-.4,p.y,p.z-.4,p.x+.4,p.y+1.8,p.z+.4),style.color,false)
                labels.add(WorldOverlay.Label(p.add(0.0,2.0,0.0),"${corpse.kind.label} Corpse",style.color,false))
                if(style.tracer && player.position().distanceToSqr(p)>s.tracerRadius*s.tracerRadius)
                    lines.add(WorldOverlay.Segment(origin,p.add(0.0,.7,0.0),style.color,false))
                if(s.coordinates && printed.add(corpse.id)) {
                    val b=BlockPos.containing(p)
                    player.sendSystemMessage(Component.literal("[solyanka] ${corpse.kind.label} Corpse: x: ${b.x}, y: ${b.y}, z: ${b.z}"))
                }
            }
        }
        if(now>=nextPartyAt && party.isNotEmpty()) {
            player.connection.sendCommand("pc ${party.remove()}");nextPartyAt=now+1500
        }
        WorldOverlay.publish(lines,text=labels,owner="mining-helpers")
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
