/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WaypointEditorSettingsTest {
 @Test fun `old settings have an unbound editor key`() {
  val settings=FeatureCodec.decode(JsonParser.parseString("{\"waypoints\":{\"allowEdits\":true}} ").asJsonObject)
  assertEquals(CopyBinding(),settings.waypoints.editorKey)
 }
 @Test fun `editor key persists and is validated`() {
  val value=FeatureSettings(waypoints=WaypointSettings(editorKey=CopyBinding(InputDevice.KEYBOARD,71)))
  assertEquals(value,FeatureCodec.decode(Gson().toJsonTree(value).asJsonObject))
  assertFails {WaypointSettings(editorKey=CopyBinding(InputDevice.MOUSE,99)).validate()}
 }
}
