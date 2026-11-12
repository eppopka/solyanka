/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MusicSessionGuardTest {
  @Test
  fun `stale authorization cleanup cannot clear a replacement request`() {
    val guard = MusicSessionGuard()
    val oldGeneration = guard.advance()
    val oldPending = Any()
    val replacement = Any()
    var pending: Any? = oldPending

    val newGeneration = guard.advance()
    assertTrue(guard.ifCurrent(newGeneration) { pending = replacement })
    assertFalse(
      guard.ifCurrent(oldGeneration) { if (pending === oldPending) pending = null },
    )

    assertSame(replacement, pending)
  }

  @Test
  fun `disconnect invalidation follows an in-flight publication atomically`() {
    val guard = MusicSessionGuard()
    val session = guard.advance()
    var publishedSocket: Any? = null
    var authenticated = false
    val publishing = CountDownLatch(1)
    val resumePublication = CountDownLatch(1)
    val disconnectStarted = CountDownLatch(1)
    val workerFailure = AtomicReference<Throwable?>()

    val worker = thread {
      try {
        guard.ifCurrent(session) {
          publishing.countDown()
          check(resumePublication.await(2, TimeUnit.SECONDS))
          publishedSocket = Any()
          authenticated = true
        }
      } catch (error: Throwable) {
        workerFailure.set(error)
      }
    }
    assertTrue(publishing.await(2, TimeUnit.SECONDS))

    val disconnect = thread {
      disconnectStarted.countDown()
      guard.advance {
        publishedSocket = null
        authenticated = false
      }
    }
    try {
      assertTrue(disconnectStarted.await(2, TimeUnit.SECONDS))
      val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
      while (
        disconnect.isAlive &&
          disconnect.state != Thread.State.BLOCKED &&
          System.nanoTime() < deadline
      ) Thread.yield()
      assertEquals(Thread.State.BLOCKED, disconnect.state)
    } finally {
      resumePublication.countDown()
    }
    worker.join(2_000)
    disconnect.join(2_000)

    assertFalse(worker.isAlive)
    assertFalse(disconnect.isAlive)
    assertSame(null, workerFailure.get())
    assertSame(null, publishedSocket)
    assertFalse(authenticated)
    assertFalse(guard.ifCurrent(session) { authenticated = true })
  }
}
