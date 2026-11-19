/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

data class TrackerHudSpan(val text:String,val color:Int)
/** Shared by the actual HUD and HUD-editor preview. Never embeds legacy formatting in item names. */
object TrackerHudStyle {
    const val TITLE=0xff79d9ed.toInt()
    const val TIME=0xffb6a2ef.toInt()
    const val QUANTITY=0xff8cdbc0.toInt()
    const val COINS=0xfff2c66d.toInt()
    const val PROFIT=0xff95e2ac.toInt()
    const val MUTED=0xffa1adbd.toInt()
    const val WARNING=0xffeba47f.toInt()
    private val item=Regex("^(.*)( x[0-9,]+)( \\()(.+?)((?: coins)?\\))$")
    fun spans(line:String,baseColor:Int,colorful:Boolean=true):List<TrackerHudSpan> {
        if(!colorful)return listOf(TrackerHudSpan(line,baseColor))
        fun part(s:String,rgb:Int)=TrackerHudSpan(s,(baseColor and -0x1000000) or (rgb and 0xffffff))
        return when {
            line.startsWith("Tracker · ")-> {
                val rest=line.removePrefix("Tracker · ")
                listOf(part("Tracker · ",TITLE),part(rest.substringBefore(" [paused]"),TIME))+
                    if(rest.endsWith(" [paused]"))listOf(part(" [paused]",WARNING)) else emptyList()
            }
            line.startsWith("Total: ")->listOf(part("Total: ",MUTED),part(line.removePrefix("Total: "),COINS))
            line.startsWith("Profit/h*: ")->listOf(part("Profit/h*: ",MUTED),part(line.removePrefix("Profit/h*: "),PROFIT))
            line.startsWith("? = ")->listOf(part(line,WARNING))
            else->item.matchEntire(line)?.let {m->listOf(part(m.groupValues[1],baseColor),part(m.groupValues[2],QUANTITY),
                part(m.groupValues[3],MUTED),part(m.groupValues[4],if(m.groupValues[4]=="?")WARNING else COINS),part(m.groupValues[5],MUTED))}
                ?: listOf(part(line,MUTED))
        }
    }
}
