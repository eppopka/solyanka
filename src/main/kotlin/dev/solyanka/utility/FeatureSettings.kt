/* SPDX-License-Identifier: GPL-3.0-only
 * Odin-derived HUD settings retain BSD-3-Clause notice in licenses/Odin-BSD-3-Clause.txt.
 * NoFrills-derived utility settings: GPLv3; see NOTICE. */
package dev.solyanka.utility

import com.google.gson.*

data class HudSpec(val enabled:Boolean=true,val x:Int=12,val y:Int=30,val scale:Double=1.0,val color:Int=-1) {
    fun validate() { require(x in 0..10000 && y in 0..10000) { "HUD position must be 0–10000" };require(scale.isFinite() && scale in 0.5..4.0) { "HUD scale must be 0.5–4" } }
}
data class TimerSettings(val ticks:Boolean=false,val symbols:Boolean=true,val prefix:Boolean=true,val startTimer:Boolean=false,
    val goldor:HudSpec=HudSpec(y=30),val pad:HudSpec=HudSpec(y=44),val lightning:HudSpec=HudSpec(y=58),
    val py:HudSpec=HudSpec(y=72),val storm:HudSpec=HudSpec(y=86),val freeze:HudSpec=HudSpec(y=100,color=0xffaa0000.toInt())) {
    fun huds()=linkedMapOf("goldor" to goldor,"pad" to pad,"lightning" to lightning,"py" to py,"storm" to storm,"freeze" to freeze)
    fun withHud(id:String,h:HudSpec)=when(id) { "goldor"->copy(goldor=h);"pad"->copy(pad=h);"lightning"->copy(lightning=h);"py"->copy(py=h);"storm"->copy(storm=h);"freeze"->copy(freeze=h);else->error("Unknown timer HUD") }
}
data class PlayerDisplaySettings(val hideArmor:Boolean=false,val hideFood:Boolean=false,val hideHearts:Boolean=false,val hideXp:Boolean=false,val hideXpBar:Boolean=false,
    val hideHealth:Boolean=true,val hideMana:Boolean=true,val hideOverflow:Boolean=true,val hideDefense:Boolean=true,val hideVitality:Boolean=true,
    val separateOverflow:Boolean=true,val hideZeroOverflow:Boolean=true,val icons:Boolean=true,
    val health:HudSpec=HudSpec(x=12,y=130,color=0xffff5555.toInt()),val mana:HudSpec=HudSpec(x=12,y=144,color=0xff55ffff.toInt()),
    val overflow:HudSpec=HudSpec(x=12,y=158,color=0xff00aaaa.toInt()),val defense:HudSpec=HudSpec(x=12,y=172,color=0xff55ff55.toInt()),
    val vitality:HudSpec=HudSpec(x=12,y=186,color=0xffaa0000.toInt()),val ehp:HudSpec=HudSpec(x=12,y=200,color=0xff00aa00.toInt()),val speed:HudSpec=HudSpec(x=12,y=214)) {
    fun huds()=linkedMapOf("health" to health,"mana" to mana,"overflow" to overflow,"defense" to defense,"vitality" to vitality,"ehp" to ehp,"speed" to speed)
    fun withHud(id:String,h:HudSpec)=when(id) { "health"->copy(health=h);"mana"->copy(mana=h);"overflow"->copy(overflow=h);"defense"->copy(defense=h);"vitality"->copy(vitality=h);"ehp"->copy(ehp=h);"speed"->copy(speed=h);else->error("Unknown player HUD") }
}
data class UtilityFixSettings(val animationSkyblockOnly:Boolean=false,val middleDebug:Boolean=false,val cursorClearTicks:Int=200,val loadingServerOnly:Boolean=true)
data class PriceSettings(val network:Boolean=false,val auction:Boolean=true,val bazaar:Boolean=true,val npc:Boolean=true,val mote:Boolean=false,val burgers:Int=0,val pricePaid:Boolean=true,val craftCost:Boolean=true)
data class ExperimentSettings(val chronomatron:Boolean=true,val ultrasequencer:Boolean=true,val superpairs:Boolean=true,val blockWrongClicks:Boolean=true)
data class SuppressionSettings(val explosions:Boolean=false,val emptyTooltips:Boolean=false,val fireOverlay:Boolean=false,val breakParticles:Boolean=false,
 val bossBar:Boolean=false,val armorBar:Boolean=false,val foodBar:Boolean=false,val healthBar:Int=0,val fog:Boolean=false,val effectDisplay:Boolean=false,
 val recipeBook:Boolean=false,val selectedItemName:Boolean=false,val deadEntities:Boolean=false,val deadPoof:Boolean=false,val lightning:Boolean=false,
 val fallingBlocks:Boolean=false,val entityFire:Boolean=false,val mageBeam:Boolean=false,val iceSpray:Boolean=false,val powderCoating:Boolean=false,
 val soulweaverSkulls:Boolean=false,val guidedSheep:Boolean=false,val bonePlating:Boolean=false,val healerFairy:Boolean=false,val treeBits:Boolean=false,
 val cocoonAnimation:Boolean=false,val nausea:Boolean=false,val vignette:Int=0,val expOrbs:Boolean=false,val stuckArrows:Boolean=false)
