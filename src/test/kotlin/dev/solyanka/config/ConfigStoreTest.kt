/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.config

import com.google.gson.JsonParser
import dev.solyanka.utility.*
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
    @Test fun `old config gains safe defaults without dropping command settings`() {
        val file=dir.resolve("solyanka.json")
        Files.writeString(file,"""{"schemaVersion":1,"commandUtilities":{"allowAllInGui":true},"future":5}""")
        val c=store();c.load();assertEquals(GeneralSettings(),c.general());assertTrue(c.profile().allowAllInGui)
        assertTrue(c.save(emptyMap(),general=GeneralSettings(chat=ChatSettings(keepHistory=true))))
        val next=store();next.load();assertTrue(next.general().chat.keepHistory);assertTrue(next.profile().allowAllInGui)
        assertEquals(5,JsonParser.parseString(Files.readString(file)).asJsonObject["future"].asInt)
    }
    @Test fun `command only saves preserve general settings and unknown nested fields`() {
        val file=dir.resolve("solyanka.json")
        Files.writeString(file,"""{"generalUtilities":{"chat":{"keepHistory":true,"future":7},"tooltips":{"museum":true}}}""")
        val c=store();c.load();assertTrue(c.save(emptyMap(),CommandProfile(allowAllInGui=true)))
        val next=store();next.load();assertTrue(next.general().chat.keepHistory);assertTrue(next.general().tooltips.museum)
        assertTrue(next.save(emptyMap(),general=next.general().copy(chat=next.general().chat.copy(lines=2000))))
        val root=JsonParser.parseString(Files.readString(file)).asJsonObject
        assertEquals(7,root.getAsJsonObject("generalUtilities").getAsJsonObject("chat")["future"].asInt)
    }
    @Test fun `invalid general section disables writes and preserves original`() {
        val file=dir.resolve("solyanka.json");val original="""{"generalUtilities":{"chat":{"lines":5001}}}"""
        Files.writeString(file,original);var errors=0
        val c=ConfigStore(file) { _,_->errors++ };c.load()
        assertFalse(c.save(emptyMap()));assertEquals(1,errors);assertEquals(original,Files.readString(file))
    }
    @Test fun `invalid new general draft cannot replace last good config`() {
        val file=dir.resolve("solyanka.json");var errors=0
        val c=ConfigStore(file) { _,_->errors++ };c.load();assertTrue(c.save(mapOf("utility.chat" to true)))
        val original=Files.readString(file)
        assertFalse(c.save(mapOf("utility.chat" to false),general=GeneralSettings(chat=ChatSettings(lines=1))))
        assertTrue(c.enabled("utility.chat"));assertEquals(original,Files.readString(file));assertEquals(1,errors)
    }
    @Test fun `instant edit reaches disk before editor is closed`() {
        val file=dir.resolve("solyanka.json");val c=store();c.load()
        val editor=dev.solyanka.ui.InstantSettings(c.profile(),c.general()) { profile,general ->
            if(c.save(emptyMap(),profile,general)) null else "Save failed"
        }
        assertTrue(editor.updateGeneral { it.copy(tooltips=it.tooltips.copy(createdDate=true)) })
        val reader=store();reader.load();assertTrue(reader.general().tooltips.createdDate)
    }
    @Test fun `invalid transient instant edit leaves last successful disk value`() {
        val c=store();c.load()
        val editor=dev.solyanka.ui.InstantSettings(c.profile(),c.general()) { profile,general ->
            if(c.save(emptyMap(),profile,general)) null else "Save failed"
        }
        assertTrue(editor.lines("2200"));assertFalse(editor.lines(""))
        val reader=store();reader.load();assertEquals(2200,reader.general().chat.lines)
        assertTrue(editor.lines("2300"));val another=store();another.load();assertEquals(2300,another.general().chat.lines)
    }
    @Test fun `invalid new feature config disables writes without replacing file`() {
        val file=dir.resolve("solyanka.json");val original="""{"generalUtilities":{"features":{"player":{"health":{"scale":0}}}}}"""
        Files.writeString(file,original);var errors=0
        val c=ConfigStore(file) { _,_->errors++ };c.load();assertFalse(c.save(emptyMap()))
        assertEquals(original,Files.readString(file));assertEquals(1,errors)
    }
    @Test fun `new feature save preserves older profile chat and unknown HUD fields`() {
        val file=dir.resolve("solyanka.json")
        Files.writeString(file,"""{"commandUtilities":{"allowAllInGui":true},"generalUtilities":{"chat":{"keepHistory":true},"features":{"timers":{"pad":{"future":7}}}}}""")
        val c=store();c.load();val g=c.general()
        assertTrue(c.save(emptyMap(),general=g.copy(features=g.features.copy(timers=g.features.timers.copy(ticks=true)))))
        val next=store();next.load();assertTrue(next.profile().allowAllInGui);assertTrue(next.general().chat.keepHistory);assertTrue(next.general().features.timers.ticks)
        val root=JsonParser.parseString(Files.readString(file)).asJsonObject
        assertEquals(7,root.getAsJsonObject("generalUtilities").getAsJsonObject("features").getAsJsonObject("timers").getAsJsonObject("pad")["future"].asInt)
    }
    @Test fun `instant new HUD edits reach disk and reject invalid scale`() {
        val c=store();c.load()
        val editor=dev.solyanka.ui.InstantSettings(c.profile(),c.general()) { profile,general -> if(c.save(emptyMap(),profile,general)) null else "Save failed" }
        assertTrue(editor.updateGeneral { it.copy(features=it.features.copy(player=it.features.player.copy(health=it.features.player.health.copy(scale=2.0)))) })
        assertFalse(editor.updateGeneral { it.copy(features=it.features.copy(player=it.features.player.copy(health=it.features.player.health.copy(scale=0.0)))) })
        val next=store();next.load();assertEquals(2.0,next.general().features.player.health.scale)
    }
}
