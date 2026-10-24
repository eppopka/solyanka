/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Flow
import kotlin.test.*
import org.junit.jupiter.api.Test

class GeneralFeaturesTest {
    @Test fun `general defaults are off and unbound`() {
        val s=GeneralCodec.decode(null);assertEquals(GeneralSettings(),s)
        assertFalse(s.chat.copy.matches(InputDevice.KEYBOARD,-1));assertFalse(s.chat.extraLines);assertFalse(s.tooltips.museum)
    }
    @Test fun `copy bindings separate mouse from keyboard`() {
        val b=CopyBinding(InputDevice.MOUSE,2);b.validate()
        assertTrue(b.matches(InputDevice.MOUSE,2));assertFalse(b.matches(InputDevice.KEYBOARD,2))
    }
    @Test fun `copy binding validates devices and bounds`() {
        for(b in listOf(CopyBinding(key=0),CopyBinding(key=349),CopyBinding(InputDevice.MOUSE,8),CopyBinding(key=-2)))
            assertFailsWith<IllegalArgumentException> { b.validate() }
        listOf(CopyBinding(key=32),CopyBinding(key=348),CopyBinding(InputDevice.MOUSE,0),CopyBinding(InputDevice.MOUSE,7)).forEach { it.validate() }
    }
    @Test fun `limits reject out of bounds`() {
        for(s in listOf(ChatSettings(feedbackLimit=-1),ChatSettings(feedbackLimit=513),ChatSettings(lines=99),ChatSettings(lines=5001)))
            assertFailsWith<IllegalArgumentException> { s.validate() }
        ChatSettings(feedbackLimit=0,lines=100).validate();ChatSettings(feedbackLimit=512,lines=5000).validate()
    }
    @Test fun `general settings round trip all options`() {
        val s=GeneralSettings(ChatSettings(CopyBinding(InputDevice.MOUSE,2),CopyBinding(key=67),true,true,512,true,true,5000),TooltipSettings(true,true,true,true,true))
        assertEquals(s,GeneralCodec.decode(GeneralCodec.encode(s)))
    }
    @Test fun `unknown nested fields survive general serialization`() {
        val base=JsonParser.parseString("""{"future":7,"chat":{"future":8,"copy":{"future":9}},"tooltips":{"future":10}}""").asJsonObject
        val encoded=GeneralCodec.encode(GeneralSettings(),base)
        assertEquals(7,encoded["future"].asInt);assertEquals(8,encoded.getAsJsonObject("chat")["future"].asInt)
        assertEquals(9,encoded.getAsJsonObject("chat").getAsJsonObject("copy")["future"].asInt)
        assertEquals(10,encoded.getAsJsonObject("tooltips")["future"].asInt)
        assertFalse(base.getAsJsonObject("chat").has("lines"))
    }
    @Test fun `codec refuses coerced booleans null sections and fractional integers`() {
        for(json in listOf("""{"chat":{"keepHistory":"true"}}""","""{"chat":{"lines":100.5}}""",
            """{"chat":null}""","""{"tooltips":[]}""","""{"chat":{"copy":{"device":"JOYSTICK"}}}""",
            """{"chat":{"copy":{"key":"65"}}}""","""{"chat":{"lines":2147483648}}""")) {
            assertFails(json) { GeneralCodec.decode(JsonParser.parseString(json).asJsonObject) }
        }
    }
    @Test fun `hover selects bottom and next line`() {
        assertEquals(0,ChatLogic.hoveredIndex(10.0,259.0,300,320,1.0,9,0,100,20))
        assertEquals(1,ChatLogic.hoveredIndex(10.0,250.0,300,320,1.0,9,0,100,20))
    }
    @Test fun `hover obeys scale scroll and floored baseline`() {
        // floor((301-40)/0.75)=348; bottom in scaled pixels = 261.
        assertEquals(7,ChatLogic.hoveredIndex(30.0,260.0,301,320,0.75,18,7,100,10))
        assertEquals(8,ChatLogic.hoveredIndex(30.0,246.0,301,320,0.75,18,7,100,10))
    }
    @Test fun `hover excludes input box and outside visible bounds`() {
        for((x,y) in listOf(-1.0 to 259.0,400.0 to 259.0,10.0 to 260.0,10.0 to 100.0))
            assertNull(ChatLogic.hoveredIndex(x,y,300,320,1.0,9,0,100,10))
    }
    @Test fun `hover handles empty and partial final page`() {
        assertNull(ChatLogic.hoveredIndex(10.0,259.0,300,320,1.0,9,0,0,20))
        assertNull(ChatLogic.hoveredIndex(10.0,249.0,300,320,1.0,9,99,100,20))
        assertEquals(99,ChatLogic.hoveredIndex(10.0,259.0,300,320,1.0,9,99,100,20))
        assertNull(ChatLogic.hoveredIndex(10.0,259.0,300,320,0.0,9,0,100,20))
    }
    @Test fun `copy removes legacy formatting and only optionally trims`() {
        assertEquals(" hello 🙂 ",ChatLogic.copied(" §ahello §L🙂 ",false))
        assertEquals("hello 🙂",ChatLogic.copied(" §ahello §L🙂 ",true))
        assertEquals("first\nsecond",ChatLogic.copied("first\nsecond",false))
    }
    @Test fun `copy feedback zero limit does not leak text`() {
        assertEquals("Copied message to clipboard.",ChatLogic.feedback("secret",false,0))
    }
    @Test fun `copy feedback preserves unicode and bounds preview`() {
        assertEquals("Copied line to clipboard: 🙂...",ChatLogic.feedback("🙂abc",true,1))
        assertEquals("Copied message to clipboard: abc",ChatLogic.feedback("abc",false,3))
    }
    @Test fun `no tooltip options no lines`() { assertTrue(TooltipLogic.lines(ItemFacts(id="TEST",boost=50),TooltipSettings(),emptyMap()).isEmpty()) }
    @Test fun `quality reports tier and perfect boost`() {
        val s=TooltipSettings(dungeonQuality=true)
        assertTrue(TooltipLogic.lines(ItemFacts(),s,emptyMap()).isEmpty())
        assertEquals(InfoLine("Quality","50/50, Tier 7",true),TooltipLogic.lines(ItemFacts(boost=50,tier=7),s,emptyMap()).single())
        assertFalse(TooltipLogic.lines(ItemFacts(boost=49),s,emptyMap()).single().perfect)
    }
    @Test fun `dye hex pads masks and uses lowercase`() {
        val s=TooltipSettings(hexColor=true)
        assertEquals("#0000af",TooltipLogic.lines(ItemFacts(color=175),s,emptyMap()).single().value)
        assertEquals("#ffffff",TooltipLogic.lines(ItemFacts(color=-1),s,emptyMap()).single().value)
        assertTrue(TooltipLogic.lines(ItemFacts(),s,emptyMap()).isEmpty())
    }
    @Test fun `creation date uses specified timezone and ignores zero`() {
        val s=TooltipSettings(createdDate=true);val f=ItemFacts(timestamp=86400001L)
        val utc=TooltipLogic.lines(f,s,emptyMap(),Locale.US,TimeZone.getTimeZone("UTC")).single().value
        val west=TooltipLogic.lines(f,s,emptyMap(),Locale.US,TimeZone.getTimeZone("America/Los_Angeles")).single().value
        assertTrue(utc.startsWith("Friday "));assertTrue(west.startsWith("Thursday "))
        assertTrue(TooltipLogic.lines(ItemFacts(),s,emptyMap()).isEmpty())
    }
    @Test fun `museum normalizes starred and donation flags`() {
        val line=TooltipLogic.lines(ItemFacts(id="STARRED_THORNS_BOOTS",donated=true),TooltipSettings(museum=true),mapOf("THORNS_BOOTS" to "DUNGEONEERING")).single()
        assertEquals("Dungeoneering (Donated)",line.value)
        assertTrue(TooltipLogic.lines(ItemFacts(id="UNKNOWN"),TooltipSettings(museum=true),emptyMap()).isEmpty())
    }
    @Test fun `market id keeps ordinary id and perfect dungeon variant`() {
        assertEquals("ASPECT_OF_THE_END",TooltipLogic.marketId(ItemFacts(id="ASPECT_OF_THE_END")))
        assertEquals("SWORD_MAX_BOOST_TIER_6",TooltipLogic.marketId(ItemFacts(id="SWORD",boost=50,tier=6)))
    }
    @Test fun `pets resolve type rarity and tolerate corrupt json`() {
        assertEquals("SHEEP_PET_LEGENDARY",TooltipLogic.marketId(ItemFacts(id="PET",petInfo="""{"type":"SHEEP","tier":"LEGENDARY"}""")))
        for(data in listOf("","bad","{}","[]","null")) assertEquals("UNKNOWN_PET",TooltipLogic.marketId(ItemFacts(id="PET",petInfo=data)))
    }
    @Test fun `runes preserve first rune and empty fallback`() {
        assertEquals("SNOW_3_RUNE",TooltipLogic.marketId(ItemFacts(id="UNIQUE_RUNE",runes=linkedMapOf("SNOW" to 3,"OTHER" to 1))))
        assertEquals("EMPTY_RUNE",TooltipLogic.marketId(ItemFacts(id="RUNE")))
    }
    @Test fun `enchantments and potions use root locale uppercase`() {
        assertEquals("ENCHANTMENT_ULTIMATE_WISE_5",TooltipLogic.marketId(ItemFacts(id="ENCHANTED_BOOK",enchantments=mapOf("ultimate_wise" to 5))))
        assertEquals("ENCHANTMENT_UNKNOWN",TooltipLogic.marketId(ItemFacts(id="ENCHANTED_BOOK",enchantments=mapOf("a" to 1,"b" to 2))))
        assertEquals("MINING_3_POTION",TooltipLogic.marketId(ItemFacts(id="POTION",potion="mining",potionLevel=3)))
        assertEquals("UNKNOWN_POTION",TooltipLogic.marketId(ItemFacts(id="POTION")))
    }
    @Test fun `shards use source lore rather than attribute display name`() {
        assertEquals("SHARD_SEA_ARCHER",TooltipLogic.marketId(ItemFacts(id="ATTRIBUTE_SHARD",name="Attribute",lore=listOf("Source: Bogged Shard (123)"))))
        assertEquals("SHARD_CINDER_BAT",TooltipLogic.marketId(ItemFacts(name="Cinderbat Shard")))
        assertEquals("SHARD_WITHER_SPECTER",TooltipLogic.marketId(ItemFacts(name="Wither Spectre Shard")))
        assertEquals("PRISMARINE_SHARD",TooltipLogic.marketId(ItemFacts(id="PRISMARINE_SHARD",name="Prismarine Shard")))
    }
    @Test fun `all upstream shard aliases are retained`() {
        val names=listOf("Abyssal Lanternfish","Stridersurfer","Loch Emperor","End Stone Protector","Lotusfish","Inkling","Field Mouse","Flipflopper","Earthworm","Zealot Bruiser","Seashine","Beetle","Inferno Demonlord")
        val ids=listOf("ABYSSAL_LANTERN","STRIDER_SURFER","SEA_EMPEROR","ENDSTONE_PROTECTOR","LOTUS_FISH","NIGHT_SQUID","PEST","FLIP_FLOPPER","TERMITE","BRUISER","SEA_SHINE","CROPEETLE","BURNINGSOUL")
        names.zip(ids).forEach { (name,id) -> assertEquals("SHARD_$id",TooltipLogic.marketId(ItemFacts(name="$name Shard"))) }
    }
    @Test fun `empty id with source and rarity is treated as shard`() {
        assertEquals("SHARD_RABBIT",TooltipLogic.marketId(ItemFacts(name="Attribute",lore=listOf("Source: Rabbit Shard (10)","Rarity: COMMON"))))
        assertEquals("",TooltipLogic.marketId(ItemFacts(name="Rabbit")))
    }
    @Test fun `tooltip lines preserve upstream order`() {
        val s=TooltipSettings(true,true,true,true,true)
        val f=ItemFacts(id="TEST",boost=50,timestamp=1,color=0)
        assertEquals(listOf("Quality","Created","Dye Color","Museum","Item ID"),TooltipLogic.lines(f,s,mapOf("TEST" to "COMBAT")).map { it.label })
    }
    @Test fun `official museum resource decodes base and mapped items`() {
        val json="""{"success":true,"items":[{"id":"NORMAL"},{"id":"AXE","museum_data":{"category":"COMBAT","mapped_item_ids":["STARRED_AXE"]}}]}"""
        assertEquals(mapOf("AXE" to "COMBAT","STARRED_AXE" to "COMBAT"),TooltipLogic.museumData(json))
    }
    @Test fun `museum rejects error or partial payloads`() {
        for(json in listOf("{}","""{"success":false,"items":[]}""","""{"success":true,"items":[]}""",
            """{"success":true,"items":[{"id":"A","museum_data":{}}]}""",
            """{"success":true,"items":[{"id":"A","museum_data":{"category":3}}]}"""))
            assertFails(json) { TooltipLogic.museumData(json) }
    }
    private class Subscription:Flow.Subscription {
        var cancelled=false;var requested=0L
        override fun request(n:Long) { requested+=n }
        override fun cancel() { cancelled=true }
    }
    @Test fun `http body subscriber collects chunks within cap`() {
        val body=BoundedBody(4);val sub=Subscription();body.onSubscribe(sub)
        body.onNext(listOf(ByteBuffer.wrap(byteArrayOf(1,2)),ByteBuffer.wrap(byteArrayOf(3,4))));body.onComplete()
        assertContentEquals(byteArrayOf(1,2,3,4),body.body.toCompletableFuture().join());assertEquals(2,sub.requested);assertFalse(sub.cancelled)
    }
    @Test fun `http body subscriber cancels oversized response`() {
        val body=BoundedBody(4);val sub=Subscription();body.onSubscribe(sub)
        body.onNext(listOf(ByteBuffer.wrap(ByteArray(3))));body.onNext(listOf(ByteBuffer.wrap(ByteArray(2))))
        assertTrue(sub.cancelled);assertTrue(body.body.toCompletableFuture().isCompletedExceptionally)
    }
    @Test fun `http body subscriber rejects duplicate subscriptions and transport errors`() {
        val body=BoundedBody(4);val first=Subscription();val second=Subscription()
        body.onSubscribe(first);body.onSubscribe(second);assertTrue(second.cancelled)
        body.onError(IllegalStateException("network down"));assertTrue(body.body.toCompletableFuture().isCompletedExceptionally)
    }
}
