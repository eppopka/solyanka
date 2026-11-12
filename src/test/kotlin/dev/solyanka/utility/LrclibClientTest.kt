/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class LrclibClientTest {
    private val now=1_000_000L

    @Test fun `exact lookup success parses lyrics and encodes query values`() {
        val rawQuery=AtomicReference<String>()
        withServer({exchange->
            rawQuery.set(exchange.requestURI.rawQuery)
            respond(exchange,200,"""{"syncedLyrics":"[00:01.00]Hello"}""")
        }) {base->
            val result=client(base).fetch("Song & Title","A+B",213.4,now)

            assertEquals("track_name=Song+%26+Title&artist_name=A%2BB&duration=213",rawQuery.get())
            assertEquals(LrclibRetryPolicy.Outcome.FOUND,result.decision.outcome)
            assertEquals(listOf(LyricLine(1.0,"Hello")),result.lines)
            assertEquals(null,result.decision.nextAttemptAtMillis)
        }
    }

    @Test fun `GET 404 falls back to search and returns parsed synced lyrics`() {
        val paths=CopyOnWriteArrayList<String>()
        withServer({exchange->
            paths+=exchange.requestURI.path
            if(exchange.requestURI.path=="/api/get") respond(exchange,404,"missing")
            else respond(exchange,200,"""[{"syncedLyrics":"[00:02.25]Search result"}]""")
        }) {base->
            val result=client(base).fetch("Track","Artist",180.0,now)

            assertEquals(listOf("/api/get","/api/search"),paths)
            assertEquals(LrclibRetryPolicy.Outcome.FOUND,result.decision.outcome)
            assertEquals(listOf(LyricLine(2.25,"Search result")),result.lines)
        }
    }

    @Test fun `429 honors Retry After and does not issue search fallback`() {
        val paths=CopyOnWriteArrayList<String>()
        withServer({exchange->
            paths+=exchange.requestURI.path
            exchange.responseHeaders.add("Retry-After","7200")
            respond(exchange,429,"limited")
        }) {base->
            val result=client(base).fetch("Track","Artist",180.0,now)

            assertEquals(listOf("/api/get"),paths)
            assertEquals(LrclibRetryPolicy.Outcome.RATE_LIMITED,result.decision.outcome)
            assertEquals(now+7_200_000L,result.decision.nextAttemptAtMillis)
            assertTrue(result.lines.isEmpty())
        }
    }

    @Test fun `malformed successful body is a transient failure`() = withServer({exchange->
        respond(exchange,200,"not json")
    }) {base->
        val result=client(base).fetch("Track","Artist",0.0,now)

        assertEquals(LrclibRetryPolicy.Outcome.TRANSIENT_FAILURE,result.decision.outcome)
        assertEquals(now+LrclibRetryPolicy.BASE_FAILURE_DELAY_MILLIS,result.decision.nextAttemptAtMillis)
        assertTrue(result.lines.isEmpty())
    }

    @Test fun `search 503 is classified transient after expected GET miss`() {
        val paths=CopyOnWriteArrayList<String>()
        withServer({exchange->
            paths+=exchange.requestURI.path
            if(exchange.requestURI.path=="/api/get") respond(exchange,404,"missing")
            else respond(exchange,503,"temporarily unavailable")
        }) {base->
            val result=client(base).fetch("Track","Artist",180.0,now)

            assertEquals(listOf("/api/get","/api/search"),paths)
            assertEquals(LrclibRetryPolicy.Outcome.TRANSIENT_FAILURE,result.decision.outcome)
            assertEquals(now+LrclibRetryPolicy.BASE_FAILURE_DELAY_MILLIS,result.decision.nextAttemptAtMillis)
        }
    }

    @Test fun `oversized response is bounded and treated as transient`() = withServer({exchange->
        respond(exchange,200," ".repeat(LrclibClient.MAX_RESPONSE_BYTES+1))
    }) {base->
        val result=client(base).fetch("Track","Artist",0.0,now)
        assertEquals(LrclibRetryPolicy.Outcome.TRANSIENT_FAILURE,result.decision.outcome)
        assertTrue(result.lines.isEmpty())
    }

    @Test fun `interruption is propagated and interrupt status restored`() = withServer({exchange->
        respond(exchange,200,"[]")
    }) {base->
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> {client(base).fetch("Track","Artist",0.0,now)}
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {Thread.interrupted()}
    }

    private fun client(base:URI)=LrclibClient(
        httpClient=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
        baseUri=base,
        requestTimeout=Duration.ofSeconds(2),
    )

    private fun withServer(handler:HttpHandler,block:(URI)->Unit) {
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/",handler)
        server.start()
        try {block(URI("http://127.0.0.1:${server.address.port}"))}
        finally {server.stop(0)}
    }

    private fun respond(exchange:HttpExchange,status:Int,body:String) {
        val bytes=body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(status,bytes.size.toLong())
        exchange.responseBody.use {it.write(bytes)}
    }
}
