/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.renderer.extract.LevelExtractor::class)
abstract class SuppressEntityMixin {
 @Inject(method=["isEntityVisible"],at=[At("HEAD")],cancellable=true)
 private fun solyankaHide(e:net.minecraft.world.entity.Entity,f:net.minecraft.client.renderer.culling.Frustum,x:Double,y:Double,z:Double,ci:CallbackInfoReturnable<Boolean>) { if(Suppression.hide(e)) ci.returnValue=false }

}
