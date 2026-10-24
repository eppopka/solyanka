/* SPDX-License-Identifier: GPL-3.0-only
 * Stray SkillProgressTracker parsing/table logic adapted from CC0 source. See NOTICE.
 * Strict amounts, bounded input and monotonic expiry are solyanka adaptations. */
package dev.solyanka.utility

import java.util.Locale
import kotlin.math.roundToLong

data class SkillSnapshot(val kind:SkillKind,val current:Long?,val needed:Long?,val level:Int?,val nextLevel:Int?,val percent:Double,val gained:String,val receivedNanos:Long,val maxed:Boolean=false)
class SkillProgress {
    var snapshot:SkillSnapshot?=null;private set
    private val levels=mutableMapOf<SkillKind,Int>()
    private val denominators=mutableMapOf<SkillKind,Long>()
    private val gain=Regex("[+＋]\\s*([\\d,.]+(?:[kmb])?)\\s*(?:[^A-Za-z0-9]+\\s*)?(${SkillKind.NAMES})\\s*\\(([^)]+)\\)",RegexOption.IGNORE_CASE)
    private val ratio=Regex("^([\\d,.]+(?:[kmb])?)\\s*/\\s*([\\d,.]+(?:[kmb])?)$",RegexOption.IGNORE_CASE)
    private val pct=Regex("^([\\d.]+)\\s*%$")
    fun reset() {snapshot=null;levels.clear();denominators.clear()}
    fun remember(kind:SkillKind,level:Int) {if(level in 0..kind.maxLevel) levels[kind]=level}
    fun tab(raw:String) {
        val clean=clean(raw)
        val m=Regex("^[^A-Za-z]*(${SkillKind.NAMES})\\s*(?::\\s*)?(\\d{1,2})(?:\\D.*)?$",RegexOption.IGNORE_CASE).find(clean) ?: return
        val kind=SkillKind.parse(m.groupValues[1]) ?: return
        m.groupValues[2].toIntOrNull()?.let {remember(kind,it)}
    }
    fun menuItem(raw:String) {
        val text=clean(raw)
        val m=Regex("\\b(${SkillKind.NAMES})\\b",RegexOption.IGNORE_CASE).find(text) ?: return
        val kind=SkillKind.parse(m.value) ?: return
        val level=Regex("\\blevel\\s*:?\\s*(\\d{1,2})\\b",RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
        if(level!=null) remember(kind,level) else tab(text.lineSequence().firstOrNull().orEmpty())
    }
    fun overlay(raw:String,now:Long=System.nanoTime()):Boolean {
        val text=clean(raw);val m=gain.find(text) ?: return false
        val kind=SkillKind.parse(m.groupValues[2]) ?: return false
        if(amount(m.groupValues[1])==null) return false
        val gained="+"+m.groupValues[1].replace(",","")
        val progress=m.groupValues[3].trim()
        val result:SkillSnapshot
        if(progress.equals("MAX",true) || progress.equals("MAXED",true)) {
            result=SkillSnapshot(kind,null,null,kind.maxLevel,kind.maxLevel,100.0,gained,now,true)
            remember(kind,kind.maxLevel)
        } else {
            val r=ratio.matchEntire(progress)
            if(r!=null) {
                val current=amount(r.groupValues[1]) ?: return false;val needed=amount(r.groupValues[2]) ?: return false
                if(needed<=0 || current>needed) return false
                val next=SkillXp.next(kind,needed)
                val level=next?.minus(1) ?: levels[kind]
                denominators[kind]=needed;if(level!=null) remember(kind,level)
                result=SkillSnapshot(kind,current,needed,level,next,current*100.0/needed,gained,now)
            } else {
                val value=pct.matchEntire(progress)?.groupValues?.get(1)?.toDoubleOrNull() ?: return false
                if(!value.isFinite() || value !in 0.0..100.0) return false
                val level=levels[kind]
                val needed=if(level!=null) SkillXp.after(kind,level) else denominators[kind]
                val next=if(level!=null && level<kind.maxLevel) level+1 else needed?.let {SkillXp.next(kind,it)}
                result=SkillSnapshot(kind,needed?.let {(value/100.0*it).roundToLong()},needed,level,next,value,gained,now,level==kind.maxLevel)
            }
        }
        snapshot=result;return true
    }
    fun visible(timeoutSeconds:Int,now:Long=System.nanoTime()):SkillSnapshot?=snapshot?.takeIf {timeoutSeconds==0 || now-it.receivedNanos in 0..timeoutSeconds*1_000_000_000L}
    companion object {
        private val amountPattern=Regex("^(?:\\d+|\\d{1,3}(?:,\\d{3})+)(?:\\.\\d+)?[kmb]?$",RegexOption.IGNORE_CASE)
        fun amount(raw:String):Long? {
            if(raw.length>32 || !amountPattern.matches(raw)) return null
            val text=raw.replace(",","").lowercase(Locale.ROOT)
            val multiplier=when(text.lastOrNull()) {'k'->1_000.0;'m'->1_000_000.0;'b'->1_000_000_000.0;else->1.0}
            val number=(if(text.lastOrNull()?.isLetter()==true) text.dropLast(1) else text).toDoubleOrNull() ?: return null
            val result=number*multiplier
            return if(result.isFinite() && result>=0 && result< Long.MAX_VALUE.toDouble()) result.roundToLong() else null
        }
        private fun clean(raw:String)=ChatLogic.plain(raw.take(8192)).replace('\u00a0',' ').replace('\u202f',' ')
        fun progressText(s:SkillSnapshot):String = when {
            s.maxed->"MAXED"
            s.current!=null && s.needed!=null->String.format(Locale.US,"%,d/%,d",s.current,s.needed)
            else->String.format(Locale.US,"%.2f%%",s.percent)
        }
    }
}
