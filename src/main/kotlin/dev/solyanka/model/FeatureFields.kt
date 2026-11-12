/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.model

import dev.solyanka.utility.FeatureSettings

/** Typed field descriptors for the generic editors. No JSON serialization on a UI edit. */
sealed interface FeatureField {
 val name:String
 data class Flag(override val name:String,val value:Boolean,val set:(FeatureSettings,Boolean)->FeatureSettings):FeatureField
 data class Integer(override val name:String,val value:Int,val set:(FeatureSettings,Int)->FeatureSettings):FeatureField
 data class Decimal(override val name:String,val value:Double,val set:(FeatureSettings,Double)->FeatureSettings):FeatureField
}
object FeatureFields {
 fun fields(f:FeatureSettings,section:String):List<FeatureField> = when(section) {
  "environment" -> listOf(
   FeatureField.Integer("clock",f.environment.clock) {s,v->s.copy(environment=s.environment.copy(clock=v))},
   FeatureField.Integer("weather",f.environment.weather) {s,v->s.copy(environment=s.environment.copy(weather=v))},
   FeatureField.Flag("ignoreWeatherBlocks",f.environment.ignoreWeatherBlocks) {s,v->s.copy(environment=s.environment.copy(ignoreWeatherBlocks=v))},
   FeatureField.Flag("precipitation",f.environment.precipitation) {s,v->s.copy(environment=s.environment.copy(precipitation=v))},
   FeatureField.Decimal("gradient",f.environment.gradient) {s,v->s.copy(environment=s.environment.copy(gradient=v))},
   FeatureField.Decimal("aspectRatio",f.environment.aspectRatio) {s,v->s.copy(environment=s.environment.copy(aspectRatio=v))},
   FeatureField.Decimal("fogStart",f.environment.fogStart) {s,v->s.copy(environment=s.environment.copy(fogStart=v))},
   FeatureField.Decimal("fogEnd",f.environment.fogEnd) {s,v->s.copy(environment=s.environment.copy(fogEnd=v))},
   FeatureField.Decimal("fogDensity",f.environment.fogDensity) {s,v->s.copy(environment=s.environment.copy(fogDensity=v))},
   FeatureField.Integer("fogColor",f.environment.fogColor) {s,v->s.copy(environment=s.environment.copy(fogColor=v))},
  )
  "map" -> listOf(
   FeatureField.Integer("x",f.map.x) {s,v->s.copy(map=s.map.copy(x=v))},
   FeatureField.Integer("y",f.map.y) {s,v->s.copy(map=s.map.copy(y=v))},
   FeatureField.Flag("disableInBoss",f.map.disableInBoss) {s,v->s.copy(map=s.map.copy(disableInBoss=v))},
   FeatureField.Flag("alert300",f.map.alert300) {s,v->s.copy(map=s.map.copy(alert300=v))},
   FeatureField.Flag("highlightPortal",f.map.highlightPortal) {s,v->s.copy(map=s.map.copy(highlightPortal=v))},
   FeatureField.Flag("paul",f.map.paul) {s,v->s.copy(map=s.map.copy(paul=v))},
   FeatureField.Flag("remaining",f.map.remaining) {s,v->s.copy(map=s.map.copy(remaining=v))},
   FeatureField.Flag("roomSecrets",f.map.roomSecrets) {s,v->s.copy(map=s.map.copy(roomSecrets=v))},
   FeatureField.Flag("announceKeySpawn",f.map.announceKeySpawn) {s,v->s.copy(map=s.map.copy(announceKeySpawn=v))},
  )
  "puzzles" -> listOf(
   FeatureField.Flag("optimizeIce",f.puzzles.optimizeIce) {s,v->s.copy(puzzles=s.puzzles.copy(optimizeIce=v))},
   FeatureField.Flag("blazeLines",f.puzzles.blazeLines) {s,v->s.copy(puzzles=s.puzzles.copy(blazeLines=v))},
   FeatureField.Flag("beamLines",f.puzzles.beamLines) {s,v->s.copy(puzzles=s.puzzles.copy(beamLines=v))},
   FeatureField.Flag("depthTest",f.puzzles.depthTest) {s,v->s.copy(puzzles=s.puzzles.copy(depthTest=v))},
   FeatureField.Flag("iceDepthTest",f.puzzles.iceDepthTest) {s,v->s.copy(puzzles=s.puzzles.copy(iceDepthTest=v))},
   FeatureField.Flag("blazeDepthTest",f.puzzles.blazeDepthTest) {s,v->s.copy(puzzles=s.puzzles.copy(blazeDepthTest=v))},
   FeatureField.Flag("beamDepthTest",f.puzzles.beamDepthTest) {s,v->s.copy(puzzles=s.puzzles.copy(beamDepthTest=v))},
   FeatureField.Flag("announceMimic",f.puzzles.announceMimic) {s,v->s.copy(puzzles=s.puzzles.copy(announceMimic=v))},
   FeatureField.Flag("announcePrince",f.puzzles.announcePrince) {s,v->s.copy(puzzles=s.puzzles.copy(announcePrince=v))},
   FeatureField.Flag("announceBat",f.puzzles.announceBat) {s,v->s.copy(puzzles=s.puzzles.copy(announceBat=v))},
   FeatureField.Integer("mimicChestColor",f.puzzles.mimicChestColor) {s,v->s.copy(puzzles=s.puzzles.copy(mimicChestColor=v))},
  )
  "prices" -> listOf(
   FeatureField.Flag("network",f.prices.network) {s,v->s.copy(prices=s.prices.copy(network=v))},
   FeatureField.Flag("auction",f.prices.auction) {s,v->s.copy(prices=s.prices.copy(auction=v))},
   FeatureField.Flag("bazaar",f.prices.bazaar) {s,v->s.copy(prices=s.prices.copy(bazaar=v))},
   FeatureField.Flag("npc",f.prices.npc) {s,v->s.copy(prices=s.prices.copy(npc=v))},
   FeatureField.Flag("mote",f.prices.mote) {s,v->s.copy(prices=s.prices.copy(mote=v))},
   FeatureField.Integer("burgers",f.prices.burgers) {s,v->s.copy(prices=s.prices.copy(burgers=v))},
   FeatureField.Flag("pricePaid",f.prices.pricePaid) {s,v->s.copy(prices=s.prices.copy(pricePaid=v))},
   FeatureField.Flag("craftCost",f.prices.craftCost) {s,v->s.copy(prices=s.prices.copy(craftCost=v))},
  )
  "experiments" -> listOf(
   FeatureField.Flag("chronomatron",f.experiments.chronomatron) {s,v->s.copy(experiments=s.experiments.copy(chronomatron=v))},
   FeatureField.Flag("ultrasequencer",f.experiments.ultrasequencer) {s,v->s.copy(experiments=s.experiments.copy(ultrasequencer=v))},
   FeatureField.Flag("superpairs",f.experiments.superpairs) {s,v->s.copy(experiments=s.experiments.copy(superpairs=v))},
   FeatureField.Flag("blockWrongClicks",f.experiments.blockWrongClicks) {s,v->s.copy(experiments=s.experiments.copy(blockWrongClicks=v))},
  )
  "suppression" -> listOf(
   FeatureField.Flag("explosions",f.suppression.explosions) {s,v->s.copy(suppression=s.suppression.copy(explosions=v))},
   FeatureField.Flag("emptyTooltips",f.suppression.emptyTooltips) {s,v->s.copy(suppression=s.suppression.copy(emptyTooltips=v))},
   FeatureField.Flag("fireOverlay",f.suppression.fireOverlay) {s,v->s.copy(suppression=s.suppression.copy(fireOverlay=v))},
   FeatureField.Flag("breakParticles",f.suppression.breakParticles) {s,v->s.copy(suppression=s.suppression.copy(breakParticles=v))},
   FeatureField.Flag("bossBar",f.suppression.bossBar) {s,v->s.copy(suppression=s.suppression.copy(bossBar=v))},
   FeatureField.Flag("armorBar",f.suppression.armorBar) {s,v->s.copy(suppression=s.suppression.copy(armorBar=v))},
   FeatureField.Flag("foodBar",f.suppression.foodBar) {s,v->s.copy(suppression=s.suppression.copy(foodBar=v))},
   FeatureField.Integer("healthBar",f.suppression.healthBar) {s,v->s.copy(suppression=s.suppression.copy(healthBar=v))},
   FeatureField.Flag("fog",f.suppression.fog) {s,v->s.copy(suppression=s.suppression.copy(fog=v))},
   FeatureField.Flag("effectDisplay",f.suppression.effectDisplay) {s,v->s.copy(suppression=s.suppression.copy(effectDisplay=v))},
   FeatureField.Flag("recipeBook",f.suppression.recipeBook) {s,v->s.copy(suppression=s.suppression.copy(recipeBook=v))},
   FeatureField.Flag("selectedItemName",f.suppression.selectedItemName) {s,v->s.copy(suppression=s.suppression.copy(selectedItemName=v))},
   FeatureField.Flag("deadEntities",f.suppression.deadEntities) {s,v->s.copy(suppression=s.suppression.copy(deadEntities=v))},
   FeatureField.Flag("deadPoof",f.suppression.deadPoof) {s,v->s.copy(suppression=s.suppression.copy(deadPoof=v))},
   FeatureField.Flag("lightning",f.suppression.lightning) {s,v->s.copy(suppression=s.suppression.copy(lightning=v))},
   FeatureField.Flag("fallingBlocks",f.suppression.fallingBlocks) {s,v->s.copy(suppression=s.suppression.copy(fallingBlocks=v))},
   FeatureField.Flag("entityFire",f.suppression.entityFire) {s,v->s.copy(suppression=s.suppression.copy(entityFire=v))},
   FeatureField.Flag("mageBeam",f.suppression.mageBeam) {s,v->s.copy(suppression=s.suppression.copy(mageBeam=v))},
   FeatureField.Flag("iceSpray",f.suppression.iceSpray) {s,v->s.copy(suppression=s.suppression.copy(iceSpray=v))},
   FeatureField.Flag("powderCoating",f.suppression.powderCoating) {s,v->s.copy(suppression=s.suppression.copy(powderCoating=v))},
   FeatureField.Flag("soulweaverSkulls",f.suppression.soulweaverSkulls) {s,v->s.copy(suppression=s.suppression.copy(soulweaverSkulls=v))},
   FeatureField.Flag("guidedSheep",f.suppression.guidedSheep) {s,v->s.copy(suppression=s.suppression.copy(guidedSheep=v))},
   FeatureField.Flag("bonePlating",f.suppression.bonePlating) {s,v->s.copy(suppression=s.suppression.copy(bonePlating=v))},
   FeatureField.Flag("healerFairy",f.suppression.healerFairy) {s,v->s.copy(suppression=s.suppression.copy(healerFairy=v))},
   FeatureField.Flag("treeBits",f.suppression.treeBits) {s,v->s.copy(suppression=s.suppression.copy(treeBits=v))},
   FeatureField.Flag("cocoonAnimation",f.suppression.cocoonAnimation) {s,v->s.copy(suppression=s.suppression.copy(cocoonAnimation=v))},
   FeatureField.Flag("nausea",f.suppression.nausea) {s,v->s.copy(suppression=s.suppression.copy(nausea=v))},
   FeatureField.Integer("vignette",f.suppression.vignette) {s,v->s.copy(suppression=s.suppression.copy(vignette=v))},
   FeatureField.Flag("expOrbs",f.suppression.expOrbs) {s,v->s.copy(suppression=s.suppression.copy(expOrbs=v))},
   FeatureField.Flag("stuckArrows",f.suppression.stuckArrows) {s,v->s.copy(suppression=s.suppression.copy(stuckArrows=v))},
  )
  "worldVisuals.playerGlow" -> listOf(
   FeatureField.Integer("color",f.worldVisuals.playerGlow.color) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(playerGlow=s.worldVisuals.playerGlow.copy(color=v)))},
   FeatureField.Integer("range",f.worldVisuals.playerGlow.range) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(playerGlow=s.worldVisuals.playerGlow.copy(range=v)))},
   FeatureField.Flag("throughWalls",f.worldVisuals.playerGlow.throughWalls) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(playerGlow=s.worldVisuals.playerGlow.copy(throughWalls=v)))},
   FeatureField.Flag("realAccountsOnly",f.worldVisuals.playerGlow.realAccountsOnly) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(playerGlow=s.worldVisuals.playerGlow.copy(realAccountsOnly=v)))},
  )
  "worldVisuals.nametags" -> listOf(
   FeatureField.Integer("color",f.worldVisuals.nametags.color) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(color=v)))},
   FeatureField.Integer("backgroundColor",f.worldVisuals.nametags.backgroundColor) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(backgroundColor=v)))},
   FeatureField.Integer("range",f.worldVisuals.nametags.range) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(range=v)))},
   FeatureField.Decimal("scale",f.worldVisuals.nametags.scale) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(scale=v)))},
   FeatureField.Flag("throughWalls",f.worldVisuals.nametags.throughWalls) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(throughWalls=v)))},
   FeatureField.Flag("background",f.worldVisuals.nametags.background) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(background=v)))},
   FeatureField.Flag("distance",f.worldVisuals.nametags.distance) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(distance=v)))},
   FeatureField.Flag("health",f.worldVisuals.nametags.health) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(health=v)))},
   FeatureField.Flag("self",f.worldVisuals.nametags.self) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(self=v)))},
   FeatureField.Flag("realAccountsOnly",f.worldVisuals.nametags.realAccountsOnly) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(realAccountsOnly=v)))},
   FeatureField.Integer("borderColor",f.worldVisuals.nametags.borderColor) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(borderColor=v)))},
   FeatureField.Integer("distanceColor",f.worldVisuals.nametags.distanceColor) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(distanceColor=v)))},
   FeatureField.Decimal("opacity",f.worldVisuals.nametags.opacity) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(opacity=v)))},
   FeatureField.Flag("distanceScaling",f.worldVisuals.nametags.distanceScaling) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(distanceScaling=v)))},
   FeatureField.Flag("rounded",f.worldVisuals.nametags.rounded) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(nametags=s.worldVisuals.nametags.copy(rounded=v)))},
  )
  "worldVisuals.blockOutline" -> listOf(
   FeatureField.Integer("color",f.worldVisuals.blockOutline.color) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(color=v)))},
   FeatureField.Integer("fillColor",f.worldVisuals.blockOutline.fillColor) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(fillColor=v)))},
   FeatureField.Flag("filled",f.worldVisuals.blockOutline.filled) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(filled=v)))},
   FeatureField.Flag("throughWalls",f.worldVisuals.blockOutline.throughWalls) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(throughWalls=v)))},
   FeatureField.Decimal("width",f.worldVisuals.blockOutline.width) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(width=v)))},
   FeatureField.Flag("glow",f.worldVisuals.blockOutline.glow) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(glow=v)))},
   FeatureField.Decimal("glowRadius",f.worldVisuals.blockOutline.glowRadius) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(glowRadius=v)))},
   FeatureField.Decimal("glowStrength",f.worldVisuals.blockOutline.glowStrength) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(blockOutline=s.worldVisuals.blockOutline.copy(glowStrength=v)))},
  )
  "worldVisuals.titanium" -> listOf(
   FeatureField.Integer("color",f.worldVisuals.titanium.color) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(color=v)))},
   FeatureField.Integer("fillColor",f.worldVisuals.titanium.fillColor) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(fillColor=v)))},
   FeatureField.Integer("range",f.worldVisuals.titanium.range) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(range=v)))},
   FeatureField.Flag("all",f.worldVisuals.titanium.all) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(all=v)))},
   FeatureField.Flag("throughWalls",f.worldVisuals.titanium.throughWalls) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(throughWalls=v)))},
   FeatureField.Flag("filled",f.worldVisuals.titanium.filled) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(filled=v)))},
   FeatureField.Flag("distance",f.worldVisuals.titanium.distance) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(distance=v)))},
   FeatureField.Flag("tracers",f.worldVisuals.titanium.tracers) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(tracers=v)))},
   FeatureField.Integer("maxMarkers",f.worldVisuals.titanium.maxMarkers) {s,v->s.copy(worldVisuals=s.worldVisuals.copy(titanium=s.worldVisuals.titanium.copy(maxMarkers=v)))},
  )
  "inventoryHud" -> listOf(
   FeatureField.Flag("hud.enabled",f.inventoryHud.hud.enabled) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(hud=s.inventoryHud.hud.copy(enabled=v))) },
   FeatureField.Integer("hud.color",f.inventoryHud.hud.color) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(hud=s.inventoryHud.hud.copy(color=v))) },
   FeatureField.Flag("armor",f.inventoryHud.armor) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(armor=v))},
   FeatureField.Flag("hotbar",f.inventoryHud.hotbar) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(hotbar=v))},
   FeatureField.Flag("count",f.inventoryHud.count) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(count=v))},
   FeatureField.Flag("minimal",f.inventoryHud.minimal) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(minimal=v))},
   FeatureField.Flag("background",f.inventoryHud.background) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(background=v))},
   FeatureField.Integer("backgroundColor",f.inventoryHud.backgroundColor) {s,v->s.copy(inventoryHud=s.inventoryHud.copy(backgroundColor=v))},
  )
  "itemNameHud" -> listOf(
   FeatureField.Flag("hud.enabled",f.itemNameHud.hud.enabled) {s,v->s.copy(itemNameHud=s.itemNameHud.copy(hud=s.itemNameHud.hud.copy(enabled=v))) },
   FeatureField.Integer("hud.color",f.itemNameHud.hud.color) {s,v->s.copy(itemNameHud=s.itemNameHud.copy(hud=s.itemNameHud.hud.copy(color=v))) },
   FeatureField.Flag("background",f.itemNameHud.background) {s,v->s.copy(itemNameHud=s.itemNameHud.copy(background=v))},
   FeatureField.Integer("backgroundColor",f.itemNameHud.backgroundColor) {s,v->s.copy(itemNameHud=s.itemNameHud.copy(backgroundColor=v))},
   FeatureField.Flag("hideVanilla",f.itemNameHud.hideVanilla) {s,v->s.copy(itemNameHud=s.itemNameHud.copy(hideVanilla=v))},
  )
  "skillHud" -> listOf(
   FeatureField.Flag("hud.enabled",f.skillHud.hud.enabled) {s,v->s.copy(skillHud=s.skillHud.copy(hud=s.skillHud.hud.copy(enabled=v))) },
   FeatureField.Integer("hud.color",f.skillHud.hud.color) {s,v->s.copy(skillHud=s.skillHud.copy(hud=s.skillHud.hud.copy(color=v))) },
   FeatureField.Flag("icon",f.skillHud.icon) {s,v->s.copy(skillHud=s.skillHud.copy(icon=v))},
   FeatureField.Flag("showGain",f.skillHud.showGain) {s,v->s.copy(skillHud=s.skillHud.copy(showGain=v))},
   FeatureField.Flag("showLevel",f.skillHud.showLevel) {s,v->s.copy(skillHud=s.skillHud.copy(showLevel=v))},
   FeatureField.Flag("background",f.skillHud.background) {s,v->s.copy(skillHud=s.skillHud.copy(background=v))},
   FeatureField.Integer("backgroundColor",f.skillHud.backgroundColor) {s,v->s.copy(skillHud=s.skillHud.copy(backgroundColor=v))},
   FeatureField.Integer("timeoutSeconds",f.skillHud.timeoutSeconds) {s,v->s.copy(skillHud=s.skillHud.copy(timeoutSeconds=v))},
  )
  else -> error("Unknown settings section: $section")
 }
}
