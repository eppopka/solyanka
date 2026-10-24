/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import kotlin.math.round
import java.math.BigDecimal

data class NumberRange(val min:Double,val max:Double,val step:Double) {
    init { require(min.isFinite() && max.isFinite() && step.isFinite() && min<max && step>0) }
    fun snap(value:Double):Double {
        require(value.isFinite())
        val n=(min+round((value.coerceIn(min,max)-min)/step)*step).coerceIn(min,max)
        return BigDecimal.valueOf(n).setScale(6,java.math.RoundingMode.HALF_UP).toDouble()
    }
    fun label(value:Double)=BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
object SettingsNumbers {
    fun range(section:String,field:String,width:Int,height:Int,current:Double):NumberRange = when {
        field=="x" -> NumberRange(0.0,maxOf(1.0,width-1.0,current),1.0)
        field=="y" -> NumberRange(0.0,maxOf(1.0,height-1.0,current),1.0)
        section=="hud" && field=="scale" -> NumberRange(.5,4.0,.05)
        field=="cursorClearTicks" -> NumberRange(0.0,1200.0,1.0)
        section=="environment" && field=="gradient" -> NumberRange(.1,1.0,.01)
        section=="environment" && field=="aspectRatio" -> NumberRange(.3,3.0,.01)
        section=="environment" && field in setOf("fogStart","fogEnd","fogDensity") -> NumberRange(0.0,1.0,.01)
        section=="waypoints" && field=="titleScale" -> NumberRange(.1,4.0,.05)
        section=="waypoints" && field in setOf("sizeX","sizeY","sizeZ") -> NumberRange(.1,5.0,.05)
        section=="chat" && field=="feedbackLimit" -> NumberRange(0.0,512.0,1.0)
        section=="chat" && field=="lines" -> NumberRange(100.0,5000.0,1.0)
        else -> error("Missing slider range for $section.$field")
    }
}
