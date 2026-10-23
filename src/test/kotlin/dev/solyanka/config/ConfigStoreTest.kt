package dev.solyanka.config

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigStoreTest {
    @TempDir lateinit var dir: Path
    private fun store(path: Path = dir.resolve("solyanka.json")) = ConfigStore(path) { _, e -> throw e }

    @Test fun `missing file yields disabled defaults and can be saved`() {
        val file = dir.resolve("nested/solyanka.json"); val c = store(file)
        c.load(); assertFalse(c.enabled("test.module")); c.save(mapOf("test.module" to true))
        val reloaded = store(file); reloaded.load(); assertTrue(reloaded.enabled("test.module"))
    }
    @Test fun `unknown config settings and modules survive save`() {
        val file = dir.resolve("solyanka.json")
        Files.writeString(file, """{"schemaVersion":1,"future":{"x":9},"modules":{"future.feature":true}}""")
        val c = store(); c.load(); c.save(mapOf("test.module" to false))
        val root = JsonParser.parseString(Files.readString(file)).asJsonObject
        assertEquals(9,root.getAsJsonObject("future").get("x").asInt)
        assertTrue(root.getAsJsonObject("modules").get("future.feature").asBoolean)
    }
    @Test fun `corrupt config is not overwritten`() {
        val file = dir.resolve("solyanka.json"); val original = "{broken config"
        Files.writeString(file, original); val errors = mutableListOf<String>()
        val c = ConfigStore(file) { message, _ -> errors.add(message) }
        c.load(); c.save(mapOf("test.module" to true))
        assertEquals(original, Files.readString(file)); assertEquals(1, errors.size)
        assertFalse(c.enabled("test.module"))
    }
    @Test fun `future schema is left untouched`() {
        val file = dir.resolve("solyanka.json"); val original = """{"schemaVersion":999,"modules":{}}"""
        Files.writeString(file,original); var errors = 0
        val c = ConfigStore(file) { _, _ -> errors++ }; c.load(); c.save(emptyMap())
        assertEquals(1,errors); assertEquals(original,Files.readString(file))
    }
    @Test fun `non boolean module state disables writes`() {
        val file = dir.resolve("solyanka.json"); val original = """{"modules":{"test.module":"yes"}}"""
        Files.writeString(file,original); var errors = 0
        val c = ConfigStore(file) { _, _ -> errors++ }; c.load(); c.save(emptyMap())
        assertEquals(1,errors); assertEquals(original,Files.readString(file))
    }
    @Test fun `successful save leaves no temporary files`() {
        val c = store(); c.load(); c.save(mapOf("test.module" to true))
        Files.list(dir).use { assertEquals(listOf("solyanka.json"),it.map { p -> p.fileName.toString() }.toList()) }
    }
}
