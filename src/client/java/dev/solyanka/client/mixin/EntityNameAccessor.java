/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityNameAccessor {
    @Accessor("DATA_CUSTOM_NAME")
    static EntityDataAccessor<Optional<Component>> solyanka$getCustomNameAccessor() { throw new AssertionError(); }
}
