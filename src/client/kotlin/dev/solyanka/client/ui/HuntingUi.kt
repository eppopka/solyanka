/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object HuntingUi {
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings) {
  val title=GuiCatalog.features.firstOrNull {it.id==id}?.name ?: "Archive"
  host.showPanel(SettingsPanel(title,{host.closePanel()},{model.error}) {panel,w->buildList {
   fun change(block:(FeatureSettings)->FeatureSettings) {model.updateGeneral {it.copy(features=block(it.features))};panel.refreshStatus()}
   fun toggle(name:String,value:Boolean,help:String="",update:(FeatureSettings,Boolean)->FeatureSettings) {add(GuiKit.toggleRow(w,name,value,help){v->change {update(it,v)}})}
   fun color(name:String,value:Int,update:(FeatureSettings,Int)->FeatureSettings) {add(GuiKit.colorRow(w,name,value){v->change {update(it,v)}})}
   fun number(name:String,value:Double,min:Double,max:Double,step:Double,update:(FeatureSettings,Double)->FeatureSettings) {add(GuiKit.sliderRow(w,name,value,NumberRange(min,max,step),""){v->change {update(it,v)}})}
   val f=model.general.features
   when(id) {
    S.BLOCKLIST->{val s=f.blocklist
     add(GuiKit.description(w,"Local UUID-backed Party Finder blocklist. /sl add <name> [reason], /sl remove <name/uuid>, /sl list [page]. /sl importlegacy imports config/shitters.txt without modifying it. New list: config/solyanka-shitters.json. Corrupt files are preserved and writes disabled. No public/global ban database."))
     add(GuiKit.description(w,"Default: local warning only. Identity must be verified from online player profiles or explicitly enabled Mojang lookups. Auto Kick sends /p kick; requires party permissions and may violate server rules. No retries. Toggle/world changes cancel pending actions."))
     toggle("Mojang Lookups",s.network,"Opt-in HTTPS profile lookup at api.mojang.com; only player names are sent."){it,v->it.copy(blocklist=it.blocklist.copy(network=v))};toggle("Auto Kick",s.autoKick){it,v->it.copy(blocklist=it.blocklist.copy(autoKick=v))};toggle("Party Warning",s.partyWarning){it,v->it.copy(blocklist=it.blocklist.copy(partyWarning=v))};number("Kick Delay (ms)",s.delayMs.toDouble(),100.0,3000.0,100.0){it,v->it.copy(blocklist=it.blocklist.copy(delayMs=v.toInt()))}
     dev.solyanka.client.feature.ShitterList.error?.let {add(GuiKit.description(w,it))}
    }
    S.MOVE_HOTBAR->{addAll(HotbarUi.controls(w,host,model,panel))}
    S.PIP->{val s=f.pip
     add(GuiKit.description(w,"Windows only. Captures ONLY a window you explicitly select below, locally; no upload, desktop capture, fuzzy title fallback or automatic selection. Selection clears on disable/world change. Tab/title changes keep the selected application window. Closing or minimizing clears its image; restore to resume, reselect after closing. Multiple windows of one program remain separate; no automatic process-restart retargeting. The current browser tab is captured, not a pinned tab. GPU/protected windows may return black or fail. PrintWindow can stall on an unresponsive application. Windows capture cannot be tested on this Linux build machine."))
     add(GuiKit.description(w,dev.solyanka.client.feature.PictureInPicture.status))
     number("Capture FPS",s.fps.toDouble(),5.0,60.0,1.0){it,v->it.copy(pip=it.pip.copy(fps=v.toInt()))};number("Width",s.width.toDouble(),100.0,640.0,10.0){it,v->it.copy(pip=it.pip.copy(width=v.toInt()))}
     add(GuiKit.button("Refresh window list",w){open(id,host,model)})
     add(GuiKit.button("Stop / clear selection",w){dev.solyanka.client.feature.PictureInPicture.choose(null);open(id,host,model)})
     dev.solyanka.client.feature.PictureInPicture.windows().take(64).forEach {window->
      val font=net.minecraft.client.Minecraft.getInstance().font;val full=window.label()
      val label=if(font.width(full)>w-16) font.plainSubstrByWidth(full,(w-28).coerceAtLeast(0))+"..." else full
      add(GuiKit.button(label,w){dev.solyanka.client.feature.PictureInPicture.choose(window);open(id,host,model)}.apply {tooltip(net.minecraft.network.chat.Component.literal(full))})
     }
     add(HudNavigation.button(w))
    }
    S.PET->{val s=f.pet
     add(GuiKit.description(w,"Open /pets once to observe the active pet. Total XP comes only from petInfo.exp; rates use changed server snapshots, never generic skill gains. Cache ambiguity stays unknown. Live level-progress curve and additional source polish are not yet ported. Sync is a manual server command."))
     add(GuiKit.button("Sync from /pets",w){dev.solyanka.client.feature.PetDisplay.sync()})
     add(GuiKit.choiceRow(w,"Style",listOf("Minimal","Glass","Neon"),s.style,readCurrent={model.general.features.pet.style}){v->change {it.copy(pet=it.pet.copy(style=v))}})
     color("Accent",s.accent){it,v->it.copy(pet=it.pet.copy(accent=v))};color("Background",s.background){it,v->it.copy(pet=it.pet.copy(background=v))};number("Rate Window (minutes)",s.sampleMinutes.toDouble(),1.0,30.0,1.0){it,v->it.copy(pet=it.pet.copy(sampleMinutes=v.toInt()))}
     toggle("Highlight Active Pet",s.highlight){it,v->it.copy(pet=it.pet.copy(highlight=v))};color("Pet Highlight",s.highlightColor){it,v->it.copy(pet=it.pet.copy(highlightColor=v))};toggle("Autopet Title",s.autoTitle){it,v->it.copy(pet=it.pet.copy(autoTitle=v))};toggle("Title Dungeon Only",s.titleDungeonOnly){it,v->it.copy(pet=it.pet.copy(titleDungeonOnly=v))}
     add(HudNavigation.button(w))
    }
    S.SIMON->{val s=f.simon
     add(GuiKit.description(w,"F7/M7 phase three; starts after Goldor's exact phase message. Tracks server lantern flashes before world replacement. Manual solver only: no Triggerbot/Auto Start yet. Sneak bypasses wrong-click prevention. Party alerts are opt-in."))
     toggle("SS Skip Compatibility",s.skip){it,v->it.copy(simon=it.simon.copy(skip=v))};toggle("Block Wrong Clicks",s.blockWrong){it,v->it.copy(simon=it.simon.copy(blockWrong=v))}
     color("First Color",s.first){it,v->it.copy(simon=it.simon.copy(first=v))};color("Second Color",s.second){it,v->it.copy(simon=it.simon.copy(second=v))};color("Other Color",s.other){it,v->it.copy(simon=it.simon.copy(other=v))}
     toggle("Outline",s.outline){it,v->it.copy(simon=it.simon.copy(outline=v))};toggle("Through Walls",s.throughWalls){it,v->it.copy(simon=it.simon.copy(throughWalls=v))};toggle("Button Numbers",s.numbers){it,v->it.copy(simon=it.simon.copy(numbers=v))};color("Number Color",s.numberColor){it,v->it.copy(simon=it.simon.copy(numberColor=v))}
     toggle("Progress Display",s.progress){it,v->it.copy(simon=it.simon.copy(progress=v))};toggle("Reset Alerts",s.alerts){it,v->it.copy(simon=it.simon.copy(alerts=v))};toggle("Party Alerts",s.partyAlerts,"Sends fixed SS restart/break messages to party chat. Off by default."){it,v->it.copy(simon=it.simon.copy(partyAlerts=v))};toggle("Alert Sound",s.sound){it,v->it.copy(simon=it.simon.copy(sound=v))}
     add(HudNavigation.button(w))
    }
    S.BESTIARY->{val s=f.bestiary
     add(GuiKit.description(w,"Comma-separated mob nameplate terms. Rechecks loaded stands and resolves their nearby mob. Uses native silhouette glow; no entity changes or server packets. Leave names empty to match nothing."))
     add(GuiKit.textRow(w,"Names",s.names){v->change {it.copy(bestiary=it.bestiary.copy(names=v))}})
     toggle("Exact Match",s.exact){it,v->it.copy(bestiary=it.bestiary.copy(exact=v))};color("Glow Color",s.color){it,v->it.copy(bestiary=it.bestiary.copy(color=v))}
     toggle("Nearest Tracer",s.tracer){it,v->it.copy(bestiary=it.bestiary.copy(tracer=v))};number("Tracer Range (0 = unlimited)",s.range.toDouble(),0.0,100.0,5.0){it,v->it.copy(bestiary=it.bestiary.copy(range=v.toInt()))}
    }
    S.INVISIBUG->{val s=f.invisibug
     add(GuiKit.description(w,"Galatea only. Requires the exact CRIT packet signature plus a single unnamed marker stand. Four nearby observations confirm a target; evidence expires after 20 ticks. No speculative highlights."))
     color("Highlight Color",s.color){it,v->it.copy(invisibug=it.invisibug.copy(color=v))};toggle("Nearest Tracer",s.tracer){it,v->it.copy(invisibug=it.invisibug.copy(tracer=v))};toggle("Distance",s.distance){it,v->it.copy(invisibug=it.invisibug.copy(distance=v))}
    }
    S.TIKI->{val s=f.tiki
     add(GuiKit.description(w,"Recognizes Sneaky/Cheeky/Shrieky three-skull texture stacks. Loaded chunks only, bounded scan within 192 blocks. Decorative patterns are local geometry. Auto Black Hole is not yet ported."))
     val styles=listOf("Outline","Filled","Both");add(GuiKit.choiceRow(w,"ESP Style",styles,s.style,readCurrent={model.general.features.tiki.style}){v->change {it.copy(tiki=it.tiki.copy(style=v))}})
     toggle("Tracer",s.tracer){it,v->it.copy(tiki=it.tiki.copy(tracer=v))};toggle("Distance",s.distance){it,v->it.copy(tiki=it.tiki.copy(distance=v))}
     color("Sneaky Color",s.sneaky){it,v->it.copy(tiki=it.tiki.copy(sneaky=v))};color("Cheeky Color",s.cheeky){it,v->it.copy(tiki=it.tiki.copy(cheeky=v))};color("Shrieky Color",s.shrieky){it,v->it.copy(tiki=it.tiki.copy(shrieky=v))}
     toggle("Particles",s.particles){it,v->it.copy(tiki=it.tiki.copy(particles=v))};add(GuiKit.choiceRow(w,"Particle Style",listOf("Rings","Double Helix","Sparkles"),s.particleStyle,readCurrent={model.general.features.tiki.particleStyle}){v->change {it.copy(tiki=it.tiki.copy(particleStyle=v))}})
     number("Particle Count",s.count.toDouble(),4.0,48.0,1.0){it,v->it.copy(tiki=it.tiki.copy(count=v.toInt()))};color("Particle Color",s.particleColor){it,v->it.copy(tiki=it.tiki.copy(particleColor=v))}
    }
    S.CHINA_HAT->{val s=f.chinaHat
     toggle("Hide in First Person",s.hideFirstPerson){it,v->it.copy(chinaHat=it.chinaHat.copy(hideFirstPerson=v))};color("Color",s.color){it,v->it.copy(chinaHat=it.chinaHat.copy(color=v))};toggle("Rainbow",s.rainbow){it,v->it.copy(chinaHat=it.chinaHat.copy(rainbow=v))}
     number("Radius",s.radius,.2,1.0,.01){it,v->it.copy(chinaHat=it.chinaHat.copy(radius=v))};number("Height",s.height,.1,1.0,.01){it,v->it.copy(chinaHat=it.chinaHat.copy(height=v))};number("Line Width",s.width,1.0,6.0,.1){it,v->it.copy(chinaHat=it.chinaHat.copy(width=v))}
     toggle("Filled",s.fill){it,v->it.copy(chinaHat=it.chinaHat.copy(fill=v))};toggle("Through Walls",s.throughWalls){it,v->it.copy(chinaHat=it.chinaHat.copy(throughWalls=v))}
    }
    S.HALO->{val s=f.halo
     toggle("Hide in First Person",s.hideFirstPerson){it,v->it.copy(halo=it.halo.copy(hideFirstPerson=v))};color("Color",s.color){it,v->it.copy(halo=it.halo.copy(color=v))};toggle("Rainbow",s.rainbow){it,v->it.copy(halo=it.halo.copy(rainbow=v))}
     number("Radius",s.radius,.1,1.0,.01){it,v->it.copy(halo=it.halo.copy(radius=v))};number("Height",s.height,0.0,1.5,.01){it,v->it.copy(halo=it.halo.copy(height=v))};number("Thickness",s.thickness,.01,.1,.005){it,v->it.copy(halo=it.halo.copy(thickness=v))};number("Glow",s.glow,0.0,1.0,.05){it,v->it.copy(halo=it.halo.copy(glow=v))};number("Opacity",s.opacity,.05,1.0,.05){it,v->it.copy(halo=it.halo.copy(opacity=v))}
     toggle("Shimmer",s.shimmer){it,v->it.copy(halo=it.halo.copy(shimmer=v))};toggle("Bobbing",s.bob){it,v->it.copy(halo=it.halo.copy(bob=v))};toggle("Filled Disc",s.fill){it,v->it.copy(halo=it.halo.copy(fill=v))};number("Disc Opacity",s.fillOpacity,0.0,1.0,.05){it,v->it.copy(halo=it.halo.copy(fillOpacity=v))};toggle("Through Walls",s.throughWalls){it,v->it.copy(halo=it.halo.copy(throughWalls=v))}
    }
   }
  }})
 }
}
