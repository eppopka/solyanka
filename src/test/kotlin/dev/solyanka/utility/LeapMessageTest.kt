/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LeapMessageTest {
  @TempDir lateinit var dir: Path

  @Test
  fun `accept only exact server confirmation`() {
    assertEquals("player123", LeapMessageLogic.target("You have teleported to player123!"))
    assertNull(LeapMessageLogic.target("Party > player: You have teleported to player123!"))
    assertNull(LeapMessageLogic.target("You have teleported to invalid-name!"))
  }

  @Test
  fun `counts persist and are case insensitive`() {
    val path = dir.resolve("leaps.json")
    val store = LeapCountStore(path)
    store.load()
    assertEquals(1L, store.increment("Player"))
    assertEquals(2L, store.increment("player"))
    val reload = LeapCountStore(path)
    reload.load()
    assertEquals(3L, reload.increment("PLAYER"))
    assertEquals("Player 3", LeapMessageLogic.format("{name} {leapcount}", "Player", 3))
  }

  @Test
  fun `corrupt counts are not overwritten`() {
    val path = dir.resolve("leaps.json")
    Files.writeString(path, "bad")
    val store = LeapCountStore(path)
    store.load()
    assertNotNull(store.error)
    assertFails { store.increment("Player") }
    assertEquals("bad", Files.readString(path))
  }
}
