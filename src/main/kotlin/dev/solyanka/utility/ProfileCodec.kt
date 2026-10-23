/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*

object ProfileCodec {
    private fun JsonObject.string(name: String, default: String = ""): String = get(name)?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "$name must be a string" }; it.asString
    } ?: default
    private fun JsonObject.bool(name: String, default: Boolean = false): Boolean = get(name)?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "$name must be boolean" }; it.asBoolean
    } ?: default
    private fun JsonObject.strings(name: String): List<String> = get(name)?.let { value ->
        require(value.isJsonArray) { "$name must be an array" }
        value.asJsonArray.map { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString }
    } ?: emptyList()
    private fun JsonObject.rows(name: String): List<JsonObject> = get(name)?.let { value ->
        require(value.isJsonArray && value.asJsonArray.size() <= 128) { "Invalid $name list" }
        value.asJsonArray.map { it.asJsonObject }
    } ?: emptyList()
    fun decode(obj: JsonObject?): CommandProfile {
        if (obj == null) return CommandProfile()
        val profile = CommandProfile(obj.bool("allowAllInGui"), obj.rows("binds").map { b ->
            val key = b.get("key")?.let {
                require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber && it.asBigDecimal.stripTrailingZeros().scale() <= 0) { "key must be integer" }; it.asBigDecimal.intValueExact()
            } ?: -1
            CommandBinding(id = b.string("id"), name = b.string("name", "Keybind"),
                device = InputDevice.valueOf(b.string("device", "KEYBOARD")), key = key,
                command = b.string("command"), enabled = b.bool("enabled"), allowInGui = b.bool("allowInGui"),
                modifier = Modifier.valueOf(b.string("modifier", "ANY")), classFilters = b.strings("classFilters"), islandFilters = b.strings("islandFilters"))
        }, obj.rows("shortcuts").map { s -> Shortcut(s.string("id"), s.string("alias"), s.string("message"), s.bool("enabled")) })
        CommandRules.validate(profile)
        return profile
    }
    /** Retain unknown top-level profile fields and per-entry fields using stable IDs. */
    fun encode(profile: CommandProfile, base: JsonObject? = null): JsonObject {
        CommandRules.validate(profile)
        val root = base?.deepCopy() ?: JsonObject()
        val gson = Gson()
        fun mergeRows(name: String, fresh: List<Any>) {
            val previous = root.getAsJsonArray(name)?.associateBy { it.asJsonObject.get("id")?.asString }
            val array = JsonArray()
            fresh.forEach { row ->
                val encoded = gson.toJsonTree(row).asJsonObject
                val merged = previous?.get(encoded.get("id").asString)?.asJsonObject?.deepCopy() ?: JsonObject()
                encoded.entrySet().forEach { (k,v) -> merged.add(k,v) }; array.add(merged)
            }
            root.add(name,array)
        }
        root.addProperty("allowAllInGui",profile.allowAllInGui)
        mergeRows("binds",profile.binds); mergeRows("shortcuts",profile.shortcuts)
        return root
    }
}
