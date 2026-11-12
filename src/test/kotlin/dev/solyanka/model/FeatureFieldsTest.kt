/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import dev.solyanka.utility.FeatureSettings
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.*
import org.junit.jupiter.api.Test

class FeatureFieldsTest {
  private val sections =
    listOf(
      "environment",
      "map",
      "puzzles",
      "prices",
      "experiments",
      "suppression",
      "worldVisuals.playerGlow",
      "worldVisuals.nametags",
      "worldVisuals.blockOutline",
      "worldVisuals.titanium",
      "inventoryHud",
      "itemNameHud",
      "skillHud",
    )
  private val gson = Gson()

  private fun node(root: JsonObject, path: String): JsonObject =
    path.split('.').fold(root) { current, key -> current.getAsJsonObject(key) }

  @Test
  fun `descriptors mirror each legacy section and update only their target`() {
    val initial = FeatureSettings()
    val original = gson.toJsonTree(initial).asJsonObject
    sections.forEach { section ->
      val target = node(original, section)
      val expectedNames =
        target.entrySet().filterNot { it.key == "hud" }.map { it.key }.toMutableSet()
      if (target.has("hud")) expectedNames.addAll(listOf("hud.enabled", "hud.color"))
      val descriptors = FeatureFields.fields(initial, section)
      assertEquals(expectedNames, descriptors.map { it.name }.toSet(), section)
      assertEquals(descriptors.size, descriptors.map { it.name }.distinct().size, section)
      descriptors.forEach { field ->
        val expected = original.deepCopy()
        val dest = node(expected, section)
        val targetField = if (field.name.startsWith("hud.")) dest.getAsJsonObject("hud") else dest
        val name = field.name.substringAfterLast('.')
        val changed =
          when (field) {
            is FeatureField.Flag -> {
              assertEquals(targetField[name].asBoolean, field.value)
              targetField.add(name, JsonPrimitive(!field.value))
              field.set(initial, !field.value)
            }
            is FeatureField.Integer -> {
              assertEquals(targetField[name].asInt, field.value)
              val v = field.value xor 1
              targetField.add(name, JsonPrimitive(v))
              field.set(initial, v)
            }
            is FeatureField.Decimal -> {
              assertEquals(targetField[name].asDouble, field.value)
              val v = field.value + 0.125
              targetField.add(name, JsonPrimitive(v))
              field.set(initial, v)
            }
          }
        assertEquals(expected, gson.toJsonTree(changed).asJsonObject, "$section.${field.name}")
      }
    }
  }

  @Test
  fun `catalog field names are all covered by descriptors`() {
    val f = FeatureSettings()
    SettingsCatalog.panels.values.forEach { spec ->
      val fields = FeatureFields.fields(f, spec.section).map { it.name }.toSet()
      assertTrue(fields.containsAll(spec.fields), "${spec.title}: ${spec.fields-fields}")
    }
  }

  @Test
  fun `generic editors do not serialize feature state on every edit`() {
    val root = Path.of("src/client/kotlin/dev/solyanka/client/ui")
    listOf("AdvancedUi.kt", "ExtraHudUi.kt", "WorldVisualUi.kt", "HudStyleUi.kt").forEach { name ->
      val text = root.resolve(name).readText()
      assertFalse("toJsonTree" in text, name)
      assertFalse("FeatureCodec.decode" in text, name)
    }
  }
}
