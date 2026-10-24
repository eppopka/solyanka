/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoLoadingScreen safety adaptation: suppress visuals only; never
 * cancel the screen, its tick or its LevelLoadTracker readiness barrier. */
package dev.solyanka.client.mixin

import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.LevelLoadingScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LevelLoadingScreen::class)
abstract class LoadingScreenMixin {
    @Inject(method=["extractRenderState","extractBackground"],at=[At("HEAD")],cancellable=true,require=2)
    private fun solyankaHideLoading(g:GuiGraphicsExtractor,x:Int,y:Int,delta:Float,ci:CallbackInfo) { if(FeatureRuntime.hideLoading()) ci.cancel() }
}
