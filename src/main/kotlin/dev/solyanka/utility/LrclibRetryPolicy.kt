/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.math.BigInteger
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Classifies one LRCLIB response and provides its next allowed retry time. */
object LrclibRetryPolicy {
    enum class Endpoint { GET, SEARCH }

    enum class Outcome {
        FOUND,
        LOOKUP_MISS,
        NO_RESULT,
        TRANSIENT_FAILURE,
        RATE_LIMITED,
        PERMANENT_FAILURE,
    }

    data class Decision(val outcome:Outcome,val nextAttemptAtMillis:Long?=null) {
        val shouldSearchFallback:Boolean get()=outcome==Outcome.LOOKUP_MISS
        fun canAttemptAt(nowMillis:Long):Boolean = nextAttemptAtMillis?.let {nowMillis>=it} ?: false
    }

    const val NO_RESULT_COOLDOWN_MILLIS=5*60*1000L
    const val DEFAULT_RATE_LIMIT_MILLIS=60*1000L
    const val MAX_RATE_LIMIT_MILLIS=60*60*1000L
    const val BASE_FAILURE_DELAY_MILLIS=15*1000L
    const val MAX_FAILURE_DELAY_MILLIS=5*60*1000L

    /** `GET` misses are expected and should immediately fall through to `SEARCH`. */
    fun classifyHttp(
        statusCode:Int,
        endpoint:Endpoint,
        hasSyncedLyrics:Boolean,
        nowMillis:Long,
        retryAfterHeader:String?=null,
        attempt:Int=0,
    ):Decision = when {
        statusCode==429 -> scheduled(
            Outcome.RATE_LIMITED,nowMillis,retryAfterDelay(retryAfterHeader,nowMillis),
        )
        statusCode in 200..299 && hasSyncedLyrics -> Decision(Outcome.FOUND)
        statusCode in 200..299 && endpoint==Endpoint.GET -> Decision(Outcome.LOOKUP_MISS)
        statusCode==404 && endpoint==Endpoint.GET -> Decision(Outcome.LOOKUP_MISS)
        statusCode in 200..299 || (statusCode==404 && endpoint==Endpoint.SEARCH) -> scheduled(
            Outcome.NO_RESULT,nowMillis,NO_RESULT_COOLDOWN_MILLIS,
        )
        statusCode==408 || statusCode==425 || statusCode in 500..599 -> transientFailure(nowMillis,attempt)
        else -> Decision(Outcome.PERMANENT_FAILURE)
    }

    /** Use when a request fails before an HTTP response is received. */
    fun networkFailure(nowMillis:Long,attempt:Int=0):Decision = transientFailure(nowMillis,attempt)

    private fun transientFailure(nowMillis:Long,attempt:Int):Decision {
        var delay=BASE_FAILURE_DELAY_MILLIS
        repeat(attempt.coerceIn(0,10)) {
            delay=(delay*2).coerceAtMost(MAX_FAILURE_DELAY_MILLIS)
        }
        return scheduled(Outcome.TRANSIENT_FAILURE,nowMillis,delay)
    }

    private fun retryAfterDelay(header:String?,nowMillis:Long):Long {
        val value=header?.trim().orEmpty()
        val millis=value.toBigIntegerOrNull()?.let {seconds->
            if(seconds.signum()<0) DEFAULT_RATE_LIMIT_MILLIS
            else seconds.multiply(BigInteger.valueOf(1000)).coerceAtMost(BigInteger.valueOf(MAX_RATE_LIMIT_MILLIS)).toLong()
        } ?: runCatching {
            val target=ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            Math.subtractExact(target,nowMillis).coerceAtLeast(0L)
        }.getOrElse {DEFAULT_RATE_LIMIT_MILLIS}
        return millis.coerceIn(1_000L,MAX_RATE_LIMIT_MILLIS)
    }

    private fun scheduled(outcome:Outcome,nowMillis:Long,delayMillis:Long):Decision = Decision(
        outcome,
        if(nowMillis>Long.MAX_VALUE-delayMillis) Long.MAX_VALUE else nowMillis+delayMillis,
    )
}
