/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import kotlin.test.*
import org.junit.jupiter.api.Test

class RngMessageFormatsTest {
    private val tracker="&6[SkyHanni] &aTracker Drop: &9Sharpness X &7(&6999M&7)"
    private val profit="&6[SkyHanni] &eProfit for &fVanguard Corpse: &6564M"
    private val found="&d&lRNG DROP! &73ppopka&e just found a &6Necron's Handle&e!"
    private fun parse(raw:String,name:String="3ppopka"):Pair<RngStyledMessage,RngDrop> {
        val styled=RngStyledMessage.preview(raw)
        return styled to assertNotNull(RngAnnounceLogic.parse(styled.text,name))
    }
    @Test fun `user examples parse with or without a test star and pass their color checks`() {
        for(prefix in listOf("","* ")) {
            val (a,ad)=parse(prefix+tracker)
            assertEquals("Sharpness X",ad.item);assertEquals(999000000.0,ad.value)
            assertEquals(RngDropKind.TRACKER,ad.kind)
            assertNull(RngAnnounceLogic.colorFailure(a,ad,true))
            val (b,bd)=parse(prefix+profit)
            assertEquals("Vanguard Corpse",bd.item);assertEquals(564000000.0,bd.value)
            assertEquals(RngDropKind.CORPSE_PROFIT,bd.kind)
            assertNull(RngAnnounceLogic.colorFailure(b,bd,true))
            val (c,cd)=parse(prefix+found)
            assertEquals("Necron's Handle",cd.item);assertNull(cd.value)
            assertNull(RngAnnounceLogic.colorFailure(c,cd,true))
            assertTrue(RngAnnounceLogic.candidate(a.text));assertTrue(RngAnnounceLogic.candidate(c.text))
            assertFalse(ad.message.startsWith("*"))
        }
    }
    @Test fun `colors are checked per token rather than anywhere in the message`() {
        val (badPrefix,a)=parse(tracker.replace("&6[SkyHanni]","&f[SkyHanni]"))
        assertNotNull(RngAnnounceLogic.colorFailure(badPrefix,a,true))
        val (badAmount,b)=parse(tracker.replace("&6999M","&f999M"))
        assertNotNull(RngAnnounceLogic.colorFailure(badAmount,b,true))
        val (badHeader,c)=parse(found.replace("&d&l","&f&l"))
        assertNotNull(RngAnnounceLogic.colorFailure(badHeader,c,true))
        val (plain,d)=parse("[SkyHanni] Tracker Drop: Sharpness X (999M)")
        assertNotNull(RngAnnounceLogic.colorFailure(plain,d,true))
        assertNull(RngAnnounceLogic.colorFailure(plain,d,false))
    }
    @Test fun `RNG header requires the exact local account name and accepts a or an`() {
        val styled=RngStyledMessage.preview(found)
        assertNull(RngAnnounceLogic.parse(styled.text,"SomeoneElse"))
        assertNull(RngAnnounceLogic.parse(styled.text,"eppopka"))
        assertNotNull(RngAnnounceLogic.parse("RNG DROP! [MVP+] 3ppopka just found an Alloy!","3ppopka"))
        assertNotNull(RngAnnounceLogic.parse("[RNG] 3ppopka found a Rare Item!","3ppopka"))
    }
    @Test fun `player chat lookalikes and echoed announcements do not match`() {
        for(prefix in listOf("Party > [MVP+] 3ppopka: ","Guild > Someone: ","[MVP+] Someone: ","From Someone: ","* [MVP+] Someone: ")) {
            for(raw in listOf(tracker,profit,found)) {
                val text=RngStyledMessage.preview(prefix+raw).text
                assertNull(RngAnnounceLogic.parse(text,"3ppopka"),text)
            }
        }
    }
    @Test fun `unreadable SkyHanni amounts never bypass the value filter`() {
        for(amount in listOf("NaN","unknown","-1M","Infinity","999M trailing garbage")) {
            assertNull(RngAnnounceLogic.parse("[SkyHanni] Tracker Drop: Item ($amount)","3ppopka"))
            assertNull(RngAnnounceLogic.parse("[SkyHanni] Profit for Vanguard Corpse: $amount","3ppopka"))
        }
        val (_,below)=parse(profit.replace("564M","29.9M"))
        val (_,equal)=parse(profit.replace("564M","30M"))
        assertFalse(RngAnnounceLogic.allowed(below,RngAnnounceSettings()))
        assertTrue(RngAnnounceLogic.allowed(equal,RngAnnounceSettings()))
        val (_,standard)=parse(found)
        assertFalse(RngAnnounceLogic.allowed(standard,RngAnnounceSettings(announceUnknownValue=false)))
    }
    @Test fun `legacy configuration receives enabled color checks without losing chat targets`() {
        val cfg=FeatureCodec.decode(JsonParser.parseString("""{"rngAnnounce":{"guildChat":true,"partyChat":false}}""").asJsonObject)
        assertTrue(cfg.rngAnnounce.requireColors)
        assertEquals(listOf("gc"),cfg.rngAnnounce.channels())
    }
    @Test fun `preview never silently interprets arbitrary names as formatting`() {
        assertEquals("3ppopka &z",RngStyledMessage.preview("3ppopka &z").text)
        val text=RngStyledMessage.preview("&6[SkyHanni] &7(&6999M&7)")
        assertEquals(text.text.length,text.colors.size)
        assertEquals("#ffaa00",text.colors.first())
    }
}
