/* SPDX-License-Identifier: GPL-3.0-only; user-provided AmbientParticles/RotatingItem adaptations. */
package dev.solyanka.client.feature

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.HumanoidArm

object ArchiveVisuals {
    private var rotationEpoch=System.nanoTime()
    fun resetRotation() {rotationEpoch=System.nanoTime()}
    fun tick(client:Minecraft) {
        if(!S.isEnabled(S.AMBIENT_PARTICLES) || client.isPaused) return
        val level=client.level ?: return;val player=client.player ?: return
        val s=S.currentGeneral().features.ambientParticles
        val type=when(s.particle) {
            "end_rod"->ParticleTypes.END_ROD;"firefly"->ParticleTypes.FIREFLY;"cherry"->ParticleTypes.CHERRY_LEAVES
            "snowflake"->ParticleTypes.SNOWFLAKE;"soul"->ParticleTypes.SOUL;"portal"->ParticleTypes.PORTAL
            "witch"->ParticleTypes.WITCH;else->ParticleTypes.HAPPY_VILLAGER
        }
        repeat(s.amount) {
            val p=AmbientParticleLogic.sample(s)
            level.addParticle(type,player.x+p.x,player.y+p.y,player.z+p.z,p.vx,p.vy,p.vz)
        }
    }
    fun transform(pose:PoseStack,arm:HumanoidArm) {
        if(!S.isEnabled(S.ROTATING_ITEM)) return
        val c=Minecraft.getInstance();if(c.player==null || c.level==null) return
        val m=RotatingItemLogic.motion(System.nanoTime()-rotationEpoch,S.currentGeneral().features.rotatingItem,arm==HumanoidArm.LEFT)
        pose.translate(m.x,m.y,m.z)
        if(m.rx!=0.0) pose.mulPose(Axis.XP.rotationDegrees(m.rx.toFloat()))
        if(m.ry!=0.0) pose.mulPose(Axis.YP.rotationDegrees(m.ry.toFloat()))
        if(m.rz!=0.0) pose.mulPose(Axis.ZP.rotationDegrees(m.rz.toFloat()))
    }
}
