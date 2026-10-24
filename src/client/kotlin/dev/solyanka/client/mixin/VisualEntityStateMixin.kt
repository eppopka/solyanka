/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.VisualEntityState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
@Mixin(EntityRenderState::class)
abstract class VisualEntityStateMixin:VisualEntityState {
 @Unique override var solyankaHideName:Boolean=false
}
