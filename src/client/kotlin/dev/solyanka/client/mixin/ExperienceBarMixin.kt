/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin

import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.feature.FeatureRuntime
import dev.solyanka.utility.PlayerHudVisibility
import net.minecraft.client.gui.contextualbar.ExperienceBar
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Do not cancel INFO_BAR: that would also suppress mount/locator bars in 26.2. */
@Mixin(ExperienceBar::class)
abstract class ExperienceBarMixin {
    @Inject(method=["extractBackground","extractRenderState"],at=[At("HEAD")],cancellable=true,require=2)
    private fun solyankaHideXpBar(ci:CallbackInfo) {
        if(PlayerHudVisibility.xpBar(SolyankaClient.isEnabled(SolyankaClient.PLAYER_DISPLAY),FeatureRuntime.context.skyblock,SolyankaClient.currentGeneral().features.player)) ci.cancel()
    }
}
