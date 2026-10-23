/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui

import com.terraformersmc.modmenu.api.ModMenuApi
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import net.minecraft.client.gui.screens.Screen

class SolyankaModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> = ConfigScreenFactory { ConfigUi.create(it) }
}
