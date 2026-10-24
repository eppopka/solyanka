/* SPDX-License-Identifier: GPL-3.0-only
 * Chat/tooltip behavior adapted from WhatYouThing/NoFrills; see NOTICE. */
package dev.solyanka.utility

import com.google.gson.*
import java.text.DateFormat
import java.util.*
import kotlin.math.*

data class CopyBinding(val device:InputDevice=InputDevice.KEYBOARD,val key:Int=-1) {
    fun matches(d:InputDevice,k:Int)=key>=0 && device==d && key==k
    fun validate() { require(key == -1 || (if(device==InputDevice.MOUSE) key in 0..7 else key in 32..348)) { "Invalid copy key" } }
}
data class ChatSettings(
    val copy:CopyBinding=CopyBinding(),val copyLine:CopyBinding=CopyBinding(),
    val trimOnCopy:Boolean=false,val messageOnCopy:Boolean=false,val feedbackLimit:Int=50,
    val keepHistory:Boolean=false,val extraLines:Boolean=false,val lines:Int=1000
) {
    fun validate() { copy.validate();copyLine.validate();require(feedbackLimit in 0..512) { "Feedback limit must be 0–512" };require(lines in 100..5000) { "History limit must be 100–5000" } }
}
data class TooltipSettings(val dungeonQuality:Boolean=false,val createdDate:Boolean=false,val hexColor:Boolean=false,
    val museum:Boolean=false,val skyblockId:Boolean=false)
data class GeneralSettings(val chat:ChatSettings=ChatSettings(),val tooltips:TooltipSettings=TooltipSettings(),val features:FeatureSettings=FeatureSettings()) {
    fun validate() { chat.validate();features.validate() }
}
object GeneralCodec {
    private fun JsonObject.obj(k:String)=get(k)?.let { require(it.isJsonObject) { "$k must be an object" };it.asJsonObject } ?: JsonObject()
    private fun JsonObject.bool(k:String,d:Boolean=false)=get(k)?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "$k must be boolean" };it.asBoolean } ?: d
    private fun JsonObject.int(k:String,d:Int)=get(k)?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "$k must be integer" };it.asBigDecimal.intValueExact() } ?: d
    private fun binding(o:JsonObject):CopyBinding {
        val device=o.get("device")?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isString);InputDevice.valueOf(it.asString) } ?: InputDevice.KEYBOARD
        return CopyBinding(device,o.int("key",-1))
    }
    fun decode(o:JsonObject?):GeneralSettings {
        val root=o ?: JsonObject();val c=root.obj("chat");val t=root.obj("tooltips")
        return GeneralSettings(ChatSettings(binding(c.obj("copy")),binding(c.obj("copyLine")),c.bool("trimOnCopy"),
            c.bool("messageOnCopy"),c.int("feedbackLimit",50),c.bool("keepHistory"),c.bool("extraLines"),c.int("lines",1000)),
            TooltipSettings(t.bool("dungeonQuality"),t.bool("createdDate"),t.bool("hexColor"),t.bool("museum"),t.bool("skyblockId")),FeatureCodec.decode(root.obj("features"))).also { it.validate() }
    }
    fun encode(s:GeneralSettings,base:JsonObject?=null):JsonObject {
        s.validate()
        fun merge(old:JsonObject,fresh:JsonObject):JsonObject {
            val result=old.deepCopy()
            fresh.entrySet().forEach { (k,v) -> result.add(k,if(v.isJsonObject && old.get(k)?.isJsonObject==true) merge(old.getAsJsonObject(k),v.asJsonObject) else v) }
            return result
        }
        return merge(base ?: JsonObject(),Gson().toJsonTree(s).asJsonObject)
    }
}
object ChatLogic {
    /** Coordinates follow 26.2 ChatComponent: scale, +4 x offset, floored baseline. */
    fun hoveredIndex(x:Double,y:Double,height:Int,width:Int,scale:Double,lineHeight:Int,scroll:Int,total:Int,perPage:Int):Int? {
        if(scale<=0 || lineHeight<=0 || scroll<0 || scroll>=total || perPage<=0) return null
        val localX=x/scale-4;val localY=y/scale
        val bottom=floor((height-40)/scale)
        val visible=min(perPage,total-scroll)
        if(localX < -4 || localX >= ceil(width/scale)+8 || localY>=bottom || localY<bottom-visible*lineHeight) return null
        return (floor((bottom-localY)/lineHeight).toInt()+scroll).takeIf { it<scroll+visible }
    }
    fun plain(text:String)=Regex("(?i)§[0-9A-Z]").replace(text,"")
    fun copied(text:String,trim:Boolean)=plain(text).let { if(trim) it.trim() else it }
    fun feedback(text:String,singleLine:Boolean,limit:Int):String {
        require(limit in 0..512)
        val type=if(singleLine) "line" else "message"
        // Bound by Unicode code points, not UTF-16 halves.
        val points=text.codePointCount(0,text.length)
        val preview=if(limit==0) "" else text.substring(0,text.offsetByCodePoints(0,min(points,limit)))+(if(points>limit) "..." else "")
        return "Copied $type to clipboard"+(if(limit==0) "." else ": $preview")
    }
}

data class ItemFacts(val id:String="",val boost:Int=0,val tier:Int=0,val timestamp:Long=0,val color:Int?=null,
    val donated:Boolean=false,val petInfo:String="",val runes:Map<String,Int> = emptyMap(),
    val enchantments:Map<String,Int> = emptyMap(),val potion:String="",val potionLevel:Int=0,
    val name:String="",val lore:List<String> = emptyList())
