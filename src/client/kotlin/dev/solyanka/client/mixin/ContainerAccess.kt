/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.mixin
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
@Mixin(AbstractContainerScreen::class)
interface ContainerAccess { @Accessor("hoveredSlot") fun solyankaHovered():Slot? }
