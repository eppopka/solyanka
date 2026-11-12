/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.solyanka.gfx.OverlayLine
import java.nio.file.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ArchiveExpansionTest {
  @TempDir lateinit var temp: Path

  @Test
  fun `names split ignore blanks and case and formatting`() {
    val s = BestiarySettings(names = " , Skeleton , Zombie, ")
    assertTrue(s.matches("§c[Lv 10] ZOMBIE 10 HP"))
    assertFalse(s.matches("Spider"))
  }

  @Test
  fun `exact names do not match substrings`() {
    assertFalse(BestiarySettings(names = "Zombie", exact = true).matches("Zombie Knight"))
    assertTrue(BestiarySettings(names = "Zombie", exact = true).matches("§aZOMBIE"))
  }

  @Test
  fun `empty term list never matches`() {
    assertFalse(BestiarySettings().matches("Zombie"))
    assertFalse(BestiarySettings(names = ", ,").matches(""))
  }

  @Test
  fun `bestiary validates input bounds`() {
    assertFails { BestiarySettings(names = "x".repeat(2049)).validate() }
    assertFails { BestiarySettings(range = 101).validate() }
    assertFails { BestiarySettings(names = "x\ny").validate() }
  }

  @Test
  fun `three observations remain unconfirmed`() {
    val b = BugEvidence()
    repeat(3) { b.observe(Point3(1.0, 2.0, 3.0)) }
    assertTrue(b.confirmed().isEmpty())
  }

  @Test
  fun `four nearby observations confirm`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(it * 0.05, 2.0, 3.0)) }
    assertEquals(listOf(Point3(0.15, 2.0, 3.0)).size, b.confirmed().size)
    assertTrue(kotlin.math.abs(b.confirmed().single().x - 0.15) < 1e-8)
  }

  @Test
  fun `distant evidence never combines`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(it.toDouble(), 0.0, 0.0)) }
    assertTrue(b.confirmed().isEmpty())
  }

  @Test
  fun `bug expiry is exactly twenty ticks`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(0.0, 0.0, 0.0)) }
    repeat(19) { b.tick() }
    assertEquals(1, b.confirmed().size)
    b.tick()
    assertTrue(b.confirmed().isEmpty())
  }

  @Test
  fun `new observations refresh evidence expiry`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(0.0, 0.0, 0.0)) }
    repeat(19) { b.tick() }
    b.observe(Point3(0.1, 0.0, 0.0))
    b.tick()
    assertEquals(1, b.confirmed().size)
  }

  @Test
  fun `nonfinite evidence ignored`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(Double.NaN, 0.0, 0.0)) }
    assertTrue(b.confirmed().isEmpty())
  }

  @Test
  fun `reset clears confirmed targets`() {
    val b = BugEvidence()
    repeat(4) { b.observe(Point3(0.0, 0.0, 0.0)) }
    b.clear()
    assertTrue(b.confirmed().isEmpty())
  }

  @Test
  fun `cone has 32 faces and 40 outline edges`() {
    val (f, e) = CosmeticMesh.cone(ChinaHatSettings())
    assertEquals(32, f.size)
    assertEquals(40, e.size)
  }

  @Test
  fun `wire cone retains edges without fill`() {
    val (f, e) = CosmeticMesh.cone(ChinaHatSettings(fill = false))
    assertTrue(f.isEmpty())
    assertEquals(40, e.size)
  }

  @Test
  fun `cone vertices obey configured radius and height`() {
    val s = ChinaHatSettings(radius = 0.8, height = 0.7)
    val (f, _) = CosmeticMesh.cone(s)
    assertTrue(f.all { it.a.y == 0.7 && it.b.y == 0.0 })
    assertTrue(f.all { kotlin.math.abs(kotlin.math.hypot(it.b.x, it.b.z) - 0.8) < 1e-8 })
  }

  @Test
  fun `halo shells bounded triangle count`() {
    assertEquals(3 * 48 * 8 * 2, CosmeticMesh.halo(HaloSettings(), 0.0).size)
    assertEquals(3 * 48 * 8 * 2 + 48, CosmeticMesh.halo(HaloSettings(fill = true), 0.0).size)
  }

  @Test
  fun `halo geometry remains finite across time`() {
    listOf(0.0, 1.0, 10000000.0).forEach { t ->
      assertTrue(
        CosmeticMesh.halo(HaloSettings(), t)
          .flatMap { listOf(it.a, it.b, it.c) }
          .all { listOf(it.x, it.y, it.z).all(Double::isFinite) },
      )
    }
  }

  @Test
  fun `shimmer off gives time invariant halo`() {
    assertEquals(
      CosmeticMesh.halo(HaloSettings(shimmer = false), 0.0),
      CosmeticMesh.halo(HaloSettings(shimmer = false), 4.0),
    )
  }

  @Test
  fun `invalid cosmetic settings are rejected`() {
    assertFails { HaloSettings(radius = Double.NaN).validate() }
    assertFails { HaloSettings(opacity = 0.0).validate() }
    assertFails { ChinaHatSettings(width = 7.0).validate() }
    assertFails { TikiSettings(count = 49).validate() }
  }

  @Test
  fun `simon flash duplicates are distinct steps`() {
    val s = SimonLogic()
    s.flash(2, false)
    s.flash(2, false)
    assertEquals(listOf(2, 2), s.pending.map { it.cell })
    assertEquals(listOf(1, 2), s.pending.map { it.number })
  }

  @Test
  fun `simon skip removes and renumbers first step`() {
    val s = SimonLogic()
    repeat(3) { s.flash(it, true) }
    assertEquals(listOf(1, 2), s.pending.map { it.cell })
    assertEquals(listOf(1, 2), s.pending.map { it.number })
  }

  @Test
  fun `simon ready check retains highest stage`() {
    val s = SimonLogic()
    repeat(5) { s.flash(it, false) }
    s.check(true)
    assertEquals(5, s.stage)
    s.check(false)
    s.check(true)
    assertEquals(5, s.stage)
  }

  @Test
  fun `wrong click protection preserves queue`() {
    val s = SimonLogic()
    s.flash(1, false)
    assertTrue(s.click(2, 1, true, false))
    assertEquals(1, s.pending.size)
  }

  @Test
  fun `sneak overrides wrong click protection`() {
    val s = SimonLogic()
    s.flash(1, false)
    assertFalse(s.click(2, 1, true, true))
    assertEquals(1, s.pending.size)
  }

  @Test
  fun `same tick duplicates blocked`() {
    val s = SimonLogic()
    s.flash(1, false)
    s.flash(2, false)
    assertFalse(s.click(1, 5, false, false))
    assertTrue(s.click(2, 5, false, false))
    assertEquals(1, s.pending.size)
  }

  @Test
  fun `normal expected click advances once`() {
    val s = SimonLogic()
    s.flash(1, false)
    s.flash(2, false)
    assertFalse(s.click(1, 5, false, false))
    assertEquals(2, s.pending.single().cell)
  }

  @Test
  fun `source special second button consumes two`() {
    val s = SimonLogic()
    repeat(3) { s.flash(it, false) }
    assertFalse(s.click(1, 2, false, false))
    assertEquals(2, s.pending.single().cell)
  }

  @Test
  fun `simon reset removes skip state and progress`() {
    val s = SimonLogic()
    s.flash(1, false)
    s.check(true)
    s.reset()
    assertEquals(0, s.stage)
    assertTrue(s.pending.isEmpty())
    repeat(3) { s.flash(it, true) }
    assertEquals(2, s.pending.size)
  }

  @Test
  fun `simon evidence bounded against packet flood`() {
    val s = SimonLogic()
    repeat(1000) { s.flash(0, false) }
    assertEquals(32, s.pending.size)
  }

  @Test
  fun `route parses number notes and repeated uses`() {
    assertEquals(listOf(1), RouteLogic.numbers("1 item"))
    assertEquals(listOf(2, 4), RouteLogic.numbers("2 / 4"))
    assertEquals(listOf(2), RouteLogic.numbers("item 2"))
    assertTrue(RouteLogic.numbers("0").isEmpty())
  }

  @Test
  fun `route rejects oversized numbers without overflow`() {
    assertTrue(RouteLogic.numbers("9999999999999999999999").isEmpty())
    assertEquals(listOf(2), RouteLogic.numbers("2/2"))
  }

  @Test
  fun `route orders start then numbered visits`() {
    val ways =
      listOf(
        Waypoint(GridPos(0, 0, 0), title = "2/4"),
        Waypoint(GridPos(1, 0, 0), title = "1 item"),
        Waypoint(GridPos(2, 0, 0), color = 0xffff5555.toInt()),
      )
    assertEquals(listOf(0, 1, 2, 4), RouteLogic.entries(ways).map { it.number })
  }

  @Test
  fun `custom start color takes priority over a numbered title`() {
    val color = 0xff123456.toInt()
    val ways =
      listOf(
        Waypoint(GridPos(0, 0, 0), title = "2"),
        Waypoint(GridPos(1, 0, 0), color = 0x80123456.toInt(), title = "99"),
        Waypoint(GridPos(2, 0, 0), title = "1"),
      )
    assertEquals(listOf(0, 1, 2), RouteLogic.entries(ways, color).map { it.number })
    assertEquals(1, RouteLogic.entries(ways, color).first().waypoint)
  }

  @Test
  fun `activation radius includes its boundary and advances sequentially`() {
    assertTrue(RouteLogic.inRange(16.0, 4.0))
    assertFalse(RouteLogic.inRange(16.01, 4.0))
    val entries = (0..2).map { RouteLogic.Entry(it, it) }
    assertEquals(0, RouteLogic.advance(entries, -1, setOf(0)))
    assertEquals(1, RouteLogic.advance(entries, 0, setOf(1)))
    assertEquals(
      listOf(1 to 2),
      RouteLogic.segments(entries, 1, RouteSettings()).map { it.first.number to it.second.number },
    )
  }

  @Test
  fun `configured route start color round trips and defaults for old configs`() {
    assertEquals(
      0xffff5555.toInt(),
      FeatureCodec.decode(JsonParser.parseString("{}").asJsonObject).route.startColor,
    )
    val f = FeatureSettings(route = RouteSettings(startColor = 0xff123456.toInt()))
    assertEquals(f.route, FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject).route)
  }

  @Test
  fun `route will not skip to repeated future block`() {
    val es = listOf(RouteLogic.Entry(0, 1), RouteLogic.Entry(1, 2), RouteLogic.Entry(0, 3))
    assertEquals(0, RouteLogic.advance(es, 0, setOf(0)))
    assertEquals(1, RouteLogic.advance(es, 0, setOf(1)))
    assertEquals(2, RouteLogic.advance(es, 1, setOf(0)))
  }

  @Test
  fun `route start resets when revisited`() {
    val es = listOf(RouteLogic.Entry(0, 0), RouteLogic.Entry(1, 1), RouteLogic.Entry(2, 2))
    assertEquals(0, RouteLogic.advance(es, 2, setOf(0)))
  }

  @Test
  fun `route lookahead bounds segments`() {
    val es = (0..30).map { RouteLogic.Entry(it, it) }
    assertEquals(2, RouteLogic.segments(es, 3, RouteSettings(ahead = 2)).size)
    assertEquals(30, RouteLogic.segments(es, 3, RouteSettings(follow = false)).size)
  }

  @Test
  fun `invalid route range fails validation`() {
    assertFails { RouteSettings(radius = Double.POSITIVE_INFINITY).validate() }
    assertFails { RouteSettings(ahead = 21).validate() }
  }

  @Test
  fun `pet reads verified JSON xp`() {
    val p =
      PetLogic.read(
        "§6[Lvl 100] Golden Dragon",
        emptyList(),
        """{"exp":123456.0,"uuid":"abc","type":"GOLDEN_DRAGON","tier":"LEGENDARY","active":true}""",
      )!!
    assertEquals(123456.0, p.xp)
    assertTrue(p.active)
    assertEquals("Golden Dragon", p.name)
    assertEquals(100, p.level)
  }

  @Test
  fun `pet never guesses xp from lore or level`() {
    val p = PetLogic.read("[Lvl 50] Sheep", listOf("+100 Farming XP"), null)!!
    assertNull(p.xp)
    assertFalse(p.active)
  }

  @Test
  fun `pet malformed info stays unknown`() {
    assertNull(PetLogic.read("[Lvl 50] Sheep", emptyList(), "broken")!!.xp)
    assertNull(PetLogic.read("Not a pet", emptyList(), "{}"))
  }

  @Test
  fun `pet negative xp never accepted`() {
    assertNull(PetLogic.read("[Lvl 50] Sheep", emptyList(), """{"exp":-1}""")!!.xp)
  }

  @Test
  fun `pet active lore identifies equipped pet`() {
    assertTrue(PetLogic.read("[Lvl 5] Bee", listOf("§eClick to despawn!"), null)!!.active)
  }

  @Test
  fun `pet rates require actual changed snapshots`() {
    val h = PetHistory()
    h.record(0, 100.0, 10)
    h.record(5000, 100.0, 10)
    assertNull(h.hourly())
    h.record(10000, 200.0, 10)
    assertEquals(36000.0, h.hourly())
  }

  @Test
  fun `old pet snapshots expire from rate window`() {
    val h = PetHistory()
    h.record(0, 100.0, 1)
    h.record(10000, 200.0, 1)
    assertNotNull(h.hourly(10000, 1))
    assertNull(h.hourly(80000, 1))
  }

  @Test
  fun `two pet samples outside selected window cannot fabricate current rate`() {
    val h = PetHistory()
    h.record(0, 100.0, 1)
    h.record(120000, 200.0, 1)
    assertNull(h.hourly(120000, 1))
  }

  @Test
  fun `pet xp decrease resets rate history`() {
    val h = PetHistory()
    h.record(0, 100.0, 10)
    h.record(10000, 200.0, 10)
    h.record(20000, 50.0, 10)
    assertNull(h.hourly())
  }

  @Test
  fun `pet clock reversal resets history`() {
    val h = PetHistory()
    h.record(10000, 100.0, 10)
    h.record(1, 101.0, 10)
    assertNull(h.hourly())
  }

  @Test
  fun `hotbar default transform is identity`() {
    val t = HotbarLayout.transform(MoveHotbarSettings(), 960, 540)
    assertEquals(0.0, t.x)
    assertEquals(0.0, t.y)
    assertEquals(1.0, t.scale)
  }

  @Test
  fun `hotbar custom origin translates native center`() {
    val t =
      HotbarLayout.transform(
        MoveHotbarSettings(customPosition = true, hud = HudSpec(x = 10, y = 20)),
        960,
        540,
      )
    assertEquals(-379.0, t.x)
    assertEquals(-498.0, t.y)
  }

  @Test
  fun `hotbar stays bounded on tiny viewport`() {
    val t =
      HotbarLayout.transform(
        MoveHotbarSettings(customPosition = true, hud = HudSpec(x = 999, y = 999, scale = 4.0)),
        20,
        10,
      )
    assertTrue(t.x.isFinite())
    assertTrue(t.y.isFinite())
  }

  @Test
  fun `pip settings never persist window identity`() {
    val json = com.google.gson.Gson().toJson(PipSettings())
    assertFalse(json.contains("Window"))
    assertFalse(json.contains("title"))
    assertFalse(json.contains("pid"))
  }

  @Test
  fun `pip validates bounded fps and size`() {
    assertFails { PipSettings(fps = 0).validate() }
    assertFails { PipSettings(width = 10000).validate() }
    PipSettings(fps = 60, width = 640).validate()
  }

  @Test
  fun `blocklist UUID normalizes dashes and case`() {
    assertEquals(
      "00112233445566778899aabbccddeeff",
      BlocklistCodec.uuid("00112233-4455-6677-8899-AABBCCDDEEFF"),
    )
    assertNull(BlocklistCodec.uuid("invalid"))
  }

  @Test
  fun `blocklist matches exact party finder line only`() {
    assertEquals("Steve", BlocklistCodec.joiner("§aParty Finder > Steve joined the dungeon group!"))
    assertNull(BlocklistCodec.joiner("[MVP+] Bob: Party Finder > Steve joined the dungeon group!"))
    assertNull(BlocklistCodec.joiner("Party Finder > Steve/pc joined the dungeon group!"))
  }

  @Test
  fun `blocklist rejects command control characters`() {
    assertFails { BlocklistCodec.entry(BlockEntry("0".repeat(32), "Steve", "x\ny")) }
    assertFalse(BlocklistCodec.name("Steve kick"))
  }

  @Test
  fun `blocklist round trips compatible archive format`() {
    val es = listOf(BlockEntry("0".repeat(32), "Steve", "Reason"))
    assertEquals(es, BlocklistCodec.decode(BlocklistCodec.encode(es)))
  }

  @Test
  fun `blocklist rejects duplicate UUIDs`() {
    val e = BlockEntry("0".repeat(32), "Steve", "R")
    assertFails { BlocklistCodec.decode(BlocklistCodec.encode(listOf(e, e))) }
  }

  @Test
  fun `blocklist atomic store roundtrip`() {
    val file = temp.resolve("list.json")
    val s = BlocklistStore(file)
    s.load()
    val rows = listOf(BlockEntry("0".repeat(32), "Steve", "reason"))
    s.save(rows)
    val next = BlocklistStore(file)
    next.load()
    assertEquals(rows, next.rows)
    assertNull(next.error)
  }

  @Test
  fun `corrupt blocklist preserved and writes blocked`() {
    val p = temp.resolve("list.json")
    Files.writeString(p, "corrupt")
    val s = BlocklistStore(p)
    s.load()
    assertNotNull(s.error)
    assertFails { s.save(emptyList()) }
    assertEquals("corrupt", Files.readString(p))
  }

  @Test
  fun `network and external actions default off`() {
    val s = BlocklistSettings()
    assertFalse(s.network)
    assertFalse(s.autoKick)
    assertFalse(s.partyWarning)
    assertFalse(SimonSettings().partyAlerts)
  }

  @Test
  fun `old config gets all new defaults`() {
    val s = FeatureCodec.decode(JsonParser.parseString("{}").asJsonObject)
    assertEquals(PipSettings(), s.pip)
    assertEquals(PetSettings(), s.pet)
    assertEquals(RouteSettings(), s.route)
    assertEquals(HaloSettings(), s.halo)
  }

  @Test
  fun `invalid new config rejected by codec`() {
    assertFails {
      FeatureCodec.decode(JsonParser.parseString("""{"pip":{"fps":999}}""").asJsonObject)
    }
    assertFails {
      FeatureCodec.decode(JsonParser.parseString("""{"halo":{"radius":"oops"}}""").asJsonObject)
    }
  }
}

