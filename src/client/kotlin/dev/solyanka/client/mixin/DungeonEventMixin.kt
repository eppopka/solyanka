/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
@Mixin(ClientPacketListener::class)
abstract class DungeonEventMixin {
 @Inject(method=["handleEntityEvent"],at=[At("TAIL")])
 private fun solyankaEntityEvent(p:ClientboundEntityEventPacket,ci:CallbackInfo) {val level=Minecraft.getInstance().level ?: return;dev.solyanka.client.feature.DungeonPuzzles.event(p.getEntity(level),p.eventId.toInt())}

 @Inject(method=["handleTakeItemEntity"],at=[At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundTakeItemEntityPacket;getItemId()I",ordinal=0)])
 private fun solyankaPickup(p:net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket,ci:CallbackInfo) {
  val c=Minecraft.getInstance();val level=c.level ?: return
  val item=level.getEntity(p.itemId) as? net.minecraft.world.entity.item.ItemEntity ?: return
  dev.solyanka.client.feature.DungeonWaypoints.pickup(item)
 }
 @Inject(method=["handleMovePlayer"],at=[At("TAIL")])
 private fun solyankaTeleport(p:net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket,ci:CallbackInfo) {
  Minecraft.getInstance().player?.let {dev.solyanka.client.feature.DungeonWaypoints.teleport(it.position())}
 }

 @Inject(method=["handleSoundEvent"],at=[At("TAIL")])
 private fun solyankaBat(p:net.minecraft.network.protocol.game.ClientboundSoundPacket,ci:CallbackInfo) {
  if(p.volume==.1f && p.sound.value() in setOf(net.minecraft.sounds.SoundEvents.BAT_HURT,net.minecraft.sounds.SoundEvents.BAT_DEATH)) dev.solyanka.client.feature.DungeonWaypoints.secret(net.minecraft.core.BlockPos.containing(p.x,p.y,p.z),5)
 }
 @Inject(method=["handleRemoveEntities"],at=[At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;",ordinal=0)])
 private fun solyankaRemoved(p:net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket,ci:CallbackInfo) {
  val level=Minecraft.getInstance().level ?: return
  p.entityIds.forEach {id->(level.getEntity(id) as? net.minecraft.world.entity.item.ItemEntity)?.let {dev.solyanka.client.feature.DungeonWaypoints.pickup(it)}}
 }
}
