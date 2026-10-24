/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 capture backend, hardened solyanka lifecycle. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.pip.PipWin32
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.renderer.texture.DynamicTexture
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

object PictureInPicture {
 private data class Target(val info:PipWin32.WindowInfo,val generation:Long)
 private data class Frame(val target:Target,val pixels:IntArray,val width:Int,val height:Int)
 private val selected=AtomicReference<Target?>();private val nextFrame=AtomicReference<Frame?>()
 private var generation=0L
 @Volatile private var stopping=false
 @Volatile private var fps=30
 @Volatile var status="Select a window";private set
 private var worker:Thread?=null
 private var image:NativeImage?=null;private var texture:DynamicTexture?=null;private var texW=0;private var texH=0
 private var shown:Target?=null
 private val id=Identifier.fromNamespaceAndPath("solyanka","dynamic/pip")
 fun supported()=PipWin32.available()
 fun windows():List<PipWin32.WindowInfo> = if(!supported())emptyList() else runCatching {PipWin32.list()}.getOrElse {status="Window listing failed";emptyList()}
 fun choose(info:PipWin32.WindowInfo?) {
  reset();if(info!=null && S.isEnabled(S.PIP)){selected.set(Target(info,++generation));status="Selected: ${info.label()}"}
 }
 fun reset(){generation++;selected.set(null);nextFrame.set(null);shown=null;status=if(supported())"Select a window" else "Windows only";releaseTexture()}
 private fun releaseTexture(){if(texture!=null)Minecraft.getInstance().textureManager.release(id);texture=null;image=null;texW=0;texH=0}
 fun close(){reset();stopping=true;worker?.interrupt()}
 private fun ensureWorker(){
  if(worker!=null)return
  stopping=false
  worker=Thread({
   var owned:Target?=null
   try {
    while(!stopping){
     val start=System.nanoTime();val target=selected.get()
     if(target!=owned){PipWin32.close();owned=target}
     if(target!=null) {
      try {
       val size=IntArray(2);val pixels=PipWin32.capture(target.info,size,null)
       if(selected.get()===target){
        if(pixels!=null){nextFrame.set(Frame(target,pixels,size[0],size[1]));status="Capturing: ${target.info.application()}"}
        else {nextFrame.set(null);status="Capture unavailable — restore window or reselect after closing"}
       }
      }catch(e:Throwable){if(selected.get()===target){nextFrame.set(null);status="Capture failed — reselect window"}}
     }
     LockSupport.parkNanos((start+1_000_000_000L/fps-System.nanoTime()).coerceAtLeast(1_000_000L))
     Thread.interrupted()
    }
   }finally {PipWin32.close()}
  },"solyanka-window-capture").apply {isDaemon=true;start()}
 }
 fun tick(){
  val c=Minecraft.getInstance()
  if(!S.isEnabled(S.PIP) || c.level==null || c.player==null || !supported()){if(selected.get()!=null || texture!=null)reset();return}
  fps=S.currentGeneral().features.pip.fps
  if(selected.get()==null)return
  ensureWorker()
  if(!status.startsWith("Capturing:")){shown=null;releaseTexture()}
  val frame=nextFrame.getAndSet(null) ?: return
  if(frame.target!==selected.get())return
  try {
   if(texture==null || texW!=frame.width || texH!=frame.height){
    releaseTexture();val img=NativeImage(frame.width,frame.height,false);image=img
    val tex=DynamicTexture({"solyanka-window"},img);texture=tex;c.textureManager.register(id,tex);texW=frame.width;texH=frame.height
   }
   val img=image ?: return
   var i=0;repeat(frame.height){y->repeat(frame.width){x->img.setPixel(x,y,frame.pixels[i++])}}
   texture!!.upload();shown=frame.target
  }catch(e:Exception){status="Texture upload failed";shown=null;releaseTexture()}
 }
 fun register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","pip")){g,_->
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.pip;val h=s.hud
  if(!S.isEnabled(S.PIP) || c.level==null || !h.enabled || c.gui.hud.isHidden || c.gui.screen() is dev.solyanka.client.ui.HudEditorScreen)return@attachElementBefore
  val height=if(texW>0)(s.width.toDouble()*texH/texW).toInt().coerceIn(1,640) else s.width*9/16
  g.pose().pushMatrix();try{g.pose().translate(h.x.toFloat(),h.y.toFloat());g.pose().scale(h.scale.toFloat(),h.scale.toFloat())
   ChromeDraw.panel(g,0,0,s.width+6,height+6,0xd0181a22.toInt(),0xff5ca0bf.toInt(),4)
   if(texture!=null && shown===selected.get())g.blit(id,3,3,s.width,height,0f,1f,0f,1f)
   else g.text(c.font,if(supported())"Select a window in PiP settings" else "Windows only",7,7,-1,false)
  }finally{g.pose().popMatrix()}
 }}
}
