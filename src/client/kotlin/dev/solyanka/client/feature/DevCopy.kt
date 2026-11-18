/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import com.google.gson.*
import dev.solyanka.client.ModInfo
import dev.solyanka.client.mixin.ChatComponentAccessor
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.scores.*
import net.minecraft.network.chat.numbers.StyledFormat
import net.minecraft.world.phys.*
import java.util.Base64

/** Explicit, read-only, clipboard-only developer snapshots. Never sends server commands or uploads. */
object DevCopy {
    private const val PREFIX="[solyanka dev] "
    private val gson=GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private fun clip(value:String,max:Int=16384)=DevCopyLogic.boundedText(value,max)
    private fun position(v:Vec3)=JsonObject().apply {addProperty("x",v.x);addProperty("y",v.y);addProperty("z",v.z)}
    private fun blockPosition(v:BlockPos)=JsonObject().apply {addProperty("x",v.x);addProperty("y",v.y);addProperty("z",v.z)}
    private fun box(b:AABB)=JsonObject().apply {
        addProperty("minX",b.minX);addProperty("minY",b.minY);addProperty("minZ",b.minZ)
        addProperty("maxX",b.maxX);addProperty("maxY",b.maxY);addProperty("maxZ",b.maxZ)
    }
    private data class TextSnapshot(val plain:String,val runs:List<DevTextRun>,val truncated:Boolean)
    private fun snapshot(component:Component):TextSnapshot {
        val runs=mutableListOf<DevTextRun>();val current=StringBuilder();var previous:Style?=null;var size=0;var truncated=false
        fun flush() {
            if(current.isEmpty())return
            val style=previous ?: Style.EMPTY
            runs.add(DevTextRun(current.toString(),style.color?.toString(),style.isBold,style.isItalic,
                style.isUnderlined,style.isStrikethrough,style.isObfuscated,style.font.toString(),clip(style.toString(),4096)))
            current.setLength(0)
        }
        // Effective styles include inherited component formatting and legacy § codes.
        component.visualOrderText.accept {_,style,codePoint->
            if(size+Character.charCount(codePoint)>16384 || runs.size>=2048) {truncated=true;false}
            else {
                if(previous!=style) {flush();previous=style}
                current.appendCodePoint(codePoint);size+=Character.charCount(codePoint);true
            }
        }
        flush()
        return TextSnapshot(clip(component.string),runs,truncated || component.string.length>16384)
    }
    private fun text(component:Component):JsonObject {
        val value=snapshot(component)
        return JsonObject().apply {
            addProperty("plain",value.plain)
            addProperty("sectionCodes",DevCopyLogic.legacy(value.runs))
            addProperty("ampersandCodes",DevCopyLogic.legacy(value.runs,'&'))
            add("styleRuns",gson.toJsonTree(value.runs));addProperty("truncated",value.truncated)
            addProperty("componentDebug",clip(component.toString()))
        }
    }
    private fun messages(count:Int,allowEmpty:Boolean=false):List<Component> {
        require(count in 1..100) {"Message count/index must be 1–100"}
        val c=Minecraft.getInstance()
        val values=(c.gui.hud.chat as ChatComponentAccessor).solyankaLines().asSequence().map {it.parent()}.distinct()
            .map {it.content()}.filterNot {it.string.startsWith(PREFIX)}.take(count).toList()
        require(allowEmpty || values.isNotEmpty()) {"No retained chat messages available"}
        return values
    }
    private fun message(index:Int):Component {
        val values=messages(index)
        require(values.size>=index) {"Only ${values.size} retained messages available"}
        return values[index-1]
    }
    private fun chat(count:Int):JsonObject=JsonObject().apply {
        val values=messages(count,true).asReversed()
        addProperty("order","oldest to newest; excludes developer-copy feedback")
        addProperty("requested",count);addProperty("available",values.size)
        add("messages",JsonArray().apply {values.forEach {add(text(it))}})
    }
    /** UI header/footer fields are optional: no new version-sensitive Mixin is installed. */
    private fun tabDecoration():JsonObject=JsonObject().apply {
        addProperty("readMode","best-effort reflection on the client TAB overlay only")
        var found=false
        val hud=Minecraft.getInstance().gui.hud
        hud.javaClass.declaredFields.filter {it.type.simpleName.contains("PlayerTab",true)}.forEach {field->
            runCatching {
                field.isAccessible=true
                val overlay=field.get(hud) ?: return@runCatching
                for(name in listOf("header","footer")) {
                    val entry=runCatching {overlay.javaClass.getDeclaredField(name)}.getOrNull() ?: continue
                    if(!Component::class.java.isAssignableFrom(entry.type))continue
                    entry.isAccessible=true
                    val component=entry.get(overlay) as? Component
                    add(name,component?.let(::text) ?: JsonNull.INSTANCE);found=true
                }
            }
        }
        addProperty("available",found)
        if(!found)addProperty("note","Header/footer fields unavailable in this client version; player rows are still captured")
    }
    private fun tab():JsonObject {
        val c=Minecraft.getInstance();val connection=c.connection ?: error("Join a world first")
        val players=connection.listedOnlinePlayers.sortedWith(
            compareByDescending<net.minecraft.client.multiplayer.PlayerInfo> {it.tabListOrder}
                .thenBy {if(it.gameMode==net.minecraft.world.level.GameType.SPECTATOR) 1 else 0}
                .thenBy {it.team?.name ?: ""}.thenBy(String.CASE_INSENSITIVE_ORDER) {it.profile.name})
        return JsonObject().apply {
            addProperty("total",players.size);addProperty("truncated",players.size>256)
            add("headerAndFooter",tabDecoration())
            add("rows",JsonArray().apply {players.take(256).forEach {info->add(JsonObject().apply {
                addProperty("profileName",info.profile.name);addProperty("profileDebug",clip(info.profile.toString(),4096))
                addProperty("order",info.tabListOrder);addProperty("latency",info.latency)
                addProperty("gameMode",info.gameMode.toString());addProperty("team",info.team?.name)
                val display=info.tabListDisplayName ?: PlayerTeam.formatNameForTeam(info.team,Component.literal(info.profile.name))
                add("display",text(display))
            })}})
        }
    }
    private fun scoreboard():JsonObject {
        val c=Minecraft.getInstance();val level=c.level ?: error("Join a world first");val board=level.scoreboard
        val team=c.player?.let {board.getPlayersTeam(it.scoreboardName)}
        val colored=team?.color?.orElse(null)?.displaySlot()
        val active=colored?.let {board.getDisplayObjective(it)} ?: board.getDisplayObjective(DisplaySlot.SIDEBAR)
        return JsonObject().apply {
            addProperty("note","All active display slots, including hidden score rows. visibleSidebar identifies the active sidebar.")
            add("displaySlots",JsonArray().apply {DisplaySlot.values().forEach {slot->
                val objective=board.getDisplayObjective(slot) ?: return@forEach
                val entries=board.listPlayerScores(objective).sortedWith(compareByDescending<PlayerScoreEntry> {it.value()}.thenBy(String.CASE_INSENSITIVE_ORDER) {it.owner()})
                add(JsonObject().apply {
                    addProperty("slot",slot.name);addProperty("objective",objective.name);addProperty("visibleSidebar",objective===active)
                    add("title",text(objective.displayName));addProperty("totalRows",entries.size);addProperty("truncated",entries.size>256)
                    add("rows",JsonArray().apply {entries.take(256).forEach {entry->add(JsonObject().apply {
                        addProperty("owner",entry.owner());addProperty("value",entry.value());addProperty("hidden",entry.isHidden)
                        add("display",text(PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()),entry.display() ?: Component.literal(entry.owner()))))
                        add("formattedValue",text(entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT))))
                    })}})
                })
            }})
        }
    }
    private fun textures(values:List<String>):JsonArray=JsonArray().apply {
        values.take(8).forEach {value->add(JsonObject().apply {
            addProperty("base64",clip(value,32768))
            if(value.length<=32768)runCatching {
                val decoded=Base64.getDecoder().decode(value).toString(Charsets.UTF_8)
                add("decoded",JsonParser.parseString(decoded))
            }.onFailure {addProperty("decodeError","Invalid texture payload")}
        })}
    }
    private fun item(stack:ItemStack):JsonObject=JsonObject().apply {
        addProperty("item",BuiltInRegistries.ITEM.getKey(stack.item).toString());addProperty("count",stack.count)
        add("name",text(stack.hoverName))
        addProperty("customDataSnbt",stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.let {clip(it.toString())})
        val lore=stack.get(DataComponents.LORE)?.lines().orEmpty()
        addProperty("loreTruncated",lore.size>64)
        add("lore",JsonArray().apply {lore.take(64).forEach {add(text(it))}})
        val profile=stack.get(DataComponents.PROFILE)?.partialProfile()
        if(profile!=null) {
            addProperty("headProfileDebug",clip(profile.toString()))
            add("headTextures",textures(profile.properties()?.get("textures")?.map {it.value()}.orEmpty()))
        }
    }
    /** Optional public snapshot method; failure is reported, never replaced by invented entity data. */
    private fun metadata(entity:Entity):JsonObject=JsonObject().apply {
        val data=entity.entityData
        val method=data.javaClass.methods.firstOrNull {it.name=="getNonDefaultValues" && it.parameterCount==0}
        if(method==null) {addProperty("available",false);return@apply}
        runCatching {
            val values=(method.invoke(data) as? List<*>).orEmpty()
            addProperty("available",true);addProperty("total",values.size);addProperty("truncated",values.size>64)
            add("entries",JsonArray().apply {values.take(64).filterNotNull().forEach {value->add(JsonObject().apply {
                addProperty("debug",clip(value.toString(),8192))
                val extracted=runCatching {value.javaClass.getMethod("value").invoke(value)}.getOrNull()
                when(extracted) {
                    is Component->add("component",text(extracted))
                    is ItemStack->add("item",item(extracted))
                    is java.util.Optional<*>->(extracted.orElse(null) as? Component)?.let {add("component",text(it))}
                }
            })}})
        }.onFailure {addProperty("available",false);addProperty("note","Entity metadata snapshot unavailable")}
    }
    private fun entity(value:Entity):JsonObject=JsonObject().apply {
        addProperty("id",value.id);addProperty("uuid",value.uuid.toString());addProperty("class",value.javaClass.name)
        addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(value.type).toString())
        add("position",position(value.position()));add("bounds",box(value.boundingBox))
        addProperty("alive",value.isAlive);addProperty("invisible",value.isInvisible)
        add("name",text(value.name));value.customName?.let {add("customName",text(it))}
        add("metadata",metadata(value))
        if(value is LivingEntity)add("equipment",JsonObject().apply {EquipmentSlot.values().forEach {slot->
            runCatching {value.getItemBySlot(slot)}.onSuccess {stack->if(!stack.isEmpty)add(slot.name.lowercase(),item(stack))}
                .onFailure {addProperty(slot.name.lowercase()+"Error","Slot unavailable")}
        }})
        if(value is ItemEntity)add("droppedItem",item(value.item))
        if(value is ItemFrame)add("frameItem",item(value.item))
    }
    private fun around(radius:Int):JsonObject {
        DevCopyLogic.validateRadius(radius)
        val c=Minecraft.getInstance();val player=c.player ?: error("Join a world first");val level=c.level ?: error("No client world")
        val center=player.blockPosition();val blocks=JsonArray();val heads=JsonArray();val blockEntities=JsonArray()
        var air=0;var unloaded=0;var omittedBlocks=0;var omittedHeads=0;var omittedBlockEntities=0
        for(y in center.y-radius..center.y+radius)for(z in center.z-radius..center.z+radius)for(x in center.x-radius..center.x+radius) {
            val pos=BlockPos(x,y,z)
            if(!level.hasChunkAt(pos)) {unloaded++;continue}
            val state=level.getBlockState(pos)
            if(state.isAir) {air++;continue}
            if(blocks.size()<4096)blocks.add(JsonObject().apply {
                add("position",blockPosition(pos));addProperty("block",BuiltInRegistries.BLOCK.getKey(state.block).toString());addProperty("state",state.toString())
            }) else omittedBlocks++
            val be=level.getBlockEntity(pos)
            if(be!=null) {
                if(blockEntities.size()<256)blockEntities.add(JsonObject().apply {add("position",blockPosition(pos));addProperty("class",be.javaClass.name)}) else omittedBlockEntities++
            }
            if(be is SkullBlockEntity) {
                if(heads.size()>=256) {omittedHeads++;continue}
                val profile=be.ownerProfile?.partialProfile()
                heads.add(JsonObject().apply {
                    add("position",blockPosition(pos));addProperty("block",BuiltInRegistries.BLOCK.getKey(state.block).toString())
                    addProperty("state",state.toString());addProperty("profileDebug",profile?.let {clip(it.toString())})
                    add("textures",textures(profile?.properties()?.get("textures")?.map {it.value()}.orEmpty()))
                })
            }
        }
        val bounds=AABB(center.x-radius.toDouble(),center.y-radius.toDouble(),center.z-radius.toDouble(),center.x+radius+1.0,center.y+radius+1.0,center.z+radius+1.0)
        val entities=level.entitiesForRendering().filter {it.boundingBox.intersects(bounds)}.sortedBy {it.position().distanceToSqr(player.position())}
        return JsonObject().apply {
            addProperty("radius",radius);addProperty("shape","inclusive cube centered on the player's block; entities use bounding-box intersection")
            add("center",blockPosition(center));add("playerPosition",position(player.position()));add("bounds",box(bounds))
            addProperty("airBlocksOmitted",air);addProperty("unloadedPositionsSkipped",unloaded)
            addProperty("blocksTruncated",omittedBlocks);addProperty("headsTruncated",omittedHeads);addProperty("blockEntitiesTruncated",omittedBlockEntities)
            add("blocks",blocks);add("blockHeads",heads);add("blockEntities",blockEntities)
            addProperty("totalEntitiesInRange",entities.size);addProperty("entitiesTruncated",(entities.size-256).coerceAtLeast(0))
            add("entities",JsonArray().apply {entities.take(256).forEach {value->
                runCatching {entity(value)}.onSuccess {add(it)}.onFailure {
                    add(JsonObject().apply {addProperty("id",value.id);addProperty("error","Could not inspect this entity")})
                }
            }})
            addProperty("limitations","Client-visible snapshot only. No forced chunk loads, private container inventory, full server NBT or unreceived entities. Block entities other than skulls include class/position only. Equipment contains selected diagnostic components, not every possible item data component.")
        }
    }
    private fun report(kind:String,data:JsonElement):String=gson.toJson(JsonObject().apply {
        addProperty("format","solyanka-dev-copy-v1");addProperty("modVersion",ModInfo.version)
        addProperty("createdAt",java.time.Instant.now().toString());addProperty("kind",kind)
        addProperty("privacy","Local clipboard only. May contain private chat, usernames, UUIDs, coordinates and item/skin data; review before sharing.")
        addProperty("textEncoding","sectionCodes/ampersandCodes preserve effective rendered style runs; arbitrary RGB uses conventional hex notation. Style runs preserve exact RGB. This is diagnostic JSON, not a native ComponentSerialization export. Logical plain text is included separately from rendered-order runs.")
        add("data",data)
    })
    fun copy(kind:String,argument:Int=1):String {
        val value=when(kind) {
            "message"->DevCopyLogic.legacy(snapshot(message(argument)).runs)
            "ampersand"->DevCopyLogic.legacy(snapshot(message(argument)).runs,'&')
            "chat"->messages(argument).asReversed().joinToString("\n") {DevCopyLogic.legacy(snapshot(it).runs)}
            "json"->report("message",text(message(argument)))
            "tab"->report("tab",tab())
            "scoreboard"->report("scoreboard",scoreboard())
            "around"->report("around",around(argument))
            "all"->report("all",JsonObject().apply {add("chat",chat(10));add("tab",tab());add("scoreboard",scoreboard());add("around",around(argument))})
            else->error("Unknown developer copy mode")
        }
        require(value.toByteArray(Charsets.UTF_8).size<=DevCopyLogic.MAX_CLIPBOARD_BYTES) {"Snapshot exceeds 4 MiB; reduce the radius/count. Clipboard was not changed."}
        Minecraft.getInstance().keyboardHandler.clipboard=value
        return PREFIX+"Copied $kind to clipboard (${value.toByteArray(Charsets.UTF_8).size} bytes). Review private data before sharing."
    }
    fun run(kind:String,argument:Int=1,success:(String)->Unit,failure:(String)->Unit):Int = try {
        success(copy(kind,argument));1
    } catch(e:Exception) {failure(PREFIX+(e.message ?: "Copy failed; see client logs"));0}
}
