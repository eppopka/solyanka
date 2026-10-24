/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*
import dev.solyanka.ui.*
import org.junit.jupiter.api.Test
import kotlin.test.*
import java.nio.file.Path
import kotlin.io.path.readText

class ExtraHudFeaturesTest {
    @Test fun `XP bar hiding requires its own option module and SkyBlock`() {
        for(enabled in listOf(false,true)) for(skyblock in listOf(false,true)) for(hide in listOf(false,true)) {
            val f=PlayerDisplaySettings(hideXp=true,hideXpBar=hide)
            assertEquals(enabled && skyblock && hide,PlayerHudVisibility.xpBar(enabled,skyblock,f))
        }
        assertFalse(PlayerHudVisibility.xpBar(true,true,PlayerDisplaySettings(hideXp=true)))
        assertTrue(PlayerHudVisibility.xpBar(true,true,PlayerDisplaySettings(hideXp=false,hideXpBar=true)))
    }
    @Test fun `old XP level setting remains independent on migration`() {
        val f=FeatureCodec.decode(JsonParser.parseString("""{"player":{"hideXp":true}}""").asJsonObject)
        assertTrue(f.player.hideXp);assertFalse(f.player.hideXpBar)
        assertEquals(InventoryHudSettings(),f.inventoryHud)
    }
    @Test fun `new HUD and XP settings round trip with strict types`() {
        val f=FeatureSettings(player=PlayerDisplaySettings(hideXpBar=true),inventoryHud=InventoryHudSettings(armor=false,minimal=true),itemNameHud=ItemNameHudSettings(hideVanilla=false),skillHud=SkillHudSettings(timeoutSeconds=20,icon=false))
        assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
        for(raw in listOf("""{"player":{"hideXpBar":1}}""","""{"inventoryHud":{"armor":"true"}}""","""{"skillHud":{"timeoutSeconds":121}}""","""{"itemNameHud":{"hud":{"scale":0.1}}}""")) assertFails {FeatureCodec.decode(JsonParser.parseString(raw).asJsonObject)}
    }
    @Test fun `settings preserve unknown fields across an upgrade`() {
        val base=JsonParser.parseString("""{"features":{"inventoryHud":{"future":123},"player":{"extra":true}}}""").asJsonObject
        val current=GeneralCodec.decode(base)
        val next=GeneralCodec.encode(current.copy(features=current.features.copy(player=current.features.player.copy(hideXpBar=true))),base)
        assertEquals(123,next.getAsJsonObject("features").getAsJsonObject("inventoryHud")["future"].asInt)
        assertTrue(next.getAsJsonObject("features").getAsJsonObject("player")["extra"].asBoolean)
    }
    @Test fun `inventory slot lists are complete unique and exclude equipped slots`() {
        assertEquals((9..35).toList(),InventoryHudLayout.mainSlots())
        assertEquals((0..8).toList(),InventoryHudLayout.hotbarSlots())
        assertEquals(36,(InventoryHudLayout.mainSlots()+InventoryHudLayout.hotbarSlots()).distinct().size)
    }
    @Test fun `inventory dimensions follow every armor hotbar and minimal combination`() {
        for(armor in listOf(false,true)) for(hotbar in listOf(false,true)) for(minimal in listOf(false,true)) {
            val s=InventoryHudSettings(armor=armor,hotbar=hotbar,minimal=minimal);val m=InventoryHudLayout.metrics(s)
            assertEquals((if(minimal) 4 else 14)+178,m.width)
            assertEquals((if(minimal) 4 else 14)+58+(if(armor) 23 else 0)+(if(hotbar) 23 else 0)+(if(!minimal && !armor) 11 else 0),m.height)
            assertTrue(m.mainY>=m.pad);if(hotbar) assertTrue(m.hotbarY>m.mainY)
        }
    }
    @Test fun `new HUDs participate in editor and preserve unrelated fields`() {
        val f=FeatureSettings();val entries=HudLayout.entries(f)
        for(key in listOf("extra.inventory","extra.item_name","extra.skill")) {
            assertTrue(entries.any {it.key==key})
            val next=HudLayout.update(f,key) {it.copy(x=350,y=200,scale=1.5,enabled=false)}
            assertEquals(350,HudLayout.entries(next).first {it.key==key}.hud.x)
            assertEquals(f.player,next.player);assertEquals(f.puzzles,next.puzzles)
        }
        val e=entries.first {it.key=="extra.inventory"};val b=HudLayout.bounds(e,1,1000,1000)
        assertEquals(InventoryHudLayout.metrics(f.inventoryHud).width,b.width)
        assertEquals(InventoryHudLayout.metrics(f.inventoryHud).height,b.height)
    }
    @Test fun `instant edit writes new HUD config without enabling its master`() {
        var saved=GeneralSettings();var writes=0
        val model=InstantSettings(CommandProfile(),saved) {_,g->saved=g;writes++;null}
        assertTrue(model.updateGeneral {it.copy(features=HudLayout.update(it.features,"extra.inventory") {h->h.copy(scale=2.0)})})
        assertEquals(2.0,saved.features.inventoryHud.hud.scale);assertEquals(1,writes)
    }
    @Test fun `runtime source gates XP-only class and excludes Aether module`() {
        val root=Path.of("src/client/kotlin/dev/solyanka/client")
        val mixin=root.resolve("mixin/ExperienceBarMixin.kt").readText()
        assertTrue(mixin.contains("@Mixin(ExperienceBar::class)"));assertTrue(mixin.contains("\"extractBackground\",\"extractRenderState\""))
        assertFalse(mixin.contains("VanillaHudElements.INFO_BAR"))
        val client=root.resolve("SolyankaClient.kt").readText()
        assertFalse(client.contains("AetherOS"));assertTrue(client.contains("ExtraHuds.register()"))
        val runtime=root.resolve("feature/FeatureRuntime.kt").readText()
        assertTrue(runtime.indexOf("ExtraHuds.skills.overlay(message.string)")<runtime.indexOf("stats.overlay(message.string)"))
    }
}
