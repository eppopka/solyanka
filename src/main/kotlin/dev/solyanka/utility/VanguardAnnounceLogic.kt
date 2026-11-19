/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.util.Locale

data class VanguardAnnounceSettings(val message:String="/gc I got Vanguard, write '+' for an invite.",val keyword:String="+") {
    fun announcement():String=message.trim().let {if(it.startsWith("/gc ",true))it.substring(4).trim() else it}
    fun validate() {
        require(message.length<=220 && announcement().isNotBlank() && !announcement().startsWith('/')) {"Use plain text or /gc followed by a message"}
        require(message.none {it.isISOControl() || it=='§'}) {"No formatting or control codes"}
        require(keyword==keyword.trim() && keyword.length in 1..64 && keyword.none {it.isISOControl() || it=='§'}) {"Invite keyword must contain 1–64 characters without surrounding spaces"}
    }
}
/** Exactly 60 seconds, at most three unique requests, no sliding timeout or duplicate retries. */
class VanguardInviteWindow {
    private var opened:Long?=null
    private val invited=linkedSetOf<String>()
    val count:Int get()=invited.size
    fun clear() {opened=null;invited.clear()}
    fun begin(now:Long) {clear();opened=now}
    fun active(now:Long):Boolean=opened?.let {now>=it && now-it<60_000L} ?: false
    fun accept(raw:String,ownName:String,keyword:String,now:Long):String? {
        if(!active(now) || invited.size>=3)return null
        val chat=ChatCommandsLogic.parse(raw) ?: return null
        if(chat.channel!="guild" || chat.text.trim()!=keyword || chat.name.equals(ownName,true))return null
        if(!invited.add(chat.name.lowercase(Locale.ROOT)))return null
        return chat.name
    }
}
