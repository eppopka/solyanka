/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client

import net.fabricmc.loader.api.FabricLoader

object ModInfo {
  val version: String by lazy {
    FabricLoader.getInstance()
      .getModContainer("solyanka")
      .map { it.metadata.version.friendlyString }
      .orElse("dev")
  }
  val userAgent: String
    get() = "Solyanka/$version (https://github.com/eppopka/solyanka)"
}
