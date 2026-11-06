/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin

import dev.solyanka.client.feature.HuntingFeatures
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.PacketUtils
import net.minecraft.network.protocol.game.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value=[ClientPacketListener::class], priority=1500)
abstract class BestiaryPacketMixin {
 @Inject(method=["handleAddEntity"],at=[At("HEAD")])
 private fun solyankaBestiarySpawn(p:ClientboundAddEntityPacket,ci:CallbackInfo) {
  val c=Minecraft.getInstance();PacketUtils.ensureRunningOnSameThread(p,this as ClientGamePacketListener,c.packetProcessor())
  if(c.connection===(this as Any))HuntingFeatures.bestiarySpawn(c.level,p)
 }
 @Inject(method=["handleSetEntityData"],at=[At("HEAD")])
 private fun solyankaBestiaryData(p:ClientboundSetEntityDataPacket,ci:CallbackInfo) {
  val c=Minecraft.getInstance();PacketUtils.ensureRunningOnSameThread(p,this as ClientGamePacketListener,c.packetProcessor())
  if(c.connection===(this as Any))HuntingFeatures.bestiaryData(c.level,p)
 }
 @Inject(method=["handleRemoveEntities"],at=[At("HEAD")])
 private fun solyankaBestiaryRemove(p:ClientboundRemoveEntitiesPacket,ci:CallbackInfo) {
  val c=Minecraft.getInstance();PacketUtils.ensureRunningOnSameThread(p,this as ClientGamePacketListener,c.packetProcessor())
  if(c.connection===(this as Any))HuntingFeatures.bestiaryRemove(c.level,p)
 }
}
