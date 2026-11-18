/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.util.Locale

data class DevTextRun(val text:String,val color:String?=null,val bold:Boolean=false,val italic:Boolean=false,
    val underlined:Boolean=false,val strikethrough:Boolean=false,val obfuscated:Boolean=false,
    val font:String="",val rawStyle:String="")

/** Clipboard diagnostics only. Custom RGB uses conventional hex notation, not vanilla Java § syntax. */
object DevCopyLogic {
    const val DEFAULT_RADIUS=3
    const val MAX_RADIUS=16
    const val MAX_CLIPBOARD_BYTES=4*1024*1024
    private val names=listOf("black","dark_blue","dark_green","dark_aqua","dark_red","dark_purple","gold","gray",
        "dark_gray","blue","green","aqua","red","light_purple","yellow","white")
    private val rgb=listOf("000000","0000aa","00aa00","00aaaa","aa0000","aa00aa","ffaa00","aaaaaa",
        "555555","5555ff","55ff55","55ffff","ff5555","ff55ff","ffff55","ffffff")
    fun validateRadius(radius:Int) {require(radius in 1..MAX_RADIUS) {"Radius must be 1–$MAX_RADIUS blocks"}}
    fun colorCodes(color:String?,marker:Char='§'):String {
        if(color==null)return ""
        val value=color.lowercase(Locale.ROOT)
        val index=names.indexOf(value).takeIf {it>=0} ?: rgb.indexOf(value.removePrefix("#"))
        if(index>=0)return "$marker${index.toString(16)}"
        if(value.matches(Regex("#[0-9a-f]{6}")))return "$marker"+"x"+value.drop(1).map {"$marker$it"}.joinToString("")
        return ""
    }
    fun legacy(runs:List<DevTextRun>,marker:Char='§'):String=buildString {
        for(run in runs) {
            if(run.text.isEmpty())continue
            append(marker).append('r');append(colorCodes(run.color,marker))
            if(run.obfuscated)append(marker).append('k')
            if(run.bold)append(marker).append('l')
            if(run.strikethrough)append(marker).append('m')
            if(run.underlined)append(marker).append('n')
            if(run.italic)append(marker).append('o')
            append(run.text)
        }
        if(isNotEmpty())append(marker).append('r')
    }
    fun boundedText(text:String,limit:Int=16384):String=if(text.length<=limit)text else text.take(limit)+"…[truncated]"
}