data class InfoLine(val label:String,val value:String,val perfect:Boolean=false)
object TooltipLogic {
    private val shardAliases=mapOf("cinderbat" to "CINDER_BAT","abyssal lanternfish" to "ABYSSAL_LANTERN",
        "stridersurfer" to "STRIDER_SURFER","bogged" to "SEA_ARCHER","loch emperor" to "SEA_EMPEROR",
        "end stone protector" to "ENDSTONE_PROTECTOR","lotusfish" to "LOTUS_FISH","inkling" to "NIGHT_SQUID",
        "field mouse" to "PEST","flipflopper" to "FLIP_FLOPPER","earthworm" to "TERMITE","zealot bruiser" to "BRUISER",
        "seashine" to "SEA_SHINE","beetle" to "CROPEETLE","wither spectre" to "WITHER_SPECTER","inferno demonlord" to "BURNINGSOUL")
    private val source=Regex("""Source: (.*) Shard \(.*[0-9]*\)""")
    fun shardId(f:ItemFacts):String {
        val isShard=f.id=="ATTRIBUTE_SHARD" || f.name.contains(" Shard") || (f.id.isEmpty() &&
            (f.lore.any { it.contains(" SHARD (ID ") } || (f.lore.any { it.startsWith("Source: ") && it.contains(" Shard") } && f.lore.any { it.startsWith("Rarity: ") })))
        if(!isShard) return ""
        val name=f.lore.firstNotNullOfOrNull { source.matchEntire(it)?.groupValues?.get(1) } ?: f.name
        val shard=name.substringBefore(" Shard").trim().lowercase(Locale.ROOT)
        if(shard in setOf("prismarine","enchanted prismarine","reroll") || shard.isEmpty()) return ""
        return "SHARD_"+(shardAliases[shard] ?: shard.replace(" ","_").uppercase(Locale.ROOT))
    }
    fun marketId(f:ItemFacts):String {
        shardId(f).takeIf { it.isNotEmpty() }?.let { return it }
        if(f.boost==50) return "${f.id}_MAX_BOOST_TIER_${f.tier}"
        return when(f.id) {
            "PET" -> try {
                if(f.petInfo.isEmpty()) "UNKNOWN_PET" else {
                    val pet=JsonParser.parseString(f.petInfo).asJsonObject
                    val type=pet.get("type");val tier=pet.get("tier")
                    if(type?.isJsonPrimitive==true && type.asJsonPrimitive.isString && tier?.isJsonPrimitive==true && tier.asJsonPrimitive.isString)
                        "${type.asString}_PET_${tier.asString}" else "UNKNOWN_PET"
                }
            } catch(_:Exception) { "UNKNOWN_PET" }
            "RUNE","UNIQUE_RUNE" -> f.runes.entries.firstOrNull()?.let { "${it.key}_${it.value}_RUNE" } ?: "EMPTY_RUNE"
            "ENCHANTED_BOOK" -> f.enchantments.entries.singleOrNull()?.let { "ENCHANTMENT_${it.key.uppercase(Locale.ROOT)}_${it.value}" } ?: "ENCHANTMENT_UNKNOWN"
            "POTION" -> if(f.potion.isEmpty()) "UNKNOWN_POTION" else "${f.potion.uppercase(Locale.ROOT)}_${f.potionLevel}_POTION"
            else -> f.id
        }
    }
    fun lines(f:ItemFacts,s:TooltipSettings,museum:Map<String,String>,locale:Locale=Locale.getDefault(),zone:TimeZone=TimeZone.getDefault()):List<InfoLine> = buildList {
        if(s.dungeonQuality && f.boost!=0) add(InfoLine("Quality","${f.boost}/50, Tier ${f.tier}",f.boost==50))
        if(s.createdDate && f.timestamp!=0L) {
            val date=Date(f.timestamp);val day=java.text.SimpleDateFormat("EEEE",locale).apply { timeZone=zone }.format(date)
            val whenText=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,locale).apply { timeZone=zone }.format(date)
            add(InfoLine("Created","$day $whenText"))
        }
        if(s.hexColor && f.color!=null) add(InfoLine("Dye Color","#%06x".format(Locale.ROOT,f.color and 0xffffff)))
        if(s.museum) museum[f.id.replace("STARRED_","")]?.let { category ->
            val title=category.lowercase(Locale.ROOT).split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
            add(InfoLine("Museum",title+(if(f.donated) " (Donated)" else "")))
        }
        if(s.skyblockId) marketId(f).takeIf { it.isNotEmpty() }?.let { add(InfoLine("Item ID",it)) }
    }
    /** Official public Hypixel resource; never replace a last-good cache with a failed/partial payload. */
    fun museumData(json:String):Map<String,String> {
        require(json.length<=8_388_608) { "Museum response too large" }
        val root=JsonParser.parseString(json).asJsonObject
        val success=root.get("success")
        require(success?.isJsonPrimitive==true && success.asJsonPrimitive.isBoolean && success.asBoolean)
        val items=root.getAsJsonArray("items");require(items.size() in 1..30000)
        return buildMap {
            items.forEach { item ->
                val o=item.asJsonObject
                o.get("museum_data")?.let { raw ->
                    val data=raw.asJsonObject;val id=o.get("id");val category=data.get("category")
                    require(id?.isJsonPrimitive==true && id.asJsonPrimitive.isString && id.asString.length in 1..200)
                    require(category?.isJsonPrimitive==true && category.asJsonPrimitive.isString && category.asString.length in 1..100)
                    put(id.asString,category.asString)
                    data.get("mapped_item_ids")?.asJsonArray?.forEach { mapped ->
                        require(mapped.isJsonPrimitive && mapped.asJsonPrimitive.isString && mapped.asString.length in 1..200)
                        put(mapped.asString,category.asString)
                    }
                }
            }
            require(isNotEmpty()) { "No museum categories returned" }
        }
    }
}
