/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import kotlin.test.*
import org.junit.jupiter.api.Test

class OdinFeaturesTest {
    private val settings=TimerSettings()
    @Test fun `timer begins inactive and ignores unrelated or player chat`() {
        val t=TickTimers();assertTrue(t.lines(settings).isEmpty())
        t.message("Player: [BOSS] Storm: Pathetic Maxor, just like expected.");assertEquals(-1,t.storm)
    }
    @Test fun `storm start seeds all phase timers`() {
        val t=TickTimers();t.message("§c[BOSS] Storm: Pathetic Maxor, just like expected.")
        assertEquals(20,t.pad);assertEquals(520,t.lightning);assertEquals(0,t.storm)
        assertEquals(setOf("pad","lightning","storm"),t.lines(settings).keys)
    }
    @Test fun `zero pings and outside boss never advance timers`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.")
        t.ping(0,true,settings);t.ping(1,false,settings)
        assertEquals(20,t.pad);assertEquals(0,t.storm)
    }
    @Test fun `one nonzero ping equals one Odin server tick`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.");t.ping(-1,true,settings)
        assertEquals(19,t.pad);assertEquals(519,t.lightning);assertEquals(1,t.storm)
    }
    @Test fun `pad wraps on next tick after zero when enabled`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.")
        repeat(20) { t.ping(1,true,settings) };assertEquals(0,t.pad)
        t.ping(1,true,settings);assertEquals(19,t.pad)
    }
    @Test fun `disabled pad does not repeat`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.")
        val s=settings.copy(pad=settings.pad.copy(enabled=false));repeat(21) { t.ping(1,true,s) }
        assertEquals(-1,t.pad);assertFalse("pad" in t.lines(s))
    }
    @Test fun `goldor ticks loop while core remains closed`() {
        val t=TickTimers();t.message("[BOSS] Goldor: Who dares trespass into my domain?")
        repeat(60) { t.ping(1,true,settings) };assertEquals(0,t.goldor)
        t.ping(1,true,settings);assertEquals(59,t.goldor)
        t.message("The Core entrance is opening!");assertEquals(-1,t.goldor);assertEquals(-1,t.goldorStart)
    }
    @Test fun `storm end starts 64 tick Goldor start and clears pad phase`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.")
        t.message("[BOSS] Storm: I should have known that I stood no chance.")
        assertEquals(64,t.goldorStart);assertEquals(-1,t.pad);assertEquals(-1,t.storm)
        assertEquals("Start: ",t.lines(settings.copy(startTimer=true)).getValue("goldor").prefix)
        assertFalse("goldor" in t.lines(settings))
    }
    @Test fun `PY only triggers once per reset for either phrase`() {
        val t=TickTimers();t.message("[BOSS] Storm: ENERGY HEED MY CALL!");assertEquals(55,t.py)
        t.ping(1,true,settings);t.message("[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!");assertEquals(54,t.py)
        t.reset();t.message("[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!");assertEquals(55,t.py)
    }
    @Test fun `fire freeze countdown prompt and expiry match Odin`() {
        val t=TickTimers();t.message("[BOSS] The Professor: Oh? You found my Guardians' one weakness?")
        assertEquals(206,t.freeze);assertEquals("5.30s",t.lines(settings).getValue("freeze").value)
        repeat(106) { t.ping(1,true,settings) };assertEquals("Use Fire Freeze",t.lines(settings).getValue("freeze").value)
        repeat(101) { t.ping(1,true,settings) };assertFalse("freeze" in t.lines(settings))
    }
    @Test fun `timer formatting uses fixed seconds ticks symbols and prefix options`() {
        assertEquals("1.75s",TickTimers.format(35,60,"Tick:",settings).value)
        val s=settings.copy(ticks=true,symbols=false,prefix=false)
        assertEquals(TimerText("","35",0xffffaa00.toInt()),TickTimers.format(35,60,"Tick:",s))
    }
    @Test fun `timer colors preserve high medium low thresholds`() {
        assertEquals(0xff55ff55.toInt(),TickTimers.format(40,60,"",settings).color)
        assertEquals(0xffffaa00.toInt(),TickTimers.format(20,60,"",settings).color)
        assertEquals(0xffff5555.toInt(),TickTimers.format(19,60,"",settings).color)
    }
    @Test fun `reset clears every timer and one-shot state`() {
        val t=TickTimers();t.message("[BOSS] Storm: Pathetic Maxor, just like expected.");t.message("[BOSS] Storm: ENERGY HEED MY CALL!");t.reset()
        assertTrue(t.lines(settings).isEmpty());assertEquals(-1,t.lightning)
    }
    @Test fun `context requires SkyBlock sidebar before trusting dungeon names`() {
        assertEquals(SkyblockContext(),ContextParser.parse("SURVIVAL",listOf("The Catacombs (F7)"),emptyList(),10.0,10.0))
    }
    @Test fun `context detects master floor and Odin boss coordinates`() {
        val c=ContextParser.parse("§eSKYBLOCK",listOf("§fThe Catacombs (M7)"),listOf("Dungeon: Catacombs"),0.0,0.0)
        assertTrue(c.skyblock && c.dungeon && c.master && c.boss);assertEquals(7,c.floor)
        assertFalse(ContextParser.parse("SKYBLOCK",listOf("The Catacombs (F7)"),emptyList(),-7.0,0.0).boss)
    }
    @Test fun `boss coordinates cover every Odin floor group`() {
        for((floor,pos) in listOf(1 to (-70.0 to -38.0),2 to (-38.0 to -38.0),3 to (-38.0 to -38.0),4 to (-38.0 to -38.0),5 to (-38.0 to -6.0),6 to (-38.0 to -6.0),7 to (-6.0 to -6.0))) {
            assertTrue(ContextParser.parse("SKYBLOCK",listOf("The Catacombs (F$floor)"),emptyList(),pos.first,pos.second).boss)
        }
    }
    @Test fun `unknown floor fails closed even if tab mentions dungeon`() {
        val c=ContextParser.parse("SKYBLOCK",emptyList(),listOf("Dungeon: Catacombs"),100.0,100.0)
        assertTrue(c.dungeon);assertNull(c.floor);assertFalse(c.boss)
    }
    @Test fun `ordinary SkyBlock area is not dungeon`() {
        val c=ContextParser.parse("SKYBLOCK",listOf("Village"),listOf("Area: Hub"),100.0,100.0)
        assertTrue(c.skyblock);assertFalse(c.dungeon);assertEquals("Hub",c.area)
    }
    @Test fun `player parses PUA glyphs and commas before display filtering`() {
        val p=PlayerStats();p.overlay("3,000/4,000\uE010 2,000/20,000\uE003 Mana 333\uE017 1,000\uE008 Defense 99.9/100.5\uE028")
        assertEquals(3000,p.health);assertEquals(4000,p.maxHealth);assertEquals(2000,p.mana);assertEquals(20000,p.maxMana)
        assertEquals(333,p.overflow);assertEquals(1000,p.defense);assertEquals(99,p.vitality);assertEquals(100,p.maxVitality)
    }
    @Test fun `player legacy glyphs yield seven independent HUDs`() {
        val p=PlayerStats();p.overlay("3000/4000❤ 2000/20000✎ 333ʬ 1000❈ Defense 100/100♨");p.tick(15.0,20.0,0.1)
        val lines=p.lines(PlayerDisplaySettings())
        assertEquals(7,lines.size);assertEquals("3,000/4,000❤",lines["health"]);assertEquals("100✦",lines["speed"])
        assertEquals("33,000",lines["ehp"])
    }
    @Test fun `health tracks vanilla health ratio and speed base attribute`() {
        val p=PlayerStats();p.overlay("1000/4000❤");p.tick(10.0,20.0,0.123)
        assertEquals(2000,p.health);assertEquals(123,p.speed)
    }
    @Test fun `EHP keeps source integer defense formula without int overflow`() {
        val p=PlayerStats();p.overlay("1000000/1000000❤ 199❈ Defense")
        assertEquals(2000000L,p.ehp)
        p.overlay("2000000000/2000000000❤ 2000000000❈ Defense");assertTrue(p.ehp>Int.MAX_VALUE)
    }
    @Test fun `zero mana hidden only with separate overflow`() {
        val p=PlayerStats();p.overlay("0/2000✎ 0ʬ")
        assertFalse("mana" in p.lines(PlayerDisplaySettings()))
        assertEquals("0/2,000✎",p.lines(PlayerDisplaySettings(separateOverflow=false))["mana"])
    }
    @Test fun `combined overflow respects overflow HUD and icon settings`() {
        val p=PlayerStats();p.overlay("100/200✎ 333ʬ")
        val s=PlayerDisplaySettings(separateOverflow=false)
        assertEquals("100/200✎ 333ʬ",p.lines(s)["mana"]);assertFalse("overflow" in p.lines(s))
        assertEquals("100/200 333",p.lines(s.copy(icons=false))["mana"])
        assertEquals("100/200✎",p.lines(s.copy(overflow=s.overflow.copy(enabled=false)))["mana"])
    }
    @Test fun `hide zero overflow and per HUD toggles work`() {
        val p=PlayerStats();p.overlay("1/2❤ 0ʬ")
        val s=PlayerDisplaySettings();assertFalse("overflow" in p.lines(s))
        assertEquals("0ʬ",p.lines(s.copy(hideZeroOverflow=false))["overflow"])
        assertFalse("health" in p.lines(s.copy(health=s.health.copy(enabled=false))))
    }
    @Test fun `vitality disappears when missing in the next action bar`() {
        val p=PlayerStats();p.overlay("99/100♨");assertTrue(p.vitalityShown)
        p.overlay("100/200✎");assertFalse(p.vitalityShown);assertFalse("vitality" in p.lines(PlayerDisplaySettings()))
    }
    @Test fun `action bar removes only selected recognized fields`() {
        val text="3,000/4,000❤   2,000/20,000✎ Mana   100❈ Defense   2/5 Secrets"
        assertEquals("2/5 Secrets",PlayerStats.strip(text,PlayerDisplaySettings()).trim())
        assertTrue(PlayerStats.strip(text,PlayerDisplaySettings(hideMana=false)).contains("2,000/20,000✎ Mana"))
    }
    @Test fun `unrecognized action bar remains intact and stats tolerate bad numbers`() {
        val p=PlayerStats();p.overlay("99999999999999999999/99999999999999999999❤")
        assertEquals(0,p.maxHealth);assertEquals("Custom Event 5/10",PlayerStats.strip("Custom Event 5/10",PlayerDisplaySettings()))
        p.tick(1.0,0.0,Double.NaN);assertEquals(0,p.speed)
    }
    @Test fun `stats reset removes all data`() {
        val p=PlayerStats();p.overlay("1/2❤ 1/2✎ 1ʬ 1❈ Defense 1/2♨");p.tick(1.0,2.0,0.1);p.reset()
        assertTrue(p.lines(PlayerDisplaySettings()).isEmpty());assertEquals(0,p.maxHealth)
    }
    @Test fun `feature defaults and all nested HUD settings round trip`() {
        val f=FeatureSettings(timers=TimerSettings(ticks=true),player=PlayerDisplaySettings(hideArmor=true,health=HudSpec(x=50,y=60,scale=2.0,color=0xff112233.toInt())),fixes=UtilityFixSettings(cursorClearTicks=500))
        val general=GeneralSettings(features=f)
        assertEquals(general,GeneralCodec.decode(GeneralCodec.encode(general)))
    }
    @Test fun `old general config gets safe new feature defaults`() {
        assertEquals(FeatureSettings(),GeneralCodec.decode(JsonParser.parseString("""{"chat":{"keepHistory":true}}""").asJsonObject).features)
    }
    @Test fun `feature codec rejects wrong booleans nulls fractions and invalid ranges`() {
        for(json in listOf("""{"timers":{"ticks":"true"}}""","""{"player":null}""","""{"fixes":{"cursorClearTicks":1.5}}""",
            """{"fixes":{"cursorClearTicks":1201}}""","""{"timers":{"pad":{"scale":0}}}""","""{"timers":{"pad":{"x":-1}}}""",
            """{"player":{"health":{"color":4294967295}}}""")) assertFails(json) { FeatureCodec.decode(JsonParser.parseString(json).asJsonObject) }
    }
    @Test fun `unknown future feature and nested HUD fields survive saving`() {
        val root=JsonParser.parseString("""{"features":{"future":{"x":7},"timers":{"pad":{"future":9}}}}""").asJsonObject
        val updated=GeneralCodec.encode(GeneralCodec.decode(root),root)
        assertEquals(7,updated.getAsJsonObject("features").getAsJsonObject("future")["x"].asInt)
        assertEquals(9,updated.getAsJsonObject("features").getAsJsonObject("timers").getAsJsonObject("pad")["future"].asInt)
    }
    @Test fun `all fixed HUD identifiers update immutably and validate`() {
        val t=TimerSettings();val p=PlayerDisplaySettings()
        t.huds().keys.forEach { assertEquals(20,t.withHud(it,HudSpec(x=20)).huds().getValue(it).x) }
        p.huds().keys.forEach { assertEquals(30,p.withHud(it,HudSpec(x=30)).huds().getValue(it).x) }
        assertFails { t.withHud("bad",HudSpec()) };assertFails { p.withHud("bad",HudSpec()) }
        assertEquals(12,t.goldor.x)
    }
}
