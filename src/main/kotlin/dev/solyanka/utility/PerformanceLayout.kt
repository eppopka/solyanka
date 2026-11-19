/* SPDX-License-Identifier: GPL-3.0-only; Odin PerformanceHUD adaptation, BSD-3-Clause, see NOTICE. */
package dev.solyanka.utility

data class PerformanceMetric(val label:String,val value:String)
object PerformanceLayout {
    fun metrics(s:PerformanceSettings,fps:Int,ping:String,tps:Double?):List<PerformanceMetric> = buildList {
        if(s.showTps)add(PerformanceMetric("TPS: ",tps?.takeIf {it.isFinite()}?.let {String.format(java.util.Locale.ROOT,"%.1f",it.coerceIn(0.0,20.0))} ?: "—"))
        if(s.showFps)add(PerformanceMetric("FPS: ",fps.toString()))
        if(s.showPing)add(PerformanceMetric("Ping: ",ping))
    }
    fun lines(s:PerformanceSettings,metrics:List<PerformanceMetric>):List<String> =
        if(s.direction=="vertical")metrics.map {it.label+it.value} else listOf(metrics.joinToString(" | ") {it.label+it.value})
}
