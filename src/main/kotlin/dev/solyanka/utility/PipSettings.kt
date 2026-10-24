/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
/** Native window handles/titles are deliberately NOT persisted. Selection is session-only. */
data class PipSettings(val hud:HudSpec=HudSpec(x=12,y=320),val fps:Int=30,val width:Int=240) {
 fun validate(){hud.validate();require(fps in 5..60 && width in 100..640)}
}
