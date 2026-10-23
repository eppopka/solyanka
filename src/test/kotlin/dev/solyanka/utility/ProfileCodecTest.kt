/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import dev.solyanka.config.ConfigStore
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ProfileCodecTest {
    @TempDir lateinit var dir: Path
    @Test fun `typed profile round trips`() {
        val p=CommandProfile(true,listOf(CommandBinding(name="Test",key=71,command="/dh",enabled=true,modifier=Modifier.CTRL,classFilters=listOf("Mage"))), listOf(Shortcut(alias="dh",message="/warp dungeon_hub",enabled=true)))
        assertEquals(p,ProfileCodec.decode(ProfileCodec.encode(p)))
    }
    @Test fun `unknown profile and row fields are retained by stable ID`() {
        val root=JsonParser.parseString("""{"future":3,"binds":[{"id":"a","futureRow":4}],"shortcuts":[]}""").asJsonObject
        val p=CommandProfile(binds=listOf(CommandBinding(id="a")))
        val updated=ProfileCodec.encode(p,root)
        assertEquals(3,updated.get("future").asInt)
        assertEquals(4,updated.getAsJsonArray("binds")[0].asJsonObject.get("futureRow").asInt)
    }
    @Test fun `wrong types and fractional keys are rejected`() {
        listOf("""{"allowAllInGui":"true"}""", """{"binds":[{"id":"a","key":71.5}]}""", """{"binds":[{"id":"a","key":999999999999}]}""", """{"shortcuts":null}""").forEach {
            assertFails { ProfileCodec.decode(JsonParser.parseString(it).asJsonObject) }
        }
    }
    @Test fun `v1 foundation config migrates additively`() {
        val path=dir.resolve("solyanka.json")
        Files.writeString(path,"""{"schemaVersion":1,"modules":{"core.diagnostics":true},"unknown":"keep"}""")
        val store=ConfigStore(path) { _,e -> throw e };store.load()
        assertEquals(CommandProfile(),store.profile())
        assertTrue(store.save(mapOf("utility.command_keybinds" to true),CommandProfile(binds=listOf(CommandBinding()))))
        val loaded=ConfigStore(path) { _,e -> throw e };loaded.load()
        assertTrue(loaded.enabled("core.diagnostics"));assertTrue(loaded.enabled("utility.command_keybinds"))
        assertEquals(1,loaded.profile().binds.size)
        assertEquals("keep",JsonParser.parseString(Files.readString(path)).asJsonObject.get("unknown").asString)
    }
    @Test fun `failed validation cannot mutate file or in memory settings`() {
        val path=dir.resolve("solyanka.json"); val errors=mutableListOf<String>()
        val store=ConfigStore(path) { m,_ -> errors.add(m) };store.load()
        assertTrue(store.save(mapOf("test.a" to false),CommandProfile()))
        val before=Files.readString(path)
        assertFalse(store.save(mapOf("test.a" to true),CommandProfile(shortcuts=listOf(Shortcut(alias="solyanka",message="/help",enabled=true)))))
        assertFalse(store.enabled("test.a"));assertEquals(before,Files.readString(path));assertEquals(CommandProfile(),store.profile());assertEquals(1,errors.size)
    }
    @Test fun `malformed profile preserves original file and denies writes`() {
        val path=dir.resolve("solyanka.json");val original="""{"modules":{},"commandUtilities":{"binds":true}}"""
        Files.writeString(path,original);val errors=mutableListOf<String>()
        val store=ConfigStore(path) { m,_ -> errors.add(m) };store.load()
        assertEquals(CommandProfile(),store.profile());assertFalse(store.save(emptyMap(),CommandProfile()))
        assertEquals(original,Files.readString(path));assertEquals(1,errors.size)
    }
}
