/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class KeyCategoryRegistrationTest {
  @Test
  fun `settings and quick toggle share one registered category`() {
    val source =
      Path.of("src/client/kotlin/dev/solyanka/client/SolyankaClient.kt").toFile().readText()
    val registration =
      "KeyMapping.Category.register(Identifier.fromNamespaceAndPath(\"solyanka\", \"general\"))"
    assertEquals(1, Regex(Regex.escape(registration)).findAll(source).count())
    assertTrue(source.contains("val keyCategory = $registration"))
    assertTrue(
      source.contains(
        "KeyMapping(\"key.solyanka.settings\", GLFW.GLFW_KEY_RIGHT_SHIFT, keyCategory)",
      ),
    )
    assertTrue(
      source.contains(
        "KeyMapping(\"key.solyanka.quick_toggle\", GLFW.GLFW_KEY_UNKNOWN, keyCategory)",
      ),
    )
  }
}
