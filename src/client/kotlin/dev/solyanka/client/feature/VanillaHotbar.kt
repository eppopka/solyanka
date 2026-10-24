/* SPDX-License-Identifier: GPL-3.0-only; preserves native hotbar effects and previous suppression. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
object VanillaHotbar {
 fun register(){listOf(VanillaHudElements.HOTBAR,VanillaHudElements.HELD_ITEM_TOOLTIP,VanillaHudElements.INFO_BAR,VanillaHudElements.EXPERIENCE_LEVEL).forEach {id->
  HudElementRegistry.replaceElement(id){original->HudElement {g,d->
   val c=Minecraft.getInstance();val s=S.currentGeneral().features.moveHotbar
   val follower=when(id){VanillaHudElements.HOTBAR->true;VanillaHudElements.HELD_ITEM_TOOLTIP->s.moveItemName;VanillaHudElements.INFO_BAR->s.moveInfoBar;else->s.moveXpLevel}
   if(!S.isEnabled(S.MOVE_HOTBAR) || c.level==null || c.player==null || !follower)original.extractRenderState(g,d)
   else if(s.hud.enabled && c.gui.screen() !is dev.solyanka.client.ui.HudEditorScreen){
    val t=HotbarLayout.transform(s,c.window.guiScaledWidth,c.window.guiScaledHeight)
    g.pose().pushMatrix();try{g.pose().translate(t.x.toFloat(),t.y.toFloat());g.pose().scale(t.scale.toFloat(),t.scale.toFloat());original.extractRenderState(g,d)}finally{g.pose().popMatrix()}
   }
  }}
 }}
}
