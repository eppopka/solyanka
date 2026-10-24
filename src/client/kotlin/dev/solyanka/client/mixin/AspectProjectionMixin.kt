/* SPDX-License-Identifier: GPL-3.0-only; adapted from Stray CC0 */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Environment
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
@Mixin(net.minecraft.client.renderer.Projection::class)
abstract class AspectProjectionMixin {
 @ModifyVariable(method=["setupPerspective"],at=[At("HEAD")],argsOnly=true,ordinal=3)
 private fun solyankaWidth(width:Float)=Environment.aspect(width)
}
