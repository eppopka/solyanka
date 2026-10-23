package dev.solyanka.config

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
            root = loaded
        } catch (e: Exception) {
            writable = false
            report("Cannot load config; writes disabled to preserve original file", e)
        }
    }
    fun enabled(id: String): Boolean = root.getAsJsonObject("modules")?.get(id)?.asBoolean ?: false
    fun save(states: Map<String, Boolean>) {
        if (!writable) return
        var temp: Path? = null
        try {
            Files.createDirectories(path.parent)
            root.addProperty("schemaVersion", 1)
            val modules = root.getAsJsonObject("modules") ?: JsonObject().also { root.add("modules", it) }
            states.forEach { (id, enabled) -> modules.addProperty(id, enabled) }
            temp = Files.createTempFile(path.parent, "solyanka-", ".tmp")
            Files.newBufferedWriter(temp).use { GsonBuilder().setPrettyPrinting().create().toJson(root, it) }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) { report("Cannot save config", e) }
        finally {
            temp?.let {
                try { Files.deleteIfExists(it) } catch (e: Exception) { report("Cannot remove temporary config", e) }
            }
        }
    }
}
