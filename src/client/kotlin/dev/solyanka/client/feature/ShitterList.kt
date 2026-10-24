/* SPDX-License-Identifier: GPL-3.0-only
 * Independently implemented archive-compatible Party Finder blocklist workflow.
 * Original feature credited by impl.zip: qwertycry/Shitterlist. No upstream code copied. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.utility.*
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.*
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.*
import java.time.Duration
import java.util.concurrent.*

object ShitterList {
 private val store by lazy {BlocklistStore(FabricLoader.getInstance().configDir.resolve("solyanka-shitters.json")).also {it.load()}}
 private val executor=ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,ArrayBlockingQueue(32),{r->Thread(r,"solyanka-profile-lookup").apply {isDaemon=true}},ThreadPoolExecutor.AbortPolicy())
 private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build()
 private var generation=0L
 private val cooldowns=linkedMapOf<String,Long>()
 private data class Action(val uuid:String,val name:String,val reason:String,val at:Long,val generation:Long,var warned:Boolean=false)
 private val actions=mutableListOf<Action>()
 private val resolving=mutableSetOf<String>()
 val error:String? get()=store.error
 fun reset(){generation++;actions.clear();resolving.clear();cooldowns.clear()}
 fun close(){reset();executor.shutdownNow()}
 private fun local(t:String){Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[solyanka] $t"))}
 private fun now()=System.nanoTime()/1_000_000L
 private fun profile(name:String,done:(Pair<String,String>?)->Unit) {
  require(BlocklistCodec.name(name))
  val c=Minecraft.getInstance();val info=c.connection?.listedOnlinePlayers?.firstOrNull {it.profile.name.equals(name,true)}?.profile
  if(info!=null){done(BlocklistCodec.uuid(info.id.toString())!! to info.name);return}
  if(!S.currentGeneral().features.blocklist.network){done(null);return}
  try {executor.execute {
   val found=runCatching {
    val req=HttpRequest.newBuilder(URI("https://api.mojang.com/users/profiles/minecraft/$name")).timeout(Duration.ofSeconds(7)).GET().build()
    val response=http.send(req,HttpResponse.BodyHandlers.ofInputStream())
    response.body().use {body->
     if(response.statusCode()!=200)return@use null
     val data=body.readNBytes(65537);require(data.size<=65536)
     val j=JsonParser.parseString(data.toString(Charsets.UTF_8)).asJsonObject
     val id=BlocklistCodec.uuid(j["id"].asString) ?: return@use null;val nick=j["name"].asString
     if(BlocklistCodec.name(nick))id to nick else null
    }
   }.getOrNull()
   c.execute{done(found)}
  }}catch(e:RejectedExecutionException){local("Profile queue is full; request skipped.");done(null)}
 }
 private fun joined(raw:String,overlay:Boolean){
  if(overlay || !S.isEnabled(S.BLOCKLIST) || !FeatureRuntime.context.skyblock || store.error!=null)return
  val name=BlocklistCodec.joiner(raw) ?: return
  if(resolving.size>=32 || !resolving.add(name.lowercase()))return
  val gen=generation;val world=Minecraft.getInstance().level
  profile(name){p->
   resolving.remove(name.lowercase())
   if(gen!=generation || world!==Minecraft.getInstance().level || !S.isEnabled(S.BLOCKLIST) || !FeatureRuntime.context.skyblock || p==null)return@profile
   val entry=store.rows.firstOrNull {it.uuid==p.first} ?: return@profile
   if(p.first==BlocklistCodec.uuid(Minecraft.getInstance().player?.uuid.toString()))return@profile
   val time=now();if(time-(cooldowns[p.first] ?: -10000L)<5000)return@profile
   cooldowns[p.first]=time;if(cooldowns.size>512)cooldowns.remove(cooldowns.keys.first())
   local("Blocklisted Party Finder joiner: ${p.second}. Reason: ${entry.reason}")
   if(S.currentGeneral().features.blocklist.autoKick && actions.size<32)actions.add(Action(p.first,p.second,entry.reason,time,gen))
  }
 }
 fun tick(){
  if(!S.isEnabled(S.BLOCKLIST) || !FeatureRuntime.context.skyblock){if(actions.isNotEmpty() || resolving.isNotEmpty())reset();return}
  val s=S.currentGeneral().features.blocklist
  if(!s.autoKick){actions.clear();return}
  val connection=Minecraft.getInstance().player?.connection ?: return
  val time=now();val it=actions.iterator()
  while(it.hasNext()){
   val a=it.next();if(a.generation!=generation || store.rows.none {e->e.uuid==a.uuid}){it.remove();continue}
   val elapsed=time-a.at
   if(elapsed>10000){it.remove();continue}
   if(s.partyWarning && !a.warned && elapsed>=s.delayMs*8/11){connection.sendCommand("pc Blocklisted player ${a.name} joined. Reason: ${a.reason.take(80)}");a.warned=true}
   if(elapsed>=s.delayMs){connection.sendCommand("p kick ${a.name}");it.remove()}
  }
 }
 private fun add(name:String,reason:String){
  if(!BlocklistCodec.name(name) || reason.length>256 || reason.any {it.isISOControl()}){local("Invalid nickname or reason (max 256 characters).");return}
  val gen=generation
  profile(name){p->if(gen!=generation)return@profile
   if(p==null){local("Profile not verified. Player must be online, or enable Mojang Lookups.");return@profile}
   runCatching {store.save(store.rows.filterNot {it.uuid==p.first}+BlockEntry(p.first,p.second,reason))}.onSuccess {local("Added/updated ${p.second}.")}.onFailure {local(it.message ?: "List write failed")}
  }
 }
 private fun remove(name:String){
  val rows=store.rows.filterNot {it.nickname.equals(name,true) || it.uuid==BlocklistCodec.uuid(name)}
  val removed=rows.size<store.rows.size
  runCatching {store.save(rows)}.onSuccess {local(if(removed)"Removed $name" else "Removal saved for $name")}.onFailure {local(it.message ?: "List write failed")}
 }
 fun register(){
  ClientReceiveMessageEvents.GAME.register {m,o->joined(m.string,o)};ClientReceiveMessageEvents.GAME_CANCELED.register {m,o->joined(m.string,o)}
  ClientCommandRegistrationCallback.EVENT.register {d,_->
   d.register(literal("sl").executes{local("/sl add <name> [reason] | remove <name/uuid> | list [page] | importlegacy. List: config/solyanka-shitters.json");1}
    .then(literal("add").then(argument("name",StringArgumentType.word()).executes {add(StringArgumentType.getString(it,"name"),"1");1}.then(argument("reason",StringArgumentType.greedyString()).executes {add(StringArgumentType.getString(it,"name"),StringArgumentType.getString(it,"reason"));1})))
    .then(literal("remove").then(argument("name",StringArgumentType.word()).executes {remove(StringArgumentType.getString(it,"name"));1}))
    .then(literal("list").executes{list(1);1}.then(argument("page",IntegerArgumentType.integer(1)).executes{list(IntegerArgumentType.getInteger(it,"page"));1}))
    .then(literal("importlegacy").executes{
     runCatching {
      val path=FabricLoader.getInstance().configDir.resolve("shitters.txt");require(java.nio.file.Files.size(path)<=2*1024*1024)
      val legacy=BlocklistCodec.decode(java.nio.file.Files.readString(path));store.save((store.rows+legacy).associateBy {it.uuid}.values.toList())
     }.onSuccess{local("Legacy entries imported. Original shitters.txt was not modified.")}.onFailure{local(it.message ?: "Import failed")};1
    }))
  }
 }
 private fun list(page:Int){val all=store.rows;val pages=((all.size+9)/10).coerceAtLeast(1);val p=page.coerceIn(1,pages);local("Blocklist $p/$pages (${all.size} entries)");all.drop((p-1)*10).take(10).forEach{local("${it.nickname}: ${it.reason}")};error?.let(::local)}
}
