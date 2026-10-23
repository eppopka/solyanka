package dev.solyanka.client

import dev.solyanka.config.ConfigStore
import dev.solyanka.core.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import com.mojang.brigadier.arguments.StringArgumentType
import org.slf4j.LoggerFactory

object SolyankaClient : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("solyanka")
    private val modules = ModuleRegistry { id, error -> logger.error("Module failure: $id", error) }
    private val config = ConfigStore(FabricLoader.getInstance().configDir.resolve("solyanka.json")) {
        message, error -> logger.error(message, error)
    }
    private var previousWorld: Any? = null

    override fun onInitializeClient() {
        // The only implemented module is a no-rendering lifecycle diagnostic.
        modules.register(object : Module(ModuleInfo("core.diagnostics", "Lifecycle diagnostics", Category.CORE)) {
            private var ticks = 0L
            override fun onTick() { ticks++ }
            override fun onReset(reason: ResetReason) { ticks = 0 }
            override fun onEnable() { logger.info("Diagnostics enabled") }
            override fun onDisable() { logger.info("Diagnostics disabled") }
        })
        config.load()
        modules.all().forEach {
            try { it.setEnabled(config.enabled(it.info.id)) }
            catch (e: Exception) { logger.error("Cannot enable ${it.info.id}", e) }
        }
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val world = client.level
            if (world !== previousWorld) {
                modules.reset(if (world == null) ResetReason.DISCONNECTED else ResetReason.WORLD_CHANGED)
                previousWorld = world
            }
            if (world != null && client.player != null) modules.tick()
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            save()
            modules.shutdown()
            previousWorld = null
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("solyanka")
                .executes { ctx ->
                    ctx.source.sendFeedback(Component.literal("solyanka foundation — Minecraft 26.2. Use /solyanka status."))
                    1
                }
                .then(literal("status").executes { ctx ->
                    modules.all().forEach {
                        ctx.source.sendFeedback(Component.literal("${it.info.id}: ${if (it.enabled) "on" else "off"}"))
                    }
                    ctx.source.sendFeedback(Component.literal("Upstream feature ports: pending; see docs/FEATURE_MATRIX.md"))
                    1
                })
                .then(literal("toggle").then(argument("module", StringArgumentType.word()).executes { ctx ->
                    val id = StringArgumentType.getString(ctx, "module")
                    val module = modules.get(id)
                    if (module == null) {
                        ctx.source.sendError(Component.literal("Unknown or unimplemented module: $id"))
                        0
                    } else {
                        try {
                            module.setEnabled(!module.enabled)
                            save()
                            ctx.source.sendFeedback(Component.literal("$id: ${if (module.enabled) "on" else "off"}"))
                            1
                        } catch (e: Exception) {
                            logger.error("Cannot toggle $id", e)
                            ctx.source.sendError(Component.literal("Module failed; see log"))
                            0
                        }
                    }
                })))
        }
        logger.info("solyanka foundation initialized for Minecraft 26.2")
    }
    private fun save() = config.save(modules.all().associate { it.info.id to it.enabled })
}
