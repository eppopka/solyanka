/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.cs2.Cs2Knives
import dev.solyanka.client.cs2.Cs2AssetDownload
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
object FinalCosmeticsUi {
 private var confirmAssets=false
 fun open(id:String,host:NoFrillsClickGui,model:InstantSettings,advanced:Boolean=false) {
  if(id==S.CS2_KNIFE && !advanced){host.showPanel(KnifeBrowserPanel(host,model));return}
  host.showPanel(SettingsPanel(if(id==S.ITEM_AURA)"Item Aura" else "CS2 Knives",{confirmAssets=false;host.closePanel()},{model.error},info={if(id==S.CS2_KNIFE) Cs2AssetDownload.status() else null}) {panel,w->buildList {
   fun change(block:(FeatureSettings)->FeatureSettings) {model.updateGeneral {it.copy(features=block(it.features))};panel.refreshStatus()}
   if(id==S.ITEM_AURA) {
    val s=model.general.features.itemAura
    fun update(block:(ItemAuraSettings)->ItemAuraSettings) {change {it.copy(itemAura=block(it.itemAura))}}
    fun number(name:String,value:Double,min:Double,max:Double,step:Double,block:(ItemAuraSettings,Double)->ItemAuraSettings) {add(GuiKit.sliderRow(w,name,value,NumberRange(min,max,step)){v->update {block(it,v)}})}
    fun toggle(name:String,value:Boolean,block:(ItemAuraSettings,Boolean)->ItemAuraSettings) {add(GuiKit.toggleRow(w,name,value){v->update {block(it,v)}})}
    add(GuiKit.description(w,"Item-attached first-person VFX from impl.zip. Captured after the arm transforms and drawn after the hand batch. No world particles. High count/trails/glow cost more CPU/GPU; geometry is bounded. Works with a loaded CS2 knife too."))
    add(GuiKit.choiceRow(w,"Style",listOf("Orbit Rings","DNA Helix","Comet Swarm","Vortex","Fireflies"),s.style-1,readCurrent={model.general.features.itemAura.style-1}){v->update {it.copy(style=v+1)}})
    add(GuiKit.choiceRow(w,"Sprite",listOf("Soft Orb","Sparkle Star","Orb + Star"),s.shape-1,readCurrent={model.general.features.itemAura.shape-1}){v->update {it.copy(shape=v+1)}})
    number("Particles",s.count.toDouble(),4.0,64.0,1.0){a,v->a.copy(count=v.toInt())};number("Radius",s.radius,.05,1.5,.01){a,v->a.copy(radius=v)}
    number("Particle Size",s.size,.004,.2,.002){a,v->a.copy(size=v)};number("Speed",s.speed,.1,4.0,.1){a,v->a.copy(speed=v)}
    number("Trail Length",s.trail.toDouble(),0.0,16.0,1.0){a,v->a.copy(trail=v.toInt())};number("Smoothness",s.quality.toDouble(),5.0,24.0,1.0){a,v->a.copy(quality=v.toInt())}
    toggle("Ribbon Lines",s.ribbon){a,v->a.copy(ribbon=v)};toggle("Soft Glow",s.glow){a,v->a.copy(glow=v)};toggle("Depth Sorting",s.depth){a,v->a.copy(depth=v)}
    toggle("Rainbow",s.rainbow){a,v->a.copy(rainbow=v)};number("Hue Spread",s.hueSpread,0.0,1.0,.05){a,v->a.copy(hueSpread=v)}
    add(GuiKit.colorRow(w,"Aura Color",s.auraColor){v->update {it.copy(auraColor=v)}})
    number("Offset X",s.offsetX,-.6,.6,.01){a,v->a.copy(offsetX=v)};number("Offset Y",s.offsetY,-.6,.6,.01){a,v->a.copy(offsetY=v)};number("Offset Z",s.offsetZ,-.6,.6,.01){a,v->a.copy(offsetZ=v)}
    number("Rotate X",s.rotX,-180.0,180.0,1.0){a,v->a.copy(rotX=v)};number("Rotate Y",s.rotY,-180.0,180.0,1.0){a,v->a.copy(rotY=v)};number("Rotate Z",s.rotZ,-180.0,180.0,1.0){a,v->a.copy(rotZ=v)}
    number("Stretch",s.stretch,.2,3.0,.05){a,v->a.copy(stretch=v)};number("Auto Spin",s.spin,-180.0,180.0,5.0){a,v->a.copy(spin=v)}
    toggle("Off Hand Too",s.offHand){a,v->a.copy(offHand=v)};toggle("Only With Item",s.onlyWithItem){a,v->a.copy(onlyWithItem=v)}
   } else {
    add(GuiKit.button("Open Knife Browser",w){open(id,host,model,false)})
    val s=model.general.features.cs2Knife
    fun update(block:(Cs2KnifeSettings)->Cs2KnifeSettings) {change {it.copy(cs2Knife=block(it.cs2Knife))}}
    add(GuiKit.description(w,"Stray's Source 2 meshes, skins, wear/seed and draw/idle/inspect/swing animations. Use a local CS2 install or explicitly download Stray's published knife pack. No assets are bundled. Downloads occur only after confirmation; enabling this module never starts a download. Cached assets work offline."))
    add(GuiKit.description(w,Cs2Knives.status()))
    add(GuiKit.description(w,Cs2AssetDownload.status()))
    add(GuiKit.choiceRow(w,"Asset source",listOf("Auto (local, then cached pack)","Local CS2 install","Downloaded Stray pack"),listOf("auto","install","pack").indexOf(s.source),readCurrent={listOf("auto","install","pack").indexOf(model.general.features.cs2Knife.source)}) {i->update {it.copy(source=listOf("auto","install","pack")[i])}})
    if(Cs2AssetDownload.running()) add(GuiKit.button("Cancel asset download",w){Cs2AssetDownload.cancel();open(id,host,model,true)})
    else if(!confirmAssets) add(GuiKit.button("Download knife assets...",w){confirmAssets=true;open(id,host,model,true)})
    else {
     add(GuiKit.description(w,"Download 134.6 MB from raw.githubusercontent.com/camberX/Stray (third-party host)? Your IP is shared with that host. Saved locally under config/solyanka/cs2.pack, checked against a pinned SHA-256. Game assets remain subject to their owners' terms. No account credentials are sent. Use only if permitted. The pack is not part of solyanka's GPL license."))
     add(GuiKit.button("Confirm download (134.6 MB)",w){confirmAssets=false;update {it.copy(source="pack")};Cs2AssetDownload.start();open(id,host,model,true)})
     add(GuiKit.button("Cancel",w){confirmAssets=false;open(id,host,model,true)})
    }
    add(GuiKit.textRow(w,"Local CS2 folder",s.path,"Path to game/csgo or CS2 installation root; blank uses local Steam detection."){v->update {it.copy(path=v)}})
    add(GuiKit.button("Reload local install",w){Cs2Knives.reload();open(id,host,model,true)})
    add(GuiKit.button("Refresh list / status",w){open(id,host,model,true)})
    val knives=Cs2Knives.knives()
    if(knives.isNotEmpty()) {
     val index=knives.indexOfFirst {it.id()==s.knife}.coerceAtLeast(0)
     add(GuiKit.choiceRow(w,"Knife",knives.map {it.name()},index,readCurrent={knives.indexOfFirst {it.id()==model.general.features.cs2Knife.knife}.coerceAtLeast(0)}){i->update {it.copy(knife=knives[i].id(),finish="")};open(id,host,model,true)})
     val selected=knives.firstOrNull {it.id()==s.knife}
     val finishes=listOf(dev.solyanka.client.cs2.Cs2Library.Finish("","Vanilla"))+(selected?.finishes() ?: emptyList())
     add(GuiKit.choiceRow(w,"Finish",finishes.map {it.name()},finishes.indexOfFirst {it.kit()==s.finish}.coerceAtLeast(0),readCurrent={finishes.indexOfFirst {it.kit()==model.general.features.cs2Knife.finish}.coerceAtLeast(0)}){i->update {it.copy(finish=finishes[i].kit())}})
    }
    add(GuiKit.choiceRow(w,"Apply To",listOf("Swords / Axes","Any Held Item","Empty Main Hand"),listOf("weapons","held","empty").indexOf(s.applyTo),readCurrent={listOf("weapons","held","empty").indexOf(model.general.features.cs2Knife.applyTo)}){i->update {it.copy(applyTo=listOf("weapons","held","empty")[i])}})
    add(GuiKit.sliderRow(w,"Seed",s.seed.toDouble(),NumberRange(0.0,1000.0,1.0)){v->update {it.copy(seed=v.toInt())}})
    add(GuiKit.sliderRow(w,"Wear",s.wear,NumberRange(0.0,1.0,.001)){v->update {it.copy(wear=v)}})
    val inspectKeys=listOf(70,71,82,86,-1)
    add(GuiKit.choiceRow(w,"Inspect Key",listOf("F","G","R","V","Disabled"),inspectKeys.indexOf(s.inspectKey).coerceAtLeast(0),readCurrent={inspectKeys.indexOf(model.general.features.cs2Knife.inspectKey).coerceAtLeast(0)}){i->update {it.copy(inspectKey=inspectKeys[i])}})
    add(GuiKit.toggleRow(w,"Animations",s.animations){v->update {it.copy(animations=v)}})
    add(GuiKit.toggleRow(w,"Third Person",s.thirdPerson){v->update {it.copy(thirdPerson=v)}})
    for((label,value,key) in listOf(Triple("View Offset X",s.offsetX,"x"),Triple("View Offset Y",s.offsetY,"y"),Triple("View Distance",s.distance,"z"))) add(GuiKit.sliderRow(w,label,value,NumberRange(-2.0,2.0,.01)){v->update {when(key){"x"->it.copy(offsetX=v);"y"->it.copy(offsetY=v);else->it.copy(distance=v)}}})
    add(GuiKit.description(w,"Inspect: F by default. Local command: /solyanka knifestatus. Per-sword skin profiles, custom grip editor and blade-only Held Item shader are not included in this port."))
   }
  }})
 }
}
