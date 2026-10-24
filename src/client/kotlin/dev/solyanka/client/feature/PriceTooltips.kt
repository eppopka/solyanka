/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills PriceTooltips, RecipeData and NoFrillsAPI; see NOTICE. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import com.google.gson.*
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.*
import net.minecraft.network.chat.Component
import java.net.URI
import java.net.http.*
import java.time.Duration
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

object PriceTooltips {
 private val log=LoggerFactory.getLogger("solyanka/prices")
 @Volatile private var prices:Map<String,MarketPrice> = emptyMap()
 @Volatile private var updated=0L
 @Volatile private var status="Prices not downloaded"
 private val busy=AtomicBoolean(false)
 private var next=0L
 private val gson=GsonBuilder().setPrettyPrinting().create()
 private val recipes=mutableMapOf<String,MutableList<CraftRecipe>>()
 private val paid=mutableMapOf<String,Long>()
 private val path by lazy { FabricLoader.getInstance().configDir.resolve("solyanka-market.json") }
 private var loaded=false
 private var writable=true
 private var localError:String?=null
 private fun load() {
  if(loaded) return;loaded=true
  try { if(Files.exists(path)) {
   require(Files.size(path)<=8_388_608) { "Market cache exceeds 8 MiB" }
   val cache=MarketCacheCodec.decode(Files.readString(path));paid.putAll(cache.paid);cache.recipes.forEach {(id,list)->recipes[id]=list.toMutableList()}
  } } catch(e:Exception) { writable=false;localError="Local market cache invalid; writes disabled (see latest.log)";log.warn("Market cache is corrupt; not overwriting it",e) }
 }
 private fun save() {
  if(!writable) return
  var tmp:Path?=null
  try { Files.createDirectories(path.parent);tmp=Files.createTempFile(path.parent,"solyanka-market-",".tmp");Files.writeString(tmp,MarketCacheCodec.encode(paid,recipes));Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);localError=null }
  catch(e:Exception) { localError="Local market data not saved (see latest.log)";log.warn("Could not save observed recipes/purchases",e) }
  finally { tmp?.let { Files.deleteIfExists(it) } }
 }
 private fun get(http:HttpClient,url:String):String {
  val request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("User-Agent","solyanka/0.5.0 (Minecraft 26.2)").GET().build()
  val pending=http.sendAsync(request,BoundedBody.handler(12_000_000))
  try { val r=pending.get(18,java.util.concurrent.TimeUnit.SECONDS);check(r.statusCode()==200) { "HTTP ${r.statusCode()}" };return String(r.body(),Charsets.UTF_8) }
  finally { if(!pending.isDone) pending.cancel(true) }
 }
 fun tick() {
  if(!SolyankaClient.isEnabled(SolyankaClient.PRICES)) return
  load()
  if(!SolyankaClient.currentGeneral().features.prices.network) return
  val now=System.nanoTime();if(now<next || !busy.compareAndSet(false,true)) return
  next=now+Duration.ofMinutes(5).toNanos()
  Thread.startVirtualThread {
   try { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build().use { http ->
    try { prices=PriceLogic.parsePricing(get(http,"https://whatyouth.ing/api/nofrills/v2/economy/get-item-pricing/"));updated=System.currentTimeMillis();status="NoFrills pricing" }
    catch(primary:Exception) {
     val result=mutableMapOf<String,MarketPrice>();val missing=mutableListOf<String>()
     try {
      result.putAll(PriceLogic.parseHypixelBazaar(get(http,"https://api.hypixel.net/v2/skyblock/bazaar")))
     } catch(e:Exception) { missing.add("Bazaar");log.debug("Bazaar fallback failed",e) }
     try {
      val root=JsonParser.parseString(get(http,"https://api.hypixel.net/v2/resources/skyblock/items")).asJsonObject
      check(root.get("success")?.asBoolean==true)
      root.getAsJsonArray("items").forEach { v->val o=v.asJsonObject;val id=o.get("id")?.asString ?: return@forEach;val npc=o.get("npc_sell_price")?.asDouble ?: 0.0;if(npc>0 && npc.isFinite()) result[id]=(result[id] ?: MarketPrice()).copy(npc=npc) }
     } catch(e:Exception) { missing.add("NPC");log.debug("NPC fallback failed",e) }
     try { JsonParser.parseString(get(http,"https://moulberry.codes/lowestbin.json")).asJsonObject.entrySet().forEach { (id,v)->val bin=v.asDouble;if(bin>0 && bin.isFinite()) result[id]=(result[id] ?: MarketPrice()).copy(bin=bin) } }
     catch(e:Exception) { missing.add("BIN");log.debug("BIN fallback failed",e) }
     missing.add("Motes")
     check(result.isNotEmpty()) { "All public price providers unavailable" }
     prices=result.toMap();updated=System.currentTimeMillis();status="Public fallback; unavailable: "+missing.joinToString()
    }
   } } catch(e:Exception) { status="Price refresh failed; "+if(prices.isEmpty()) "no cache" else "showing stale cache";log.warn(status,e) }
   finally { busy.set(false) }
  }
 }
 private fun data(s:ItemStack)=s.get(DataComponents.CUSTOM_DATA)?.copyTag()
 private fun lore(s:ItemStack)=s.get(DataComponents.LORE)?.lines()?.map { ChatLogic.plain(it.string) } ?: emptyList()
 private fun id(s:ItemStack):String {
  val d=data(s) ?: return ""
  val facts=ItemFacts(id=d.getStringOr("id",""),boost=d.getIntOr("baseStatBoostPercentage",0),tier=d.getIntOr("item_tier",0),petInfo=d.getStringOr("petInfo",""),runes=d.getCompoundOrEmpty("runes").let { c->c.keySet().associateWith { c.getIntOr(it,0) } },enchantments=d.getCompoundOrEmpty("enchantments").let { c->c.keySet().associateWith { c.getIntOr(it,0) } },potion=d.getStringOr("potion",""),potionLevel=d.getIntOr("potion_level",0),name=ChatLogic.plain(s.hoverName.string),lore=lore(s))
  return TooltipLogic.marketId(facts)
 }
 fun afterSlot(slot:Int?) {
  val screen=Minecraft.getInstance().gui.screen() as? ContainerScreen ?: return
  if(slot==null || slot==screen.menu.slots.lastOrNull()?.index) observeRecipe(screen)
 }
 private fun observeRecipe(screen:ContainerScreen) {
  if(!SolyankaClient.isEnabled(SolyankaClient.PRICES) || !SolyankaClient.currentGeneral().features.prices.craftCost || !FeatureRuntime.context.skyblock) return
  if(screen.menu.rowCount!=6) return
  val slots=screen.menu.slots;if(slots.size<54 || slots[23].item.item!=Items.CRAFTING_TABLE || ChatLogic.plain(slots[23].item.hoverName.string)!="Crafting Table") return
  val result=slots[25].item;val output=id(result);if(result.isEmpty || output.isBlank()) return
  val ingredients=mutableMapOf<String,Int>();for(row in 0..2) for(col in 0..2) {
   val stack=slots[10+row*9+col].item;if(stack.isEmpty) continue;val key=id(stack);if(key.isBlank()) return;ingredients[key]=(ingredients[key] ?: 0)+stack.count
  }
  if(ingredients.isEmpty()) return
  load();val recipe=CraftRecipe(result.count,ingredients);val list=recipes.getOrPut(output) { mutableListOf() };if(recipe !in list) { list.add(recipe);save() }
 }
 fun purchase(slot:Slot) {
  if(!SolyankaClient.isEnabled(SolyankaClient.PRICES) || !SolyankaClient.currentGeneral().features.prices.pricePaid) return
  val screen=Minecraft.getInstance().gui.screen() as? ContainerScreen ?: return
  if(ChatLogic.plain(screen.title.string)!="Confirm Purchase" || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(slot.item.item).toString()!="minecraft:green_terracotta") return
  val cost=PriceLogic.purchaseCost(lore(slot.item));if(cost<=0) return
  val uuid=screen.menu.slots.firstNotNullOfOrNull { data(it.item)?.getStringOr("uuid","")?.takeIf(String::isNotBlank) } ?: return
  load();paid[uuid]=cost;save()
 }
 fun register() { ItemTooltipCallback.EVENT.register { stack,_,_,lines ->
  if(SolyankaClient.isEnabled(SolyankaClient.PRICES)) try {
   load();val key=id(stack);if(key.isNotBlank()) {
    val s=SolyankaClient.currentGeneral().features.prices;val quantity=PriceLogic.quantity(stack.count,lore(stack));val snapshot=prices;val price=snapshot[key]
    fun line(label:String,unit:Double,n:Int=quantity) { if(unit>0 && unit.isFinite()) lines.add(Component.literal("[solyanka] $label: "+PriceLogic.format(unit*n)+(if(n>1) " ($n× "+PriceLogic.format(unit)+")" else ""))) }
    if(s.npc) price?.let { line("NPC Price",it.npc) }
    if(s.bazaar) price?.let { line("Bazaar Buy",it.buy);line("Bazaar Sell",it.sell) }
    if(s.auction) price?.let { line("Lowest BIN",it.bin) }
    if(s.mote && FeatureRuntime.context.area=="The Rift") price?.let { line("Motes Price",it.mote*(1+0.05*s.burgers)) }
    if(s.pricePaid) data(stack)?.getStringOr("uuid","")?.let { paid[it] }?.let { line("Price Paid",it.toDouble(),1) }
    if(s.craftCost) PriceLogic.craft(key,snapshot,recipes)?.let { line("Craft Cost ~",it) }
    if(!s.network) lines.add(Component.literal("[solyanka] Online pricing disabled in settings"))
    else if(updated==0L) lines.add(Component.literal("[solyanka] $status"))
    if(updated>0L) lines.add(Component.literal("[solyanka] $status · ${((System.currentTimeMillis()-updated).coerceAtLeast(0)/1000)}s old"))
    if(s.network && price==null && snapshot.isNotEmpty()) lines.add(Component.literal("[solyanka] No market data for $key"))
    if(s.pricePaid || s.craftCost) localError?.let { lines.add(Component.literal("[solyanka] $it")) }
   }
  } catch(e:Exception) { log.debug("Malformed priced item",e) }
 } }
}
