/* SPDX-License-Identifier: GPL-3.0-only; Stray ChatChrome CC0 adaptation. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.mixin.ChatComponentAccessor
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ChatComponent
object ThemedChat {
 private val scopedFills=ThreadLocal.withInitial {false}
 fun enabled()=S.isEnabled(S.THEMED_CHAT) && !Minecraft.getInstance().gui.hud.isHidden
 fun begin(g:GuiGraphicsExtractor,chat:ChatComponent,ticks:Int,mode:ChatComponent.DisplayMode):Boolean {
  val previous=scopedFills.get();scopedFills.set(false)
  if(!enabled()) return previous
  val a=chat as ChatComponentAccessor;val page=chat.linesPerPage.coerceIn(1,1000)
  val live=ChatPaneLayout.visible(a.solyankaLines().map {ticks-it.addedTime()},mode.foreground,page,a.solyankaScroll())
  if(live==0 && !mode.foreground) return previous
  val box=ChatPaneLayout.pane(Minecraft.getInstance().window.guiScaledHeight,a.solyankaChatWidth(),a.solyankaChatScale(),a.solyankaLineHeight(),if(mode.foreground) page else live)
  val s=S.currentGeneral().features.themedChat
  ChromeDraw.panel(g,box.x,box.y,box.width,box.height,VisualChrome.alpha(s.backgroundColor,s.opacity),VisualChrome.alpha(s.borderColor,s.opacity),if(s.rounded) 6 else 0)
  scopedFills.set(true);return previous
 }
 fun end(previous:Boolean) {scopedFills.set(previous)}
 @JvmStatic fun skipFill(x0:Int,x1:Int)=enabled() && scopedFills.get() && x1-x0>4
 @JvmStatic fun input(g:GuiGraphicsExtractor,x0:Int,y0:Int,x1:Int,y1:Int,color:Int) {
  val s=S.currentGeneral().features.themedChat
  if(!enabled() || !s.input) {g.fill(x0,y0,x1,y1,color);return}
  ChromeDraw.panel(g,x0,y0,x1-x0,y1-y0,VisualChrome.alpha(s.inputColor,s.opacity),VisualChrome.alpha(s.borderColor,s.opacity),if(s.rounded) 4 else 0)
 }
}
