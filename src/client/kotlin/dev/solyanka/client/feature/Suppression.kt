/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills NoRender and its render/particle mixins; see NOTICE. */
package dev.solyanka.client.feature
import dev.solyanka.client.SolyankaClient
import dev.solyanka.utility.*
import net.fabricmc.fabric.api.client.rendering.v1.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.*
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.*
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.*

object Suppression {
 private val deadName=Regex(".* 0(?:/.*)?❤")
 fun deadName(text:String)=on { deadEntities } && deadName.matches(ChatLogic.plain(text))
 fun on(test:SuppressionSettings.()->Boolean)=SolyankaClient.isEnabled(SolyankaClient.SUPPRESSION) && SolyankaClient.currentGeneral().features.suppression.test()
 private fun texture(s:ItemStack,hash:Int):Boolean = s.item==Items.PLAYER_HEAD && s.get(DataComponents.PROFILE)?.partialProfile()?.properties()?.get("textures")?.any { it.value().hashCode()==hash }==true
 fun hide(e:Entity):Boolean {
  if(!SolyankaClient.isEnabled(SolyankaClient.SUPPRESSION)) return false
  val s=SolyankaClient.currentGeneral().features.suppression;val c=FeatureRuntime.context
  if(s.deadEntities && e is LivingEntity && !e.isAlive) return true
  if(s.fallingBlocks && e is FallingBlockEntity && c.area!="The Rift") return true
  if(s.lightning && e is LightningBolt || s.expOrbs && e is ExperienceOrb) return true
  if(s.treeBits && e is Display.BlockDisplay && c.area in setOf("Galatea","Hub","The Park")) {
   val state=e.blockState;val block=state.block;val sound=state.soundType
   if(block is LeavesBlock || block is RotatedPillarBlock && sound in setOf(SoundType.WOOD,SoundType.CHERRY_WOOD,SoundType.BAMBOO_WOOD,SoundType.NETHER_WOOD)) return true
  }
  if(c.dungeon) {
   if(s.guidedSheep && e is Sheep && e.health==8f) return true
   if(s.bonePlating && e is ItemEntity && e.item.item==Items.BONE_MEAL && e.item.hoverName.string=="Bone Meal") return true
   if(e is ArmorStand) {
    if(s.soulweaverSkulls && texture(e.getItemBySlot(EquipmentSlot.HEAD),-1020507406)) return true
    if(s.healerFairy && e.isMarker && texture(e.getItemBySlot(EquipmentSlot.MAINHAND),758129854)) return true
   }
  }
  if(s.cocoonAnimation && c.skyblock && e is ArmorStand && e.isMarker && texture(e.getItemBySlot(EquipmentSlot.HEAD),1843970301)) {
   // Fail closed while a Tarantula boss is visible, preserving its fight animation.
   val boss=Minecraft.getInstance().level?.entitiesForRendering()?.any { it.name.string.contains("Tarantula Broodfather") }==true
   if(!boss) return true
  }
  return false
 }
 fun particle(p:ClientboundLevelParticlesPacket):Boolean {
  if(!SolyankaClient.isEnabled(SolyankaClient.SUPPRESSION)) return false
  val s=SolyankaClient.currentGeneral().features.suppression;val type=p.particle.type
  fun match(t:ParticleType<*>,count:Int,speed:Float,x:Float,y:Float,z:Float)=type==t && p.count==count && p.maxSpeed==speed && p.xDist==x && p.yDist==y && p.zDist==z
  if(s.explosions && type in setOf(ParticleTypes.EXPLOSION,ParticleTypes.EXPLOSION_EMITTER,ParticleTypes.GUST,ParticleTypes.GUST_EMITTER_LARGE)) return true
  if(s.deadPoof && type==ParticleTypes.POOF && p.count==1 && p.maxSpeed==0f && listOf(p.xDist,p.yDist,p.zDist).all { it!=0f && it> -0.1f && it<0.1f }) return true
  if(s.mageBeam && type==ParticleTypes.FIREWORK && FeatureRuntime.context.dungeon) return true
  if(s.iceSpray && match(ParticleTypes.POOF,3,0f,0f,0f,0f)) return true
  if(s.powderCoating && p.particle is DustParticleOptions) {
   val color=(p.particle as DustParticleOptions).color
   if((color.x==1f && color.y==1f && color.z==1f || color.x==1f && color.y==0.6f && color.z==0f) && (match(ParticleTypes.DUST,0,1f,1f,1f,1f) || match(ParticleTypes.DUST,0,1f,1f,0.6f,0f))) return true
  }
  return false
 }
 fun register() {
  listOf(VanillaHudElements.ARMOR_BAR to { on { armorBar } },VanillaHudElements.FOOD_BAR to { on { foodBar } },VanillaHudElements.HEALTH_BAR to { on { healthBar==2 || healthBar==1 && FeatureRuntime.context.area!="The Rift" } }).forEach { (id,hide) ->
   HudElementRegistry.replaceElement(id) { old -> HudElement { g,d -> if(!hide()) old.extractRenderState(g,d) } }
  }
 }
}
