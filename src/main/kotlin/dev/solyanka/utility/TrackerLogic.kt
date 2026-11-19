/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*
import java.util.Locale

data class TrackerItem(val name:String="",val itemId:String="",val manualPrice:Double=0.0) {
    fun key()=TrackerLogic.nameKey(name)
}
data class TrackerSettings(val hud:HudSpec=HudSpec(x=12,y=240),val items:List<TrackerItem> = emptyList(),
    val priceMode:String="instant_sell",val onlinePrices:Boolean=false,val persistence:String="session",
    val activityMode:String="movement",val countSacks:Boolean=true,val skyblockOnly:Boolean=true,
    val maxRows:Int=10,val background:Boolean=true,val colorful:Boolean=true,val compactValues:Boolean=false) {
    fun validate() {
        hud.validate();require(items.size<=32 && items.map {it.key()}.distinct().size==items.size) {"Use up to 32 uniquely named tracked items"}
        val explicit=items.map {it.itemId.uppercase(Locale.ROOT)}.filter {it.isNotBlank()}
        require(explicit.distinct().size==explicit.size) {"Do not track the same explicit item ID twice"}
        items.forEach {require(it.name.isNotBlank() && it.name.length<=80 && it.name.none {c->c.isISOControl() || c=='§'})
            require(it.itemId.isEmpty() || it.itemId.matches(Regex("[A-Za-z0-9_:;.\\-]{1,128}")))
            require(it.manualPrice.isFinite() && it.manualPrice in 0.0..1.0e12)}
        require(priceMode in listOf("instant_sell","sell_offer","lowest_bin","npc","manual"))
        require(persistence in listOf("session","never","world_change"));require(activityMode in listOf("movement","item"));require(maxRows in 1..16)
    }
}
data class TrackerSaved(val elapsedMillis:Long=0,val counts:Map<String,Long> = emptyMap()) {
    fun validate() {require(elapsedMillis in 0..31_536_000_000_000L && counts.size<=128)
        require(counts.all {(k,v)->k.isNotBlank() && k.length<=80 && k.none {it.isISOControl()} && v in 0..1_000_000_000_000L})}
}
/** Time since the latest activity remains provisional until the next activity or an explicit pause.
 * At exactly 60 seconds the entire idle interval is rolled back. */
