/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.SpreadLogic
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.registries.BuiltInRegistries

object SpreadGuesser {
    var spread:Double?=null;private set
    fun reset() {spread=null}
    fun tick(c:Minecraft) {
        spread=if(S.isEnabled(S.SPREAD_GUESSER) && FeatureRuntime.context.skyblock)
            SpreadLogic.readTab(c.connection?.listedOnlinePlayers?.mapNotNull {it.tabListDisplayName?.string} ?: emptyList())
        else null
    }
    fun collect(c:Minecraft,lines:MutableList<WorldOverlay.Segment>,fills:MutableList<WorldOverlay.Fill>) {
        val value=spread ?: return
        if(value<=0 || !S.isEnabled(S.SPREAD_GUESSER) || !FeatureRuntime.context.skyblock || c.gui.screen()!=null) return
        val level=c.level ?: return
        val hit=c.hitResult as? BlockHitResult ?: return
        if(hit.type != net.minecraft.world.phys.HitResult.Type.BLOCK) return
        val pos=hit.blockPos
        if(!level.hasChunk(pos.x shr 4,pos.z shr 4)) return
        val block=level.getBlockState(pos)
        if(block.isAir) return
        val settings=S.currentGeneral().features.spreadGuesser
        val target=BuiltInRegistries.BLOCK.getKey(block.block).toString()
        // shortcut: same-type or grouped face neighbors are only guesses, upgrade after server behavior is verified.
        Direction.entries.forEach {direction ->
            val adjacent=pos.relative(direction)
            if(!level.hasChunk(adjacent.x shr 4,adjacent.z shr 4)) return@forEach
            val other=BuiltInRegistries.BLOCK.getKey(level.getBlockState(adjacent).block).toString()
            if(SpreadLogic.equivalent(target,other)) {
                val box=AABB(adjacent).inflate(.003)
                WorldOverlay.box(lines,box,settings.color)
                if(settings.filled) fills.add(WorldOverlay.Fill(box,settings.fillColor))
            }
        }
    }
}
