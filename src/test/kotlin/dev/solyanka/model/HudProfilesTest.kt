/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model
import com.google.gson.Gson
import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test
class HudProfilesTest {
 @Test fun `switch stores and restores independent layouts without changing feature state`() {
  val start=FeatureSettings(hudProfiles=HudProfilesSettings(enabled=true),scoreboardHud=ScoreboardHudSettings(hud=HudSpec(x=21,y=30),anchorRight=true))
  val inDungeon=HudProfiles.transition(start,"area:Hub","dungeon:F7").copy(scoreboardHud=start.scoreboardHud.copy(hud=start.scoreboardHud.hud.copy(x=180),anchorRight=false))
  val back=HudProfiles.transition(inDungeon,"dungeon:F7","area:Hub")
  assertEquals(21,back.scoreboardHud.hud.x);assertTrue(back.scoreboardHud.anchorRight)
  val next=HudProfiles.transition(back,"area:Hub","dungeon:F7")
  assertEquals(180,next.scoreboardHud.hud.x);assertFalse(next.scoreboardHud.anchorRight)
  assertEquals(next,FeatureCodec.decode(Gson().toJsonTree(next).asJsonObject))
 }
 @Test fun `context handles transient and dungeon data`() {
  assertEquals("default",HudProfiles.context(SkyblockContext()))
  assertNull(HudProfiles.context(SkyblockContext(skyblock=true)))
  assertEquals("area:Hub",HudProfiles.context(SkyblockContext(skyblock=true,area="Hub")))
  assertEquals("dungeon:M7",HudProfiles.context(SkyblockContext(skyblock=true,dungeon=true,floor=7,master=true)))
 }
}
