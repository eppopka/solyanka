/* SPDX-License-Identifier: GPL-3.0-only
 * Stray CC0 player visuals adaptation; native Minecraft 26.2 outline backend. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.PlayerVisualPolicy as Policy
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.resources.Identifier

interface VisualEntityState {var solyankaHideName:Boolean}
object WorldVisuals {
    private val visibility=hashMapOf<Int,Boolean>()
    fun reset() {visibility.clear()}
    private fun visible(e:Entity):Boolean=visibility.getOrPut(e.id) {
        val c=Minecraft.getInstance();val level=c.level
        level!=null && level.clip(ClipContext(c.gameRenderer.mainCamera().position(),e.eyePosition,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,c.player ?: e)).type==HitResult.Type.MISS
    }
    private fun eligible(e:Entity,tag:Boolean):Boolean {
        val c=Minecraft.getInstance();if(e !is AbstractClientPlayer || c.player==null || c.level==null || c.gui.hud.isHidden) return false
        val s=S.currentGeneral().features.worldVisuals;val self=e===c.player
        val range=if(tag) s.nametags.range else s.playerGlow.range
        val distance=c.gameRenderer.mainCamera().position().distanceToSqr(e.position())
        if(tag && !s.nametags.throughWalls && e.isDiscrete && distance>32.0*32.0) return false
        val real=if(tag) s.nametags.realAccountsOnly else s.playerGlow.realAccountsOnly
        return Policy.eligible(S.isEnabled(if(tag) S.NAMETAGS else S.PLAYER_GLOW),self,tag&&s.nametags.self,c.options.cameraType.isFirstPerson,e.isAlive,e.isSpectator,e.isInvisible,Policy.account(e.uuid,self,real),distance,range) && ((if(tag) s.nametags.throughWalls else s.playerGlow.throughWalls) || visible(e))
    }
    @JvmStatic fun glow(e:Entity)=eligible(e,false)
    @JvmStatic fun glowColor()=Policy.glowColor(S.currentGeneral().features.worldVisuals.playerGlow.color)
    @JvmStatic fun throughGlow(e:Entity)=S.currentGeneral().features.worldVisuals.playerGlow.throughWalls && glow(e)
    @JvmStatic fun hideName(e:Entity)=eligible(e,true)
    fun register() {
        BlockSelection.register()
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","nametags")) {g,d->
            val c=Minecraft.getInstance();val level=c.level
            if(level!=null && !c.gui.hud.isHidden && S.isEnabled(S.NAMETAGS)) {
                val s=S.currentGeneral().features.worldVisuals.nametags;val camera=c.gameRenderer.mainCamera();val origin=camera.position()
                level.players().filter {hideName(it)}.sortedByDescending {origin.distanceToSqr(it.position())}.forEach {e->
                    val pos=e.getPosition(d.getGameTimeDeltaPartialTick(true)).add(0.0,e.bbHeight.toDouble()+.35,0.0)
                    val delta=pos.subtract(origin);val forward=camera.forwardVector()
                    if(delta.x*forward.x()+delta.y*forward.y()+delta.z*forward.z()<=.1) return@forEach
                    val p=c.gameRenderer.projectPointToScreen(pos)
                    if(!p.x.isFinite() || !p.y.isFinite() || p.x !in -1.2..1.2 || p.y !in -1.2..1.2) return@forEach
                    val text=Policy.tag(e.displayName.string,origin.distanceToSqr(e.position()),e.health.toDouble(),e.maxHealth.toDouble(),s.distance,s.health)
                    val width=c.font.width(text);val x=((p.x+1)*c.window.guiScaledWidth/2).toFloat();val y=((1-p.y)*c.window.guiScaledHeight/2).toFloat()
                    g.pose().pushMatrix()
                    try {g.pose().translate(x,y);g.pose().scale(s.scale.toFloat(),s.scale.toFloat());if(s.background) g.fill(-width/2-3,-2,width/2+3,10,s.backgroundColor);g.text(c.font,text,-width/2,0,s.color,true)} finally {g.pose().popMatrix()}
                }
            }
        }
    }
}
