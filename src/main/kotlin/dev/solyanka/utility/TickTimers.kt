/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from Odin TickTimers (c) 2025 odtheking. Original BSD-3-Clause
 * license is reproduced in licenses/Odin-BSD-3-Clause.txt. */
package dev.solyanka.utility

import java.util.Locale

data class TimerText(val prefix:String,val value:String,val color:Int,val prefixColor:Int=0xff55ffff.toInt())
class TickTimers {
    var goldor=-1;private set
    var goldorStart=-1;private set
    var pad=-1;private set
    var lightning=-1;private set
    var py=-1;private set
    var storm=-1;private set
    var freeze=-1;private set
    private var pyTriggered=false
    fun message(text:String) {
        when(ChatLogic.plain(text)) {
            "[BOSS] Goldor: Who dares trespass into my domain?" -> goldor=60
            "The Core entrance is opening!" -> { goldorStart=-1;goldor=-1 }
            "[BOSS] Storm: I should have known that I stood no chance." -> { goldorStart=64;pad=-1;storm=-1 }
            "[BOSS] Storm: Pathetic Maxor, just like expected." -> { pad=20;lightning=520;storm=0 }
            "[BOSS] Storm: ENERGY HEED MY CALL!","[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!" -> if(!pyTriggered) { pyTriggered=true;py=55 }
            "[BOSS] The Professor: Oh? You found my Guardians' one weakness?" -> freeze=206
        }
    }
    /** Only nonzero server-ping IDs count. Never substitute a client 20 Hz timer. */
    fun ping(id:Int,boss:Boolean,s:TimerSettings) {
        if(id==0 || !boss) return
        if(goldor==0 && goldorStart<=0 && s.goldor.enabled) goldor=60
        if(goldorStart>=0) goldorStart--
        if(goldor>=0) goldor--
        if(pad==0 && s.pad.enabled) pad=20
        if(pad>=0) pad--
        if(lightning>=0) lightning--
        if(py>=0) py--
        if(storm>=0 && storm<Int.MAX_VALUE) storm++
        if(freeze>=0) freeze--
    }
    fun reset() { goldor=-1;goldorStart=-1;pad=-1;lightning=-1;py=-1;storm=-1;freeze=-1;pyTriggered=false }
    fun lines(s:TimerSettings):Map<String,TimerText> = buildMap {
        if(s.goldor.enabled) {
            if(goldorStart>=0 && s.startTimer) put("goldor",format(goldorStart,64,"Start:",s,prefixColor=0xff55ff55.toInt()))
            else if(goldor>=0) put("goldor",format(goldor,60,"Tick:",s,prefixColor=0xffaaaaaa.toInt()))
        }
        if(s.pad.enabled && pad>=0) put("pad",format(pad,20,"Pad:",s))
        if(s.lightning.enabled && lightning>=0) put("lightning",format(lightning,520,"Lightning:",s))
        if(s.py.enabled && py>=0) put("py",format(py,55,"PY:",s))
        if(s.storm.enabled && storm>=0) put("storm",format(storm,540,"Storm:",s))
        if(s.freeze.enabled && freeze>=0) put("freeze",if(freeze>100) format(freeze-100,106,"Fire Freeze:",s,prefixColor=0xffaa0000.toInt()) else TimerText("","Use Fire Freeze",0xffaa0000.toInt()))
    }
    companion object {
        fun format(time:Int,max:Int,prefix:String,s:TimerSettings,prefixColor:Int=0xff55ffff.toInt()):TimerText {
            val color=when { time>=max*0.66->0xff55ff55.toInt();time>=max*0.33->0xffffaa00.toInt();else->0xffff5555.toInt() }
            val value=if(s.ticks) "$time"+(if(s.symbols) "t" else "") else String.format(Locale.US,"%.2f",time/20.0)+(if(s.symbols) "s" else "")
            return TimerText(if(s.prefix) "$prefix " else "",value,color,prefixColor)
        }
    }
}
