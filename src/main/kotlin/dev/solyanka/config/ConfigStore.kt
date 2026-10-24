/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.config

import dev.solyanka.utility.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

/** Keeps unknown settings intact so future feature configuration is not discarded. */
class ConfigStore(private val path: Path, private val report: (String, Exception) -> Unit) {
    private var root = JsonObject()
    private var writable = true
    fun load() {
        if (!Files.exists(path)) return
        try {
            val loaded = Files.newBufferedReader(path).use { JsonParser.parseReader(it).asJsonObject }
            val schema = loaded.get("schemaVersion")?.asInt ?: 1
            require(schema == 1) { "Unsupported config schema $schema; original file left untouched" }
            val modules = loaded.get("modules")
            require(modules == null || modules.isJsonObject) { "modules must be an object" }
            modules?.asJsonObject?.entrySet()?.forEach { (_, value) ->
                require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "Module values must be booleans" }
            }
            loaded.get("commandUtilities")?.let { ProfileCodec.decode(it.asJsonObject) }
            loaded.get("generalUtilities")?.let { GeneralCodec.decode(it.asJsonObject) }
            root = loaded
        } catch (e: Exception) {
            writable = false
            report("Cannot load config; writes disabled to preserve original file", e)
        }
    }
    fun enabled(id: String): Boolean = root.getAsJsonObject("modules")?.get(id)?.asBoolean ?: false
    fun profile(): CommandProfile = ProfileCodec.decode(root.getAsJsonObject("commandUtilities"))
    fun general(): GeneralSettings = GeneralCodec.decode(root.getAsJsonObject("generalUtilities"))
    fun save(states: Map<String, Boolean>, profile: CommandProfile? = null, general: GeneralSettings? = null): Boolean {
        if (!writable) return false
        var temp: Path? = null
        try {
            Files.createDirectories(path.parent)
            val candidate = root.deepCopy()
            candidate.addProperty("schemaVersion", 1)
            if (profile != null) candidate.add("commandUtilities", ProfileCodec.encode(profile, candidate.getAsJsonObject("commandUtilities")))
            if (general != null) candidate.add("generalUtilities", GeneralCodec.encode(general, candidate.getAsJsonObject("generalUtilities")))
            val modules = candidate.getAsJsonObject("modules") ?: JsonObject().also { candidate.add("modules", it) }
            states.forEach { (id, enabled) -> modules.addProperty(id, enabled) }
            temp = Files.createTempFile(path.parent, "solyanka-", ".tmp")
            Files.newBufferedWriter(temp).use { GsonBuilder().setPrettyPrinting().create().toJson(candidate, it) }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            }
            root = candidate
            return true
        } catch (e: Exception) { report("Cannot save config", e); return false }
        finally {
            temp?.let {
                try { Files.deleteIfExists(it) } catch (e: Exception) { report("Cannot remove temporary config", e) }
            }
        }
    }
}
