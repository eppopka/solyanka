/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from Odin PlayerDisplay/ActionBarListener (c) 2025 odtheking,
 * BSD-3-Clause; full original license is bundled. */
package dev.solyanka.utility

import java.util.Locale
import kotlin.math.floor

class PlayerStats {
    var health=0;private set
    var maxHealth=0;private set
    var mana=0;private set
    var maxMana=0;private set
    var overflow=0;private set
    var defense=0;private set
    var vitality=0;private set
    var maxVitality=0;private set
    var vitalityShown=false;private set
    var speed=0;private set
    val ehp:Long get()=health.toLong()*(1+defense/100) // Retain Odin's integer-defense formula.
    fun overlay(message:String) {
        val text=ChatLogic.plain(message)
        fun number(s:String)=s.replace(",","").toDoubleOrNull()?.takeIf { it.isFinite() && it>=0 && it<=Int.MAX_VALUE }?.toInt()
        healthRegex.find(text)?.let { m -> number(m.groupValues[1])?.let { health=it };number(m.groupValues[2])?.let { maxHealth=it } }
        manaRegex.find(text)?.let { m -> number(m.groupValues[1])?.let { mana=it };number(m.groupValues[2])?.let { maxMana=it } }
        overflowRegex.find(text)?.let { m -> number(m.groupValues[1])?.let { overflow=it } }
        defenseRegex.find(text)?.let { m -> number(m.groupValues[1])?.let { defense=it } }
        val v=vitalityRegex.find(text);vitalityShown=v!=null
        v?.let { m -> number(m.groupValues[1])?.let { vitality=it };number(m.groupValues[2])?.let { maxVitality=it } }
    }
    fun tick(playerHealth:Double,playerMaxHealth:Double,movementSpeed:Double) {
        if(playerMaxHealth>0 && playerHealth.isFinite()) health=(maxHealth*playerHealth/playerMaxHealth).coerceIn(0.0,Int.MAX_VALUE.toDouble()).toInt()
        speed=if(movementSpeed.isFinite()) floor(movementSpeed*1000).coerceIn(0.0,Int.MAX_VALUE.toDouble()).toInt() else 0
    }
    fun reset() { health=0;maxHealth=0;mana=0;maxMana=0;overflow=0;defense=0;vitality=0;maxVitality=0;vitalityShown=false;speed=0 }
    fun lines(s:PlayerDisplaySettings):Map<String,String> = buildMap {
        fun n(value:Long)=String.format(Locale.US,"%,d",value)
        fun single(value:Long,icon:String,hideZero:Boolean=false)=if(hideZero && value==0L) "" else n(value)+(if(s.icons) icon else "")
        fun pair(current:Int,max:Int,icon:String)=n(current.toLong())+"/"+n(max.toLong())+(if(s.icons) icon else "")
        if(s.health.enabled && health!=0 && maxHealth!=0) put("health",pair(health,maxHealth,"❤"))
        if(s.mana.enabled && maxMana!=0 && !(mana==0 && s.separateOverflow)) {
            val extra=if(!s.separateOverflow && s.overflow.enabled) single(overflow.toLong(),"ʬ",s.hideZeroOverflow) else ""
            put("mana",pair(mana,maxMana,"✎")+(if(extra.isEmpty()) "" else " $extra"))
        }
        if(s.overflow.enabled && s.separateOverflow) single(overflow.toLong(),"ʬ",s.hideZeroOverflow).takeIf { it.isNotEmpty() }?.let { put("overflow",it) }
        if(s.defense.enabled && defense!=0) put("defense",single(defense.toLong(),"❈",true))
        if(s.vitality.enabled && vitalityShown && maxVitality!=0) put("vitality",pair(vitality,maxVitality,"♨"))
        if(s.ehp.enabled && ehp!=0L) put("ehp",single(ehp,"",true))
        if(s.speed.enabled && speed!=0) put("speed",single(speed.toLong(),"✦",true))
    }
    companion object {
        // PUA SkyBlock glyphs plus legacy equivalent icons for server/resource-pack compatibility.
        val healthRegex=Regex("""([\d,]+)/([\d,]+)[\uE010❤]""")
        val manaRegex=Regex("""([\d,]+)/([\d,]+)[\uE003✎](?: Mana)?""")
        val overflowRegex=Regex("""([\d,]+)[\uE017ʬ]""")
        val defenseRegex=Regex("""([\d,]+)[\uE008❈](?: Defense)?""")
        val vitalityRegex=Regex("""([\d.,]+)/([\d.,]+)[\uE028♨]""")
        fun strip(text:String,s:PlayerDisplaySettings):String {
            var result=ChatLogic.plain(text)
            if(s.hideHealth) result=healthRegex.replace(result,"")
            if(s.hideMana) result=manaRegex.replace(result,"")
            if(s.hideOverflow) result=overflowRegex.replace(result,"")
            if(s.hideDefense) result=defenseRegex.replace(result,"")
            if(s.hideVitality) result=vitalityRegex.replace(result,"")
            return result.trim()
        }
    }
}
