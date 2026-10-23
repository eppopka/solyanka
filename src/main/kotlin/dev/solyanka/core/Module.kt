package dev.solyanka.core

enum class Category { CORE, DUNGEON, SOLVER, UTILITY, RENDER, HUD, COSMETIC, HUNTING }
enum class ResetReason { WORLD_CHANGED, DISCONNECTED }

data class ModuleInfo(val id: String, val name: String, val category: Category) {
    init { require(id.matches(Regex("[a-z][a-z0-9_.-]*"))) { "Invalid module id: $id" } }
}

/** Hooks run on the client thread. Release owned resources in onDisable(). */
abstract class Module(val info: ModuleInfo) {
    var enabled: Boolean = false
        private set

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        if (value) {
            try {
                onEnable()
                enabled = true
            } catch (failure: Exception) {
                try { onDisable() } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
                throw failure
            }
        } else {
            // Disabled first so event dispatch cannot keep invoking a failed module.
            enabled = false
            onDisable()
        }
    }

    protected open fun onEnable() {}
    protected open fun onDisable() {}
    open fun onTick() {}
    open fun onReset(reason: ResetReason) {}
}
