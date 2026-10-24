/* SPDX-License-Identifier: GPL-3.0-only
 * NoFrills NoRender-derived hooks; see NOTICE. */
package dev.solyanka.client.mixin
import dev.solyanka.client.feature.Suppression
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.*

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener::class)
abstract class SuppressParticleMixin {
 @Inject(method=["handleParticleEvent"],at=[At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;getCount()I",ordinal=0)],cancellable=true)
 private fun solyankaParticles(p:net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket,ci:CallbackInfo) { if(Suppression.particle(p)) ci.cancel() }
 @Inject(method=["handleContainerSetSlot"],at=[At("TAIL")])
 private fun solyankaSlotUpdate(p:net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket,ci:CallbackInfo) { dev.solyanka.client.feature.Experiments.update();dev.solyanka.client.feature.PriceTooltips.afterSlot(p.slot) }
 @Inject(method=["handleContainerContent"],at=[At("TAIL")])
 private fun solyankaBulkUpdate(ci:CallbackInfo) { dev.solyanka.client.feature.Experiments.update();dev.solyanka.client.feature.PriceTooltips.afterSlot(null) }

}
