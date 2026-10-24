/* SPDX-License-Identifier: GPL-3.0-only
 * Server tick signal adapted from Odin ConnectionMixin; BSD-3-Clause notice bundled. */
package dev.solyanka.client.mixin

import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
import net.minecraft.network.protocol.common.ClientboundPingPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientCommonPacketListenerImpl::class)
abstract class ClientCommonPacketListenerMixin {
    // TAIL runs only after vanilla's ensureRunningOnSameThread, never on the Netty thread.
    @Inject(method=["handlePing"],at=[At("TAIL")])
    private fun solyankaServerTick(packet:ClientboundPingPacket,ci:CallbackInfo) { FeatureRuntime.ping(packet.id) }
}
