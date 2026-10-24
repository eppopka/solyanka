/* SPDX-License-Identifier: GPL-3.0-only; user-provided impl.zip MoveHearts adaptation. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.utility.HeartLayout
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Never reimplements health state or sprites; wraps the previously installed element once. */
object VanillaHearts {
    fun register() {
        listOf(VanillaHudElements.HEALTH_BAR,VanillaHudElements.ARMOR_BAR).forEach {id->
            HudElementRegistry.replaceElement(id) {original->HudElement {g,d->
                val c=Minecraft.getInstance();val s=S.currentGeneral().features.moveHearts
                val active=S.isEnabled(S.MOVE_HEARTS) && c.level!=null && c.player!=null && (id==VanillaHudElements.HEALTH_BAR || s.moveArmor)
                if(!active) original.extractRenderState(g,d)
                else if(s.hud.enabled && c.gui.screen() !is HudEditorScreen) {
                    val t=HeartLayout.transform(s,c.window.guiScaledWidth,c.window.guiScaledHeight)
                    g.pose().pushMatrix()
                    try {
                        g.pose().translate(t.translateX.toFloat(),t.translateY.toFloat());g.pose().scale(t.scale.toFloat(),t.scale.toFloat())
                        original.extractRenderState(g,d)
                    } finally {g.pose().popMatrix()}
                }
            }}
        }
    }
    fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {
        if(key!="archive.hearts") return false
        val s=S.currentGeneral().features.moveHearts;g.pose().pushMatrix()
        try {
            g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(s.hud.scale.toFloat(),s.hud.scale.toFloat())
            g.fill(0,0,HeartLayout.WIDTH,HeartLayout.HEIGHT,0x50c82828)
            g.text(Minecraft.getInstance().font,"Vanilla Hearts",1,0,if(s.hud.enabled) 0xffff5a5a.toInt() else 0xff888888.toInt(),false)
        } finally {g.pose().popMatrix()}
        return true
    }
}
