/* SPDX-License-Identifier: GPL-3.0-only; adapted from Stray CC0 */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Environment
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
@Mixin(net.minecraft.client.Camera::class)
abstract class AspectCameraMixin {
 @ModifyArg(method=["createProjectionMatrixForCulling"],at=[At(value="INVOKE",target="Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;")],index=1)
 private fun solyankaCull(aspect:Float)=Environment.aspect(aspect)
}
