/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LrclibRetryPolicyTest {
    private val policy=LrclibRetryPolicy
    private val now=1_000_000L

    @Test fun `GET miss and unsynced success fall through to search immediately`() {
        val notFound=policy.classifyHttp(404,LrclibRetryPolicy.Endpoint.GET,false,now)
        val noSyncedLyrics=policy.classifyHttp(200,LrclibRetryPolicy.Endpoint.GET,false,now)

        assertEquals(LrclibRetryPolicy.Outcome.LOOKUP_MISS,notFound.outcome)
        assertTrue(notFound.shouldSearchFallback)
        assertNull(notFound.nextAttemptAtMillis)
        assertEquals(LrclibRetryPolicy.Outcome.LOOKUP_MISS,noSyncedLyrics.outcome)
        assertTrue(noSyncedLyrics.shouldSearchFallback)
    }

    @Test fun `successful empty search is deferred instead of retried every refresh`() {
        val decision=policy.classifyHttp(200,LrclibRetryPolicy.Endpoint.SEARCH,false,now)

        assertEquals(LrclibRetryPolicy.Outcome.NO_RESULT,decision.outcome)
        assertEquals(now+policy.NO_RESULT_COOLDOWN_MILLIS,decision.nextAttemptAtMillis)
        assertFalse(decision.canAttemptAt(now+policy.NO_RESULT_COOLDOWN_MILLIS-1))
        assertTrue(decision.canAttemptAt(now+policy.NO_RESULT_COOLDOWN_MILLIS))
    }

    @Test fun `synced lyrics found on either endpoint have no retry schedule`() {
        for(endpoint in LrclibRetryPolicy.Endpoint.entries) {
            val decision=policy.classifyHttp(200,endpoint,true,now)
            assertEquals(LrclibRetryPolicy.Outcome.FOUND,decision.outcome)
            assertNull(decision.nextAttemptAtMillis)
            assertFalse(decision.shouldSearchFallback)
        }
    }

    @Test fun `429 honors server delays without shortening and saturates overflow`() {
        val seconds=policy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"120")
        val date=policy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"Thu, 01 Jan 1970 00:17:00 GMT")
        val longDate=policy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"Fri, 02 Jan 1970 00:00:00 GMT")
        val tooLong=policy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"999999999999999999999999")
        val invalid=policy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"not-a-date")

        assertEquals(LrclibRetryPolicy.Outcome.RATE_LIMITED,seconds.outcome)
        assertEquals(now+120_000L,seconds.nextAttemptAtMillis)
        assertEquals(now+20_000L,date.nextAttemptAtMillis)
        assertEquals(now+85_400_000L,longDate.nextAttemptAtMillis)
        assertEquals(Long.MAX_VALUE,tooLong.nextAttemptAtMillis)
        assertEquals(now+policy.DEFAULT_RATE_LIMIT_MILLIS,invalid.nextAttemptAtMillis)
    }

    @Test fun `transient HTTP and network failures schedule bounded exponential retries`() {
        val first=policy.classifyHttp(503,LrclibRetryPolicy.Endpoint.SEARCH,false,now)
        val later=policy.classifyHttp(500,LrclibRetryPolicy.Endpoint.SEARCH,false,now,attempt=3)
        val capped=policy.networkFailure(now,attempt=20)
        val unavailable=policy.networkFailure(now)

        assertEquals(LrclibRetryPolicy.Outcome.TRANSIENT_FAILURE,first.outcome)
        assertEquals(now+policy.BASE_FAILURE_DELAY_MILLIS,first.nextAttemptAtMillis)
        assertEquals(LrclibRetryPolicy.Outcome.TRANSIENT_FAILURE,later.outcome)
        assertEquals(now+120_000L,later.nextAttemptAtMillis)
        assertEquals(now+policy.MAX_FAILURE_DELAY_MILLIS,capped.nextAttemptAtMillis)
        assertEquals(now+policy.BASE_FAILURE_DELAY_MILLIS,unavailable.nextAttemptAtMillis)
    }

    @Test fun `client errors other than lookup miss are not retried`() {
        val decision=policy.classifyHttp(401,LrclibRetryPolicy.Endpoint.SEARCH,false,now)

        assertEquals(LrclibRetryPolicy.Outcome.PERMANENT_FAILURE,decision.outcome)
        assertNull(decision.nextAttemptAtMillis)
        assertFalse(decision.shouldSearchFallback)
    }
}
