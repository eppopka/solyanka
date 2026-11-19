/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

object UiHelp {
    fun summary(text:String):String {
        if(text.length<=110)return text
        if(text.startsWith("Windows only. Captures ONLY"))return "Windows only. Selected window capture; local, no uploads or desktop capture."
        val ends=Regex("[.!?](?:\\s|$)").findAll(text).map {it.range.first+1}.filter {it>=30 && it<=110}.toList()
        if(ends.isNotEmpty())return text.substring(0,ends.first())
        val short=text.take(106)
        return short.substringBeforeLast(' ',short)+"…"
    }
}
