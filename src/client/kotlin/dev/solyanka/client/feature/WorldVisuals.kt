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
            if(level!=null && c.gameRenderer.mainCamera().isInitialized && !c.gui.hud.isHidden && S.isEnabled(S.NAMETAGS)) {
                val s=S.currentGeneral().features.worldVisuals.nametags;val camera=c.gameRenderer.mainCamera();val origin=camera.position()
                level.players().filter {hideName(it)}.sortedByDescending {origin.distanceToSqr(it.position())}.forEach {e->
                    val pos=e.getPosition(d.getGameTimeDeltaPartialTick(true)).add(0.0,e.bbHeight.toDouble()+.35,0.0)
                    val delta=pos.subtract(origin);val forward=camera.forwardVector()
                    if(delta.x*forward.x()+delta.y*forward.y()+delta.z*forward.z()<=.1) return@forEach
                    val p=c.gameRenderer.projectPointToScreen(pos)
                    if(!p.x.isFinite() || !p.y.isFinite() || p.x !in -1.2..1.2 || p.y !in -1.2..1.2) return@forEach
                    val distance=kotlin.math.sqrt(origin.distanceToSqr(e.position()))
                    val name=e.displayName.copy();val distanceText=if(s.distance && e!==c.player) "${distance.toInt()}m" else ""
                    val nameW=c.font.width(name);val distW=if(distanceText.isEmpty()) 0 else c.font.width(distanceText)+5
                    val healthText=if(s.health) "${e.health.toInt()}/${e.maxHealth.toInt()} HP" else ""
                    val width=maxOf(nameW+distW,c.font.width(healthText))+14;val height=if(s.health) 29 else 19
                    val x=((p.x+1)*c.window.guiScaledWidth/2).toFloat();val y=((1-p.y)*c.window.guiScaledHeight/2).toFloat()
                    val scale=s.scale*(if(s.distanceScaling) dev.solyanka.utility.VisualChrome.distanceScale(distance) else 1.0)
                    fun alpha(color:Int)=dev.solyanka.utility.VisualChrome.alpha(color,s.opacity)
                    g.pose().pushMatrix()
                    try {
                        g.pose().translate(x,y);g.pose().scale(scale.toFloat(),scale.toFloat())
                        val left=-width/2;val top=-height-2
                        if(s.background) ChromeDraw.panel(g,left,top,width,height,alpha(s.backgroundColor),alpha(s.borderColor),if(s.rounded) 5 else 0)
                        g.text(c.font,name,left+7,top+5,alpha(s.color),false)
                        if(distanceText.isNotEmpty()) g.text(c.font,distanceText,left+7+nameW+5,top+5,alpha(s.distanceColor),false)
                        if(s.health) g.text(c.font,healthText,left+7,top+16,alpha(s.distanceColor),false)
                    } finally {g.pose().popMatrix()}
                }
            }
        }
    }
}
