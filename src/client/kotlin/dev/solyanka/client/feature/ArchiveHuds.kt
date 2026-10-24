/* SPDX-License-Identifier: GPL-3.0-only; user-provided impl.zip Timer/DvdBounce adaptations. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.HudEditorScreen
import dev.solyanka.client.ui.nofrills.NoFrillsClickGui
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt
object ArchiveHuds {
 val clock=StopwatchClock();val dvd=DvdMotion()
 private var soundPending=false
 private val colors=listOf(0xff7c71ff.toInt(),0xff3fc9ad.toInt(),0xffff6384.toInt(),0xffffb84d.toInt(),0xff4aa8ff.toInt(),0xffd26cff.toInt())
 fun resetTimer() {clock.reset()}
 fun resetDvd() {dvd.reset();soundPending=false}
 fun input(device:InputDevice,key:Int):Boolean {
  val c=Minecraft.getInstance();if(!S.isEnabled(S.STOPWATCH) || c.level==null || c.player==null || !c.isWindowActive || c.gui.screen()!=null || !S.currentGeneral().features.stopwatch.matches(device,key)) return false
  clock.press()?.let {c.player?.sendSystemMessage(Component.literal("[solyanka] Timer: ${StopwatchClock.formatNanos(it)}"))};return true
 }
 fun tick() {if(soundPending) {soundPending=false;if(S.isEnabled(S.DVD) && S.currentGeneral().features.dvd.sound) Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(),1.55f,.72f))}}
 private fun timer(g:GuiGraphicsExtractor,sample:Boolean=false,atX:Int?=null,atY:Int?=null) {
  val c=Minecraft.getInstance();val h=S.currentGeneral().features.stopwatch.hud;val scale=h.scale.toFloat()
  val x=atX ?: h.x.coerceIn(0,(c.window.guiScaledWidth-160*scale).roundToInt().coerceAtLeast(0));val y=atY ?: h.y.coerceIn(0,(c.window.guiScaledHeight-34*scale).roundToInt().coerceAtLeast(0))
  val state=if(sample) StopwatchClock.State.RUNNING else clock.state;val elapsed=if(sample) 5_123_000_000L else clock.elapsedNanos()
  g.pose().pushMatrix()
  try {g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale);ChromeDraw.panel(g,0,0,160,34,0xcd0c1119.toInt(),0xff5f869f.toInt(),6);g.text(c.font,"Timer - "+when(state) {StopwatchClock.State.IDLE->"Ready";StopwatchClock.State.RUNNING->"Running";StopwatchClock.State.STOPPED->"Stopped"},8,6,when(state) {StopwatchClock.State.RUNNING->0xff55ff55.toInt();StopwatchClock.State.STOPPED->0xffffff55.toInt();else->0xffaaaaaa.toInt()},false);g.text(c.font,StopwatchClock.formatNanos(elapsed),8,19,h.color,false)} finally {g.pose().popMatrix()}
 }
 fun editor(g:GuiGraphicsExtractor,key:String,x:Int,y:Int):Boolean {if(key!="archive.stopwatch") return false;timer(g,true,x,y);return true}
 private fun bounce(g:GuiGraphicsExtractor,now:Long) {
  val c=Minecraft.getInstance();val s=S.currentGeneral().features.dvd;val sw=c.window.guiScaledWidth;val sh=c.window.guiScaledHeight
  if(dvd.step(now,sw,sh,s) && s.sound) soundPending=true
  val x=dvd.x.roundToInt();val y=dvd.y.roundToInt();val w=s.width.coerceAtMost(sw);val h=s.height.coerceAtMost(sh);val color=colors[dvd.colorIndex]
  ChromeDraw.panel(g,x,y,w,h,0xe1070a10.toInt(),color,s.radius)
  val label=c.font.plainSubstrByWidth(s.text.ifBlank {"DVD"},(w-14).coerceAtLeast(0))
  g.text(c.font,label,x+(w-c.font.width(label))/2,y+(h-8)/2,color,true)
  if(s.counter && sh>=14) {val text="CORNERS ${dvd.cornerHits}";val tw=c.font.width(text)+10;val tx=(x+w-tw).coerceIn(0,(sw-tw).coerceAtLeast(0));val ty=(y+h+4).coerceIn(0,sh-14);ChromeDraw.panel(g,tx,ty,tw.coerceAtMost(sw),14,0xcd05080d.toInt(),0x335f869f,5);g.text(c.font,text,tx+5,ty+3,0xffd3d8e5.toInt(),false)}
  val alpha=if(s.flash) dvd.flashAlpha(now) else 0
  if(alpha>0) {g.fill(0,0,sw,sh,(color and 0xffffff) or (alpha shl 24));val msg="CORNER HIT #${dvd.cornerHits}";g.text(c.font,msg,((sw-c.font.width(msg))/2).coerceAtLeast(0),20.coerceAtMost((sh-9).coerceAtLeast(0)),color,true)}
 }
 private fun render(g:GuiGraphicsExtractor) {
  val c=Minecraft.getInstance();val now=System.nanoTime();val screen=c.gui.screen()
  if(c.level==null || c.player==null || c.gui.hud.isHidden || screen is HudEditorScreen || screen!=null && screen !is ChatScreen && screen !is NoFrillsClickGui) {dvd.pause(now);return}
  if(S.isEnabled(S.STOPWATCH) && S.currentGeneral().features.stopwatch.hud.enabled) timer(g)
  if(S.isEnabled(S.DVD)) bounce(g,now) else dvd.pause(now)
 }
 fun register() {HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("solyanka","archive_huds")) {g,_->render(g)}}
}