class TrackerClock(initialMillis:Long=0) {
    var committedMillis:Long=initialMillis.coerceAtLeast(0);private set
    private var provisional=0L
    private var signal:Long?=null
    private var previous:Long?=null
    private var mode=""
    var paused=true;private set
    val elapsedMillis:Long get()=committedMillis+provisional
    fun freeze() {committedMillis+=provisional;provisional=0;signal=null;previous=null;paused=true}
    fun step(now:Long,eligible:Boolean,activity:Boolean,itemGained:Boolean,itemMode:Boolean) {
        val nextMode=if(itemMode) "item" else "movement"
        if(mode!=nextMode) {freeze();mode=nextMode}
        val old=previous
        if(old!=null && now<old)freeze()
        previous=now
        if(!eligible) {freeze();return}
        val active=if(itemMode)itemGained else activity
        val at=signal
        if(at==null) {
            if(!itemMode || active) {signal=now;paused=false} else paused=true
            return
        }
        val gap=(now-at).coerceAtLeast(0)
        if(gap>=60_000) {provisional=0;paused=true} else {provisional=gap;paused=false}
        if(active) {
            if(!paused)committedMillis+=provisional
            provisional=0;signal=now;paused=false
        }
    }
}
/** Aggregate all carried slots first: moving the same item between slots isn't a gain. */
class TrackerInventoryDelta {
    private var previous:Map<String,Long>?=null
    fun reset() {previous=null}
    fun observe(current:Map<String,Long>,ignore:Boolean):Map<String,Long> {
        val old=previous;previous=current.toMap()
        if(old==null || ignore)return emptyMap()
        return (current.keys+old.keys).associateWith {(current[it] ?: 0)-(old[it] ?: 0)}.filterValues {it!=0L}
    }
}
/** Match inventory<->sack transfers over a short, bounded observation window. */
class TrackerTransferLosses {
    private data class Loss(val key:String,val source:String,var count:Long,val time:Long)
    private val losses=mutableListOf<Loss>()
    fun clear()=losses.clear()
    fun record(key:String,source:String,count:Long,now:Long) {
        prune(now);if(count<=0)return
        if(losses.size>=256)losses.removeAt(0)
        losses.add(Loss(key,source,count,now))
    }
    private fun prune(now:Long) {losses.removeAll {now<it.time || now-it.time>2000 || it.count==0L}}
    fun consume(key:String,gainSource:String,count:Long,now:Long):Long {
        prune(now);var gain=count
        for(loss in losses)if(loss.key==key && loss.source!=gainSource && gain>0) {val used=minOf(gain,loss.count);gain-=used;loss.count-=used}
        return gain
    }
}
object TrackerLogic {
    fun nameKey(name:String)=ChatLogic.plain(name).trim().replace(Regex("\\s+")," ").lowercase(Locale.ROOT)
    private val sack=Regex("""^\s*([+-])([0-9][0-9,]*) (.+?) \([^\r\n]+\)\s*$""")
    /** Detailed native Sacks hover lines, not its aggregate '+N items' summary. */
    fun sackChanges(lines:List<String>):Map<String,Long> = buildMap {
        lines.take(256).forEach {line->
            val m=sack.matchEntire(ChatLogic.plain(line)) ?: return@forEach
            val value=m.groupValues[2].replace(",","").toLongOrNull()?.takeIf {it in 1..1_000_000_000} ?: return@forEach
            val amount=if(m.groupValues[1]=="-")-value else value
            val key=nameKey(m.groupValues[3]);put(key,(get(key) ?: 0)+amount)
        }
    }
    fun unitPrice(item:TrackerItem,mode:String,price:MarketPrice?):Double? {
        if(mode=="manual")return item.manualPrice
        val p=price ?: return null
        fun valid(n:Double)=n.takeIf {it.isFinite() && it>0}
        return when(mode) {
            "instant_sell"->valid(p.sell) ?: valid(p.bin) ?: valid(p.npc)
            "sell_offer"->valid(p.buy) ?: valid(p.bin) ?: valid(p.npc)
            "lowest_bin"->valid(p.bin)
            "npc"->valid(p.npc)
            else->null
        }
    }
    fun perHour(coins:Double,elapsedMillis:Long):Double? = if(elapsedMillis<1000 || !coins.isFinite())null else (coins*3_600_000.0/elapsedMillis).takeIf {it.isFinite()}
    fun duration(ms:Long):String {val n=ms.coerceAtLeast(0)/1000;return "%02d:%02d:%02d".format(Locale.ROOT,n/3600,(n/60)%60,n%60)}
}
object TrackerDataCodec {
    fun encode(accounts:Map<String,TrackerSaved>):String {
        require(accounts.size<=16)
        val root=JsonObject();root.addProperty("version",1)
        root.add("accounts",JsonObject().apply {accounts.forEach {(owner,data)->
            require(owner.matches(Regex("[a-zA-Z0-9_-]{1,64}")));data.validate()
            add(owner,JsonObject().apply {addProperty("elapsedMillis",data.elapsedMillis);add("counts",JsonObject().apply {data.counts.forEach {(k,v)->addProperty(k,v)}})})
        }})
        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }
    fun decode(text:String):Map<String,TrackerSaved> {
        require(text.toByteArray().size<=1_048_576)
        val root=JsonParser.parseString(text).asJsonObject;require(root.get("version")?.asBigDecimal?.intValueExact()==1)
        val accounts=root.getAsJsonObject("accounts");require(accounts.size()<=16)
        return accounts.entrySet().associate {(owner,value)->
            require(owner.matches(Regex("[a-zA-Z0-9_-]{1,64}")))
            val o=value.asJsonObject
            fun number(v:JsonElement):Long {require(v.isJsonPrimitive && v.asJsonPrimitive.isNumber);return v.asBigDecimal.longValueExact()}
            val counts=o.getAsJsonObject("counts");require(counts.size()<=128)
            owner to TrackerSaved(number(o.get("elapsedMillis")),counts.entrySet().associate {(k,v)->k to number(v)}).also {it.validate()}
        }
    }
}
