package dev.solyanka.core

/** Construct/register/tick only on the client thread. One bad feature is quarantined. */
class ModuleRegistry(private val report: (String, Exception) -> Unit) {
    private val modules = linkedMapOf<String, Module>()
    fun register(module: Module) {
        require(module.info.id !in modules) { "Duplicate module: ${module.info.id}" }
        modules[module.info.id] = module
    }
    fun all(): List<Module> = modules.values.toList()
    fun get(id: String): Module? = modules[id]
    fun tick() = dispatch("tick") { it.onTick() }
    fun reset(reason: ResetReason) = dispatch("reset") { it.onReset(reason) }
    fun shutdown() {
        all().asReversed().filter { it.enabled }.forEach {
            try { it.setEnabled(false) } catch (e: Exception) { report(it.info.id, e) }
        }
    }
    private fun dispatch(event: String, action: (Module) -> Unit) {
        all().filter { it.enabled }.forEach { module ->
            try { action(module) } catch (e: Exception) {
                try { module.setEnabled(false) } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
                report("${module.info.id}:$event", e)
            }
        }
    }
}