class BestiaryTagCacheTest {
  @Test
  fun `names and bindings are removed when server clears or despawns a tag`() {
    val c = BestiaryTagCache()
    c.spawn(5, Point3(1.0, 2.0, 3.0))
    c.name(5, "Zombie")
    c.get(5)!!.target = BestiaryTagCache.Target(4, java.util.UUID.randomUUID())
    c.name(5, null)
    assertNull(c.get(5)!!.name)
    assertNull(c.get(5)!!.target)
    c.remove(5)
    assertNull(c.get(5))
  }

  @Test
  fun `cache is bounded and reused IDs do not retain names`() {
    val c = BestiaryTagCache(2)
    val p = Point3(0.0, 0.0, 0.0)
    c.spawn(1, p)
    c.name(1, "Zombie")
    c.spawn(2, p)
    c.spawn(3, p)
    assertNull(c.get(1))
    assertEquals(2, c.all().size)
    c.name(2, "Skeleton")
    c.spawn(2, p)
    assertNull(c.get(2)!!.name)
    c.name(2, "x".repeat(2049))
    assertNull(c.get(2)!!.name)
    c.clear()
    assertTrue(c.all().isEmpty())
  }
}

class OverlayLineTest {
  @Test
  fun `line direction remains proportional at long distances`() {
    val d = OverlayLine.direction(300.0, 400.0, 0.0)!!
    assertEquals(0.6, d.x, 1e-9)
    assertEquals(0.8, d.y, 1e-9)
    assertEquals(0.0, d.z)
  }

  @Test
  fun `zero and nonfinite line directions are ignored`() {
    assertNull(OverlayLine.direction(0.0, 0.0, 0.0))
    assertNull(OverlayLine.direction(Double.NaN, 1.0, 0.0))
  }
}
