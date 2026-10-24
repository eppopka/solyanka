/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.WorldVisuals
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*
@Mixin(Minecraft::class)
abstract class PlayerGlowMinecraftMixin {
 @Inject(method=["shouldEntityAppearGlowing"],at=[At("RETURN")],cancellable=true)
 private fun solyankaGlow(e:Entity,ci:CallbackInfoReturnable<Boolean>) {if(WorldVisuals.glow(e) || dev.solyanka.client.feature.HuntingFeatures.glow(e)) ci.returnValue=true}
}
