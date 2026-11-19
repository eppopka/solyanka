/* SPDX-License-Identifier: GPL-3.0-only
 * Corpse helmet IDs and TAB formats follow the user-supplied SkyHanni
 * CorpseType.kt / CorpseFinder.kt. See NOTICE for attribution. */
package dev.solyanka.utility

import java.util.Locale

data class HidePlayersSettings(val onlyInDungeons:Boolean=false,val hideAll:Boolean=false,val distance:Double=3.0) {
    fun validate() {require(distance.isFinite() && distance in 0.0..32.0)}
}
data class RngAnnounceSettings(val allChat:Boolean=false,val guildChat:Boolean=false,val partyChat:Boolean=true,
    val minimumMillions:Double=30.0,val announceUnknownValue:Boolean=true,val requireColors:Boolean=true) {
    fun validate() {require(minimumMillions.isFinite() && minimumMillions in 0.0..1000000.0)}
    fun channels()=buildList {if(allChat)add("ac");if(guildChat)add("gc");if(partyChat)add("pc")}
}
data class PowderChestSettings(val tracer:Boolean=true,val color:Int=0xffffcc55.toInt())
data class CorpseStyle(val color:Int,val tracer:Boolean=true)
data class MineshaftSettings(val announceType:Boolean=false,val announceCorpses:Boolean=false,val coordinates:Boolean=true,val hideOnClick:Boolean=true,
    val tracerRadius:Double=5.0,val lapis:CorpseStyle=CorpseStyle(0xff5555ff.toInt()),
    val tungsten:CorpseStyle=CorpseStyle(0xffaaaaaa.toInt()),val umber:CorpseStyle=CorpseStyle(0xffffaa00.toInt()),
    val vanguard:CorpseStyle=CorpseStyle(-1)) {
    fun validate() {require(tracerRadius.isFinite() && tracerRadius in 0.0..32.0)}
    fun style(type:CorpseKind)=when(type) {CorpseKind.LAPIS->lapis;CorpseKind.TUNGSTEN->tungsten;CorpseKind.UMBER->umber;CorpseKind.VANGUARD->vanguard}
}
enum class CorpseKind(val label:String,val helmet:String) {
    LAPIS("Lapis","LAPIS_ARMOR_HELMET"), TUNGSTEN("Tungsten","MINERAL_HELMET"),
    UMBER("Umber","ARMOR_OF_YOG_HELMET"), VANGUARD("Vanguard","VANGUARD_HELMET");
    companion object {fun fromHelmet(id:String)=entries.firstOrNull {it.helmet==id}}
}
object MineshaftLogic {
    fun typeName(code:String):String {
        val family=mapOf("TOPA" to "Topaz","SAPP" to "Sapphire","AMET" to "Amethyst","AMBE" to "Amber",
            "JADE" to "Jade","TITA" to "Titanium","UMBE" to "Umber","TUNG" to "Tungsten","FAIR" to "Vanguard",
            "RUBY" to "Ruby","ONYX" to "Onyx","AQUA" to "Aquamarine","CITR" to "Citrine","PERI" to "Peridot",
            "JASP" to "Jasper","OPAL" to "Opal","LITT" to "Littlefoot's Den")
        val name=family[code.substringBefore('_')] ?: return code
        return when(code.substringAfter('_',"")) {"C"->"$name Crystal";"L"->name;else->"$name ${code.substringAfter('_')}"}
    }
    private val corpse=Regex("""^\s*(Lapis|Tungsten|Umber|Vanguard):\s*(NOT LOOTED|LOOTED)\s*$""",RegexOption.IGNORE_CASE)
    // Each TAB line represents a corpse, not a count per corpse type.
    fun corpses(tab:List<String>):Map<CorpseKind,Int> = buildMap {
        tab.forEach {line->corpse.matchEntire(ChatLogic.plain(line))?.let {m->
            val kind=CorpseKind.entries.first {it.label.equals(m.groupValues[1],true)}
            put(kind,(get(kind) ?: 0)+1)
        }}
    }
    fun summary(counts:Map<CorpseKind,Int>):String = "[solyanka] Corpses: "+
        CorpseKind.entries.filter { (counts[it] ?: 0)>0 }.joinToString(", ") {"${counts[it]} ${it.label}"}+
        " (total ${counts.values.sum()})"
}
enum class RngDropKind { FOUND, TRACKER, CORPSE_PROFIT }
data class RngDrop(val item:String,val value:Double?,val message:String,
    val kind:RngDropKind=RngDropKind.FOUND,val valueText:String="")
