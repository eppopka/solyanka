/* SPDX-License-Identifier: GPL-3.0-only; Stray Cs2KnifeScreen preview lifecycle adaptation. */
package dev.solyanka.client.ui
import dev.solyanka.client.cs2.Cs2Knives
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture
/** Screen-owned, bounded CPU futures and GPU textures. Upload/release only during GUI/client-thread callbacks. */
class KnifePreviewCache {
 data class Image(val id:Identifier,val width:Int,val height:Int)
 private data class Job(val future:CompletableFuture<Cs2Knives.PreviewImage?>,val epoch:Int)
 private val jobs=linkedMapOf<String,Job>();private val textures=linkedMapOf<String,Image>();private val misses=linkedSetOf<String>()
 private var closed=false;private var generation=Cs2Knives.assetGeneration();private var sequence=0
 private val session=java.util.UUID.randomUUID().toString().replace("-","")
 fun poll() {
  if(generation!=Cs2Knives.assetGeneration()){clear();generation=Cs2Knives.assetGeneration()}
  val done=jobs.filterValues {it.future.isDone}.keys.toList()
  done.forEach {key->val job=jobs.remove(key) ?: return@forEach
   val image=runCatching {job.future.join()}.getOrNull()
   if(closed || job.epoch!=generation)return@forEach
   if(image==null){misses.add(key);while(misses.size>256)misses.remove(misses.first());return@forEach}
   val pixels=NativeImage(image.width(),image.height(),false)
   try {
    for(y in 0 until image.height())for(x in 0 until image.width())pixels.setPixel(x,y,image.pixels()[y*image.width()+x])
    val texture=DynamicTexture({"solyanka knife preview"},pixels)
    val id=Identifier.fromNamespaceAndPath("solyanka","knife_menu/$session/${sequence++}")
    try{Minecraft.getInstance().textureManager.register(id,texture)}catch(e:Exception){texture.close();throw e}
    textures[key]=Image(id,image.width(),image.height())
    while(textures.size>40){val first=textures.keys.first();Minecraft.getInstance().textureManager.release(textures.remove(first)!!.id)}
   }catch(e:Exception){if(!pixels.isClosed)pixels.close();misses.add(key)}
  }
 }
 fun image(knife:String,finish:String,seed:Int,wear:Double,thumbnail:Boolean,request:Boolean=true):Image? {
  val key=if(thumbnail)"tile|$knife|$finish"else "render|$knife|$finish|$seed|${Math.round(wear*10000)}"
  textures[key]?.let {return it}
  if(request && !closed && key !in misses && key !in jobs && jobs.size<2 && Cs2Knives.library()!=null)jobs[key]=Job(Cs2Knives.preview(knife,finish,seed,wear.toFloat(),thumbnail),generation)
  return null
 }
 fun clear(){jobs.values.forEach {it.future.cancel(false)};jobs.clear();textures.values.forEach {Minecraft.getInstance().textureManager.release(it.id)};textures.clear();misses.clear()}
 fun close(){closed=true;clear()}
}
