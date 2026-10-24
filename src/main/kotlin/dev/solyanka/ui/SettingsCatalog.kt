/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

/** Only fields belonging to the selected feature are exposed in its settings panel. */
data class FeaturePanelSpec(val section:String,val title:String,val fields:List<String>)
object SettingsCatalog {
    private fun panel(section:String,title:String,vararg fields:String)=FeaturePanelSpec(section,title,fields.toList())
    val panels=linkedMapOf(
        "render.ambience" to panel("environment","Ambience","clock","weather","precipitation","gradient"),
        "render.aspect" to panel("environment","Aspect","aspectRatio"),
        "render.fog" to panel("environment","Fog","fogStart","fogEnd","fogDensity","fogColor"),
        "dungeon.door_highlight" to panel("map","Door Highlight","announceKeySpawn"),
        "dungeon.map_info" to panel("map","Map Info","x","y","disableInBoss","alert300","highlightPortal","paul","remaining","roomSecrets"),
        "solver.ice_fill" to panel("puzzles","Ice Fill","optimizeIce","iceDepthTest"),
        "solver.blaze" to panel("puzzles","Blaze","blazeLines","blazeDepthTest"),
        "solver.creeper_beams" to panel("puzzles","Creeper Beams","beamLines","beamDepthTest"),
        "dungeon.mimic" to panel("puzzles","Mimic Detection","announceMimic","announcePrince","announceBat"),
        "custom.mimic_chest" to panel("puzzles","Mimic Chest","mimicChestColor"),
        "utility.price_tooltips" to panel("prices","Price Tooltips","network","auction","bazaar","npc","mote","burgers","pricePaid","craftCost"),
        "solver.experiments" to panel("experiments","Experiment Solver","chronomatron","ultrasequencer","superpairs","blockWrongClicks"),
        "render.suppression" to panel("suppression","No Render","explosions","emptyTooltips","fireOverlay","breakParticles","bossBar","armorBar","foodBar","healthBar","fog","effectDisplay","recipeBook","selectedItemName","deadEntities","deadPoof","lightning","fallingBlocks","entityFire","mageBeam","iceSpray","powderCoating","soulweaverSkulls","guidedSheep","bonePlating","healerFairy","treeBits","cocoonAnimation","nausea","vignette","expOrbs","stuckArrows")
    )
    val waypointTypes=listOf("None","Normal","Secret","Etherwarp")
    val choices=mapOf(
        "clock" to listOf("Unchanged","Dawn","Day","Noon","Dusk","Night","Midnight"),
        "weather" to listOf("Unchanged","Sunny","Rain","Thunder","Snow"),
        "healthBar" to listOf("Nowhere","Outside Rift","Everywhere"),
        "vignette" to listOf("None","Ambient","Danger","Both"),
        "burgers" to (0..5).map { "$it burgers" }
    )
    val colors=setOf("fogColor","mimicChestColor")
    fun nextChoice(current:Int,size:Int):Int { require(size>0);return Math.floorMod(current+1,size) }
}
