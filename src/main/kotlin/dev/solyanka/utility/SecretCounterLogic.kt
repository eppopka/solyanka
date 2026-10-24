/* SPDX-License-Identifier: GPL-3.0-only; user-provided impl.zip SecretCounter adaptation.
 * Run lifecycle markers reference pinned NoammAddons CC0; see NOTICE. */
package dev.solyanka.utility

data class SecretCounterSettings(val autoAnnounce:Boolean=false,val channel:String="current") {
    fun validate() {require(channel in listOf("current","party"))}
}
object DungeonRunMessages {
    private const val START="[NPC] Mort: Here, I found this map when I first entered the dungeon."
    private val end=Regex("""^\s*(?:Master Mode )?(?:The )?Catacombs - (?:Floor (?:VII|VI|IV|III|II|V|I|[1-7])|Entrance)\s*$""")
    fun started(text:String)=ChatLogic.plain(text).trim()==START
    fun ended(text:String)=end.matches(ChatLogic.plain(text))
}
/** One request and one personal result per run, without API estimates or team-stat parsing. */
class SecretCounterState {
    enum class Action { REQUEST_STATS, TIMEOUT }
    private var finished=false
    private var requestStartedAt:Long?=null
    private var awaitingStartedAt:Long?=null
    private val secrets=Regex("""^\s*Secrets Found:\s*([0-9]+(?:,[0-9]{3})*)\s*$""")
    fun finish(now:Long) {if(finished) return;finished=true;requestStartedAt=now}
    fun poll(now:Long):Action? {
        requestStartedAt?.let {if(elapsed(now,it)>=1000) {requestStartedAt=null;awaitingStartedAt=now;return Action.REQUEST_STATS}}
        awaitingStartedAt?.let {if(elapsed(now,it)>=10000) {awaitingStartedAt=null;return Action.TIMEOUT}}
        return null
    }
    fun readSecrets(text:String,now:Long):Int? {
        val begin=awaitingStartedAt ?: return null
        if(elapsed(now,begin)>=10000) return null
        val count=secrets.matchEntire(ChatLogic.plain(text))?.groupValues?.get(1)?.replace(",","")?.toIntOrNull() ?: return null
        awaitingStartedAt=null
        return count
    }
    fun reset() {finished=false;requestStartedAt=null;awaitingStartedAt=null}
    companion object {fun elapsed(now:Long,begin:Long)=(now-begin).coerceAtLeast(0)}
}
/** Cancels on reset/toggle; latest settings choose the channel at dispatch time. */
class SecretAnnouncement {
    private var pending:Pair<Long,Int>?=null
    fun schedule(now:Long,count:Int,enabled:Boolean) {pending=if(enabled) now to count else null}
    fun poll(now:Long,enabled:Boolean):Int? {
        val p=pending ?: return null
        if(!enabled) {pending=null;return null}
        if(SecretCounterState.elapsed(now,p.first)<500) return null
        pending=null;return p.second
    }
    fun reset() {pending=null}
}
object AlwaysWaterPolicy {
    fun rain(enabled:Boolean,original:Float)=if(enabled) 1f else original
    // Extraction clamps every use against camera Y/radius before making a column/light position.
    // Do not change the world's heightmap or precipitation state.
    fun roofHeight(enabled:Boolean,original:Int)=if(enabled) Int.MIN_VALUE else original
}
