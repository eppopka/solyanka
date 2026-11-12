/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

/** Session-only history of actual module state changes, newest first. */
class RecentModules(private val limit: Int = 8) {
  init {
    require(limit > 0)
  }

  private val ids = mutableListOf<String>()

  fun record(id: String) {
    ids.remove(id)
    ids.add(0, id)
    if (ids.size > limit) ids.removeAt(ids.lastIndex)
  }

  fun snapshot(): List<String> = ids.toList()
}
