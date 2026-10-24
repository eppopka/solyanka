/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin

import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Gui::class)
abstract class ScreenStateMixin {
    @Inject(method=["setScreen"],at=[At("TAIL")])
    private fun solyankaScreenChanged(screen:Screen?,ci:CallbackInfo) { FeatureRuntime.screenChanged() }
}
