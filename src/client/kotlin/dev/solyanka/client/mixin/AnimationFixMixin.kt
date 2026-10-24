/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills ClientPacketListenerMixin.onPreTrackerUpdate. */
package dev.solyanka.client.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.*
import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(ClientPacketListener::class)
abstract class AnimationFixMixin {
    @WrapOperation(method=["handleSetEntityData"],at=[At(value="INVOKE",target="Lnet/minecraft/network/syncher/SynchedEntityData;assignValues(Ljava/util/List;)V")])
    private fun solyankaPose(data:SynchedEntityData,items:List<SynchedEntityData.DataValue<*>>,original:Operation<Void>,packet:ClientboundSetEntityDataPacket) {
        if(FeatureRuntime.animationActive() && Minecraft.getInstance().player?.id==packet.id()) {
            val index=items.indexOfFirst { it.serializer()==EntityDataSerializers.POSE }
            if(index>=0) { original.call(data,items.toMutableList().apply { removeAt(index) });return }
        }
        original.call(data,items)
    }
}
