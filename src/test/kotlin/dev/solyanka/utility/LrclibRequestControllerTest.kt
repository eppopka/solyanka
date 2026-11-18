/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class LrclibRequestControllerTest {
    private val controller=LrclibRequestController()
    private val now=1_000_000L

    @Test fun `repeated renders enqueue a lookup only once until it completes`() {
        val ticket=assertNotNull(controller.refresh("track-a",true,now))
        repeat(20) {assertNull(controller.refresh("track-a",true,now+it))}

        val miss=LrclibRetryPolicy.classifyHttp(404,LrclibRetryPolicy.Endpoint.GET,false,now)
        assertTrue(controller.complete(ticket,miss))
        assertNull(controller.refresh("track-a",true,now))

        val empty=LrclibRetryPolicy.classifyHttp(200,LrclibRetryPolicy.Endpoint.SEARCH,false,now)
        assertTrue(controller.complete(ticket,empty))
        assertNull(controller.refresh("track-a",true,now+LrclibRetryPolicy.NO_RESULT_COOLDOWN_MILLIS-1))
        assertNotNull(controller.refresh("track-a",true,now+LrclibRetryPolicy.NO_RESULT_COOLDOWN_MILLIS))
    }

    @Test fun `track change invalidates old completion without clearing new lookup`() {
        val old=assertNotNull(controller.refresh("track-a",true,now))
        val current=assertNotNull(controller.refresh("track-b",true,now))
        assertNotSame(old,current)

        val found=LrclibRetryPolicy.classifyHttp(200,LrclibRetryPolicy.Endpoint.SEARCH,true,now)
        assertFalse(controller.complete(old,found))
        assertNull(controller.refresh("track-b",true,now))
        assertTrue(controller.complete(current,found))
        assertNull(controller.refresh("track-b",true,now))
    }

    @Test fun `disabling invalidates in-flight completion before same track is enabled again`() {
        val old=assertNotNull(controller.refresh("track-a",true,now))
        assertNull(controller.refresh("track-a",false,now))
        val current=assertNotNull(controller.refresh("track-a",true,now))
        val found=LrclibRetryPolicy.classifyHttp(200,LrclibRetryPolicy.Endpoint.SEARCH,true,now)

        assertFalse(controller.complete(old,found))
        assertNull(controller.refresh("track-a",true,now))
        assertTrue(controller.complete(current,found))
    }

    @Test fun `rate limiting persists when tracks or enabled state change`() {
        val ticket=assertNotNull(controller.refresh("track-a",true,now))
        val limited=LrclibRetryPolicy.classifyHttp(429,LrclibRetryPolicy.Endpoint.SEARCH,false,now,"120")
        assertTrue(controller.complete(ticket,limited))
        assertTrue(controller.isRateLimited(now+1))
        assertNull(controller.refresh("track-b",true,now+1))
        assertNull(controller.refresh(null,false,now+2))
        assertNull(controller.refresh("track-b",true,now+119_999))
        assertFalse(controller.isRateLimited(now+120_000))
        assertNotNull(controller.refresh("track-b",true,now+120_000))
    }

    @Test fun `stale requests cannot publish lyrics after a track change`() {
        val old=assertNotNull(controller.refresh("track-a",true,now))
        val current=assertNotNull(controller.refresh("track-b",true,now))
        val found=LrclibRetryPolicy.classifyHttp(200,LrclibRetryPolicy.Endpoint.SEARCH,true,now)
        var published=""
        assertFalse(controller.complete(old,found) {published="old"})
        assertTrue(controller.complete(current,found) {published="new"})
        assertTrue(published=="new")
    }
}
