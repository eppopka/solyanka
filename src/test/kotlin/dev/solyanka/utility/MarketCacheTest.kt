/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import kotlin.test.*
import org.junit.jupiter.api.Test
class MarketCacheTest {
 @Test fun `local cache round trips full integer precision`() {val paid=mapOf("uuid" to 9007199254740993L);val recipes=mapOf("B" to listOf(CraftRecipe(2,mapOf("A" to 3))));assertEquals(MarketCache(paid,recipes),MarketCacheCodec.decode(MarketCacheCodec.encode(paid,recipes)))}
 @Test fun `fractional negative or overflowing stored costs are rejected`() {listOf("1.5","0","-1","9223372036854775808").forEach {assertFails {MarketCacheCodec.decode("""{"paid":{"uuid":$it}}""")}}}
 @Test fun `recipes require positive integer quantities and complete ingredients`() {listOf("""{"quantity":0,"items":{"A":1}}""","""{"quantity":1.5,"items":{"A":1}}""","""{"quantity":1,"items":{"A":0}}""","""{"quantity":1,"items":{"A":1.5}}""","""{"quantity":1,"items":{}}""").forEach {assertFails {MarketCacheCodec.decode("""{"recipes":{"B":[$it]}}""")}}}
 @Test fun `empty or unknown-only cache can be read safely`() {assertEquals(MarketCache(emptyMap(),emptyMap()),MarketCacheCodec.decode("""{"future":true}"""))}

 @Test fun `Hypixel bids and asks map to correct player-facing labels`() {val prices=PriceLogic.parseHypixelBazaar("""{"success":true,"products":{"A":{"quick_status":{"buyPrice":90,"sellPrice":100}}}}""");assertEquals(100.0,prices.getValue("A").buy);assertEquals(90.0,prices.getValue("A").sell)}
 @Test fun `failed or invalid Hypixel response never becomes prices`() {assertFails {PriceLogic.parseHypixelBazaar("""{"success":false}""")};assertFails {PriceLogic.parseHypixelBazaar("""{"success":true,"products":{}}""")};assertFails {PriceLogic.parseHypixelBazaar("""{"success":true,"products":{"A":{"quick_status":{"buyPrice":-1,"sellPrice":100}}}}""")}}
}
