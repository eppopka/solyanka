/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

object TrackerValues {
    fun format(value:Double?,compact:Boolean):String {
        if(value==null || !value.isFinite() || value<0)return "?"
        if(!compact)return PriceLogic.format(value)+" coins"
        val (divisor,suffix)=when {value>=1e9->1e9 to "b";value>=1e6->1e6 to "m";value>=1e3->1e3 to "k";else->1.0 to ""}
        return java.math.BigDecimal.valueOf(value).divideToIntegralValue(java.math.BigDecimal.valueOf(divisor)).toBigInteger().toString()+suffix
    }
}