/** Plain displayed text and the effective color for each UTF-16 code unit. */
data class RngStyledMessage(val text:String,val colors:List<String?>) {
    companion object {
        /** Explicit preview only: ampersand codes are never interpreted in live player chat. */
        fun preview(raw:String):RngStyledMessage {
            val palette=mapOf('0' to "#000000",'1' to "#0000aa",'2' to "#00aa00",'3' to "#00aaaa",
                '4' to "#aa0000",'5' to "#aa00aa",'6' to "#ffaa00",'7' to "#aaaaaa",'8' to "#555555",
                '9' to "#5555ff",'a' to "#55ff55",'b' to "#55ffff",'c' to "#ff5555",'d' to "#ff55ff",'e' to "#ffff55",'f' to "#ffffff")
            val text=StringBuilder();val colors=mutableListOf<String?>();var color:String?=null;var i=0
            while(i<raw.length && text.length<=2048) {
                if((raw[i]=='&' || raw[i]=='§') && i+1<raw.length) {
                    val code=raw[i+1].lowercaseChar()
                    if(code in palette) {color=palette[code];i+=2;continue}
                    if(code=='r') {color=null;i+=2;continue}
                    if(code in "klmno") {i+=2;continue}
                }
                text.append(raw[i]);colors.add(color);i++
            }
            return RngStyledMessage(text.toString(),colors)
        }
    }
}
object RngAnnounceLogic {
    private val found=Regex("""^(?:\[[^\]\r\n]{1,40}]\s*)*(?:RNG DROP!\s+)?(?:\[[^\]\r\n]{1,40}]\s*)*([A-Za-z0-9_]{1,16})\s+(?:just\s+)?found\s+an?\s+(.+)!$""",RegexOption.IGNORE_CASE)
    private val tracker=Regex("""^\[SkyHanni]\s*\+?\s*Tracker Drop:\s*(.+?)\s+\(([^()]+)\)\s*$""",RegexOption.IGNORE_CASE)
    private val profit=Regex("""^\[SkyHanni]\s+Profit for\s+((?:Lapis|Tungsten|Umber|Vanguard) Corpse):\s*(.+?)\s*$""",RegexOption.IGNORE_CASE)
    private val amount=Regex("""^([0-9]+(?:\.[0-9]+)?)\s*([kmb]?)\s*(?:coins?)?$""",RegexOption.IGNORE_CASE)
    private val testPrefix=Regex("""^\*\s+""")
    fun normalize(raw:String)=testPrefix.replaceFirst(ChatLogic.plain(raw).trim(),"")
    fun candidate(raw:String):Boolean {
        val text=normalize(raw)
        return text.startsWith("[SkyHanni]",true) || text.startsWith("RNG DROP!",true) || found.matches(text)
    }
    fun price(text:String):Double? {
        val m=amount.matchEntire(text.trim().replace(",","").replace("$","")) ?: return null
        val multiplier=when(m.groupValues[2].lowercase(Locale.ROOT)) {"k"->1e3;"m"->1e6;"b"->1e9;else->1.0}
        return (m.groupValues[1].toDoubleOrNull()?.times(multiplier))?.takeIf {it.isFinite() && it>=0}
    }
    fun parse(raw:String,ownName:String):RngDrop? {
        if(raw.length>2048) return null
        val text=normalize(raw)
        // Anchored whole-message patterns reject All/Party/Guild/whisper speaker prefixes and echoes.
        tracker.matchEntire(text)?.let {m->
            val token=m.groupValues[2].trim();val coins=price(token) ?: return null
            return RngDrop(m.groupValues[1],coins,text,RngDropKind.TRACKER,token)
        }
        profit.matchEntire(text)?.let {m->
            val token=m.groupValues[2].trim();val coins=price(token) ?: return null
            return RngDrop(m.groupValues[1],coins,text,RngDropKind.CORPSE_PROFIT,token)
        }
        val m=found.matchEntire(text) ?: return null
        if(!m.groupValues[1].equals(ownName,true)) return null
        return RngDrop(m.groupValues[2],null,text)
    }
    private fun tokenColor(message:RngStyledMessage,token:String,accepted:Set<String>):Boolean {
        if(token.isBlank() || message.colors.size!=message.text.length)return false
        val start=message.text.indexOf(token,ignoreCase=true)
        if(start<0)return false
        return token.indices.filter {token[it].isLetterOrDigit()}.all {offset->
            message.colors[start+offset]?.lowercase(Locale.ROOT) in accepted
        }
    }
    /** Extra format checks, NOT source authentication: any local mod can synthesize these colors. */
    fun colorFailure(message:RngStyledMessage,drop:RngDrop,required:Boolean):String? {
        if(!required)return null
        if(drop.kind!=RngDropKind.FOUND) {
            val gold=setOf("gold","#ffaa00")
            if(!tokenColor(message,"[SkyHanni]",gold))return "SkyHanni prefix is not gold"
            if(!tokenColor(message,drop.valueText,gold))return "SkyHanni price/profit is not gold"
        } else if(drop.message.contains("RNG DROP!",true)) {
            if(!tokenColor(message,"RNG DROP!",setOf("light_purple","#ff55ff")))return "RNG DROP header is not light purple"
        }
        return null
    }
    fun allowed(drop:RngDrop,s:RngAnnounceSettings)=drop.value?.let {it>=s.minimumMillions*1e6} ?: s.announceUnknownValue
    fun safeMessage(text:String)=text.filterNot {it.isISOControl() || it=='§'}.take(240)
}
/** Session-scoped bounded de-duplication shared by the game and canceled-game callbacks. */
class DropDeduplicator {
    private val seen=linkedMapOf<String,Long>()
    fun clear()=seen.clear()
    fun accept(item:String,now:Long):Boolean {
        seen.entries.removeIf {now-it.value>30000L}
        val key=item.lowercase(Locale.ROOT).replace(Regex("\\s+")," ").trim()
        if(key in seen) return false
        if(seen.size>=128)seen.remove(seen.keys.first())
        seen[key]=now;return true
    }
}
