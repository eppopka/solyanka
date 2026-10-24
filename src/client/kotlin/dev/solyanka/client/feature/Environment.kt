/* SPDX-License-Identifier: GPL-3.0-only
 * Stray CustomAmbience/AspectFov/CustomFog CC0 adapters; see NOTICE. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.fog.FogData
import net.fabricmc.loader.api.FabricLoader
object Environment {
 fun settings()=SolyankaClient.currentGeneral().features.environment
 fun ambience()=SolyankaClient.isEnabled(SolyankaClient.AMBIENCE) && Minecraft.getInstance().level!=null
 fun aspect(value:Float)=if(SolyankaClient.isEnabled(SolyankaClient.ASPECT) && Minecraft.getInstance().level!=null) value*settings().aspectRatio.toFloat() else value
 fun fog(data:FogData,distance:Int,atmospheric:Boolean) {
  // No Render wins. Never override blindness, darkness, fluids or powder-snow fog.
  if(!atmospheric || Suppression.on {fog} || !SolyankaClient.isEnabled(SolyankaClient.FOG)) return
  val s=EnvironmentLogic.fog(settings(),distance,FabricLoader.getInstance().isModLoaded("sodium"))
  data.environmentalStart=s.start;data.renderDistanceStart=s.start;data.environmentalEnd=s.end;data.renderDistanceEnd=s.end;data.skyEnd=s.end;data.cloudEnd=s.end;data.color.set(s.red,s.green,s.blue,s.alpha)
 }
}
