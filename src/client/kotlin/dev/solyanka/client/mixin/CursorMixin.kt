/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills MouseHandlerMixin; see NOTICE. */
package dev.solyanka.client.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.*
import com.mojang.blaze3d.platform.Window
import dev.solyanka.client.SolyankaClient
import dev.solyanka.client.feature.FeatureRuntime
import net.minecraft.client.MouseHandler
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MouseHandler::class)
abstract class CursorMixin {
    @Shadow private var xpos:Double=0.0
    @Shadow private var ypos:Double=0.0
    @Inject(method=["grabMouse"],at=[At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I",ordinal=0)])
    private fun solyankaRemember(ci:CallbackInfo) { if(SolyankaClient.isEnabled(SolyankaClient.CURSOR)) FeatureRuntime.cursor.capture(xpos,ypos) }
    @WrapOperation(method=["releaseMouse"],at=[At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V")])
    private fun solyankaRestore(window:Window,mode:Int,x:Double,y:Double,original:Operation<Void>) {
        val pos=if(FeatureRuntime.cursorActive()) FeatureRuntime.cursor.position(window.screenWidth,window.screenHeight) else null
        if(pos==null) original.call(window,mode,x,y)
        else {
            xpos=pos.first;ypos=pos.second
            original.call(window,mode,xpos,ypos);GLFW.glfwSetCursorPos(window.handle(),xpos,ypos)
        }
    }
}
