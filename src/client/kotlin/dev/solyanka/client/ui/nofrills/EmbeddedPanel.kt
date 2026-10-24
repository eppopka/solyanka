/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills
import dev.solyanka.ui.PanelDrag
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.*
/** Both ordinary settings and the photo knife browser are modals on the existing feature screen. */
interface EmbeddedPanel {
 fun position():PanelDrag.Point?
 fun placeAt(point:PanelDrag.Point?)
 fun mount(width:Int,height:Int)
 fun render(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float)
 fun dispose()
 fun focused():GuiEventListener?
 fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean
 fun mouseReleased(event:MouseButtonEvent):Boolean
 fun mouseDragged(event:MouseButtonEvent,dx:Double,dy:Double):Boolean
 fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean
 fun keyPressed(event:KeyEvent):Boolean
 fun charTyped(event:CharacterEvent):Boolean
}
