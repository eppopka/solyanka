/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Serializes session invalidation with state changes made by its worker. */
class MusicSessionGuard {
    private val lock=Any()
    private var generation=0

    fun current():Int = synchronized(lock) {generation}

    fun advance(onAdvance:(Int)->Unit = {}):Int = synchronized(lock) {
        val next=generation+1
        generation=next
        onAdvance(next)
        next
    }

    fun ifCurrent(expected:Int,action:()->Unit):Boolean = synchronized(lock) {
        if(expected!=generation) false else {action();true}
    }
}
