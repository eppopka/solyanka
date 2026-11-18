/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

/** Prevents duplicate lyric lookups and rejects completions for obsolete tracks. */
class LrclibRequestController {
    class Ticket internal constructor(val trackKey:String)

    private var activeTrack:String?=null
    private var enabled=false
    private var inFlight:Ticket?=null
    private var retryAtMillis:Long?=null
    private var rateLimitUntilMillis=0L
    private var finished=false

    /** Returns a ticket only when a new lookup may be submitted. */
    @Synchronized fun refresh(trackKey:String?,isEnabled:Boolean,nowMillis:Long):Ticket? {
        if(!isEnabled || trackKey.isNullOrBlank()) {
            invalidate()
            return null
        }
        if(!enabled || activeTrack!=trackKey) {
            invalidate()
            enabled=true
            activeTrack=trackKey
        }
        if(inFlight!=null || finished || nowMillis<rateLimitUntilMillis || retryAtMillis?.let {nowMillis<it}==true) return null
        retryAtMillis=null
        return Ticket(trackKey).also {inFlight=it}
    }

    /**
     * Completes the current lookup. A GET miss keeps its ticket active so the
     * caller can search before finishing the same lookup operation.
     */
    @Synchronized fun complete(ticket:Ticket,decision:LrclibRetryPolicy.Decision,publish:()->Unit = {}):Boolean {
        if(!enabled || activeTrack!=ticket.trackKey || inFlight!==ticket) return false
        if(decision.shouldSearchFallback) return true
        if(decision.outcome==LrclibRetryPolicy.Outcome.RATE_LIMITED) {
            rateLimitUntilMillis=decision.nextAttemptAtMillis ?: rateLimitUntilMillis
        }
        inFlight=null
        retryAtMillis=decision.nextAttemptAtMillis
        finished=retryAtMillis==null
        publish()
        return true
    }

    @Synchronized fun isRateLimited(nowMillis:Long):Boolean = nowMillis<rateLimitUntilMillis

    @Synchronized fun clear() {invalidate()}

    @Synchronized private fun invalidate() {
        enabled=false
        activeTrack=null
        inFlight=null
        retryAtMillis=null
        finished=false
    }
}
