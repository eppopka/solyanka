/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills InfoTooltips, Utils.getMarketId and ShardData; see NOTICE. */
package dev.solyanka.client.feature

import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import java.net.URI
import java.net.http.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

object InfoTooltips {
    private val logger=LoggerFactory.getLogger("solyanka/tooltip")
    @Volatile private var museum:Map<String,String> = emptyMap()
    private val busy=AtomicBoolean(false)
    private var attempted=false
    private var nextAttempt=0L
    // The museum setting is the explicit opt-in. No player/item/server data or credentials are sent.
    private const val ENDPOINT="https://api.hypixel.net/v2/resources/skyblock/items"
    fun tick() {
        if(!SolyankaClient.isEnabled(SolyankaClient.TOOLTIPS) || !SolyankaClient.currentGeneral().tooltips.museum) return
        val now=System.nanoTime()
        if((attempted && now-nextAttempt<0) || !busy.compareAndSet(false,true)) return
        attempted=true;nextAttempt=now+Duration.ofMinutes(10).toNanos()
        Thread.startVirtualThread {
            try {
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build().use { http ->
                    val request=HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofSeconds(12))
                        .header("User-Agent","solyanka/0.4.0 (Minecraft 26.2)").GET().build()
                    val pending=http.sendAsync(request,BoundedBody.handler(8_388_608))
                    try {
                        val response=pending.get(15,java.util.concurrent.TimeUnit.SECONDS)
                        check(response.statusCode()==200) { "Museum HTTP ${response.statusCode()}" }
                        museum=TooltipLogic.museumData(String(response.body(),Charsets.UTF_8))
                    } finally { if(!pending.isDone) pending.cancel(true) }
                }
            } catch(e:Exception) { logger.warn("Museum data unavailable; keeping last-good session cache, retry in 10 minutes",e) }
            finally { busy.set(false) }
        }
    }
    private fun ints(tag:CompoundTag)=tag.keySet().associateWith { tag.getIntOr(it,0) }
    fun register() {
        ItemTooltipCallback.EVENT.register { stack,_,_,lines ->
            if(SolyankaClient.isEnabled(SolyankaClient.TOOLTIPS)) {
                try {
                    val data=stack.get(DataComponents.CUSTOM_DATA)?.copyTag()
                    if(data!=null) {
                        val facts=ItemFacts(id=data.getStringOr("id",""),boost=data.getIntOr("baseStatBoostPercentage",0),
                            tier=data.getIntOr("item_tier",0),timestamp=data.getLongOr("timestamp",0L),
                            color=stack.get(DataComponents.DYED_COLOR)?.rgb(),donated=data.getByteOr("donated_museum",0.toByte())!=0.toByte(),
                            petInfo=data.getStringOr("petInfo",""),runes=ints(data.getCompoundOrEmpty("runes")),
                            enchantments=ints(data.getCompoundOrEmpty("enchantments")),potion=data.getStringOr("potion",""),
                            potionLevel=data.getIntOr("potion_level",0),name=ChatLogic.plain(stack.hoverName.string),
                            lore=stack.get(DataComponents.LORE)?.lines()?.map { ChatLogic.plain(it.string) } ?: emptyList())
                        TooltipLogic.lines(facts,SolyankaClient.currentGeneral().tooltips,museum).forEach { line ->
                            val value=Component.literal(line.value).withStyle(if(line.perfect) ChatFormatting.RED else ChatFormatting.GOLD)
                            if(line.perfect) value.withStyle(ChatFormatting.BOLD)
                            lines.add(Component.literal("[solyanka] ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal("${line.label}: ").withStyle(ChatFormatting.AQUA)).append(value))
                        }
                    }
                } catch(e:Exception) {
                    // A malformed server item must not crash inventory rendering or log every frame.
                    logger.debug("Skipped malformed item tooltip",e)
                }
            }
        }
    }
}
