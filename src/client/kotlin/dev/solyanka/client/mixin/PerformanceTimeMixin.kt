/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin

import dev.solyanka.client.feature.PerformanceHud
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientPacketListener::class)
abstract class PerformanceTimeMixin {
 // Vanilla's handler has already moved the packet to the client thread at TAIL.
 @Inject(method=["handleSetTime"],at=[At("TAIL")])
 private fun solyankaTimeUpdate(packet:ClientboundSetTimePacket,ci:CallbackInfo) {
  val c=Minecraft.getInstance()
  if(c.connection===(this as Any) && c.level!=null) PerformanceHud.observe(packet.gameTime())
 }
}
