/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from the user-supplied Odin HidePlayers.kt. Odin: BSD-3-Clause;
 * see licenses/Odin-BSD-3-Clause.txt. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.core.ModuleIds
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

object HidePlayers {
    fun hide(entity:Entity):Boolean {
        if(!S.isEnabled(ModuleIds.HIDE_PLAYERS) || entity !is Player || entity.uuid.version()!=4) return false
        val player=Minecraft.getInstance().player ?: return false
        if(entity===player) return false
        val settings=S.currentGeneral().features.hidePlayers
        if(settings.onlyInDungeons && !FeatureRuntime.context.dungeon) return false
        return settings.hideAll || entity.distanceToSqr(player)<=settings.distance*settings.distance
    }
}