data class FeatureSettings(val timers:TimerSettings=TimerSettings(),val player:PlayerDisplaySettings=PlayerDisplaySettings(),val fixes:UtilityFixSettings=UtilityFixSettings(),val puzzles:PuzzleSettings=PuzzleSettings(),val map:MapSettings=MapSettings(),val environment:EnvironmentSettings=EnvironmentSettings(),val waypoints:WaypointSettings=WaypointSettings(),val prices:PriceSettings=PriceSettings(),val experiments:ExperimentSettings=ExperimentSettings(),val suppression:SuppressionSettings=SuppressionSettings(),val inventoryHud:InventoryHudSettings=InventoryHudSettings(),val itemNameHud:ItemNameHudSettings=ItemNameHudSettings(),val skillHud:SkillHudSettings=SkillHudSettings(),val worldVisuals:WorldVisualSettings=WorldVisualSettings(),val scoreboardHud:ScoreboardHudSettings=ScoreboardHudSettings(),val themedChat:ThemedChatSettings=ThemedChatSettings(),val heldItem:HeldItemSettings=HeldItemSettings(),val skybox:SkyboxSettings=SkyboxSettings(),val motionBlur:MotionBlurSettings=MotionBlurSettings(),val stopwatch:StopwatchSettings=StopwatchSettings(),val dvd:DvdSettings=DvdSettings(),val ambientParticles:AmbientParticleSettings=AmbientParticleSettings(),val rotatingItem:RotatingItemSettings=RotatingItemSettings(),val moveHearts:MoveHeartsSettings=MoveHeartsSettings(),val secretCounter:SecretCounterSettings=SecretCounterSettings(),val blocklist:BlocklistSettings=BlocklistSettings(),val moveHotbar:MoveHotbarSettings=MoveHotbarSettings(),val pip:PipSettings=PipSettings(),val pet:PetSettings=PetSettings(),val route:RouteSettings=RouteSettings(),val simon:SimonSettings=SimonSettings(),val bestiary:BestiarySettings=BestiarySettings(),val invisibug:InvisibugSettings=InvisibugSettings(),val tiki:TikiSettings=TikiSettings(),val chinaHat:ChinaHatSettings=ChinaHatSettings(),val halo:HaloSettings=HaloSettings(),val itemAura:ItemAuraSettings=ItemAuraSettings(),val cs2Knife:Cs2KnifeSettings=Cs2KnifeSettings()) {
    fun validate() { itemAura.validate();cs2Knife.validate();blocklist.validate();moveHotbar.validate();pip.validate();pet.validate();route.validate();simon.validate();bestiary.validate();tiki.validate();chinaHat.validate();halo.validate();secretCounter.validate();ambientParticles.validate();rotatingItem.validate();moveHearts.validate();motionBlur.validate();stopwatch.validate();dvd.validate();skybox.validate();heldItem.validate();scoreboardHud.validate();themedChat.validate();worldVisuals.validate();inventoryHud.hud.validate();itemNameHud.hud.validate();skillHud.hud.validate();require(skillHud.timeoutSeconds in 0..120);map.validate();environment.validate();waypoints.validate();require(prices.burgers in 0..5);require(suppression.healthBar in 0..2);require(suppression.vignette in 0..3);timers.huds().values.forEach { it.validate() };player.huds().values.forEach { it.validate() };require(fixes.cursorClearTicks in 0..1200) { "Cursor expiry must be 0–1200 ticks (0 = retain until reset)" } }
}
object FeatureCodec {
    private val gson=Gson()
    fun decode(input:JsonObject?):FeatureSettings {
        val defaults=gson.toJsonTree(FeatureSettings()).asJsonObject
        fun overlay(base:JsonObject,value:JsonObject) {
            base.entrySet().toList().forEach { (key,default) ->
                val v=value.get(key) ?: return@forEach
                if(default.isJsonObject) { require(v.isJsonObject) { "$key must be an object" };overlay(default.asJsonObject,v.asJsonObject) }
                else {
                    require(v.isJsonPrimitive) { "$key has invalid type" }
                    val kind=default.asJsonPrimitive;val actual=v.asJsonPrimitive
                    if(kind.isBoolean) require(actual.isBoolean) { "$key must be boolean" }
                    else if(kind.isString) require(actual.isString) { "$key must be text" }
                    else { require(actual.isNumber && actual.asDouble.isFinite()) { "$key must be numeric" }
                        if(kind.asNumber !is Double && kind.asNumber !is Float) actual.asBigDecimal.intValueExact()
                    }
                    base.add(key,v)
                }
            }
        }
        if(input!=null) {
            overlay(defaults,input)
            val legacy=input.getAsJsonObject("puzzles")
            if(legacy?.has("depthTest")==true) {
                listOf("iceDepthTest","blazeDepthTest","beamDepthTest").forEach { field ->
                    if(!legacy.has(field)) defaults.getAsJsonObject("puzzles").add(field,legacy.get("depthTest"))
                }
            }
        }
        return gson.fromJson(defaults,FeatureSettings::class.java).also { it.validate() }
    }
}
