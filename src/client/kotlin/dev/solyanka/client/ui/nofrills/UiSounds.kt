/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object UiSounds {
 fun toggle(enabled:Boolean) = Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(),if(enabled) 1.35f else .85f,.3f))
 fun open() = Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(),1.6f,.18f))
}
