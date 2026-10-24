/* SPDX-License-Identifier: GPL-3.0-only
 * Stray Cs2KnifeScreen CC0 layout/preview concepts; native embedded solyanka adaptation. */
package dev.solyanka.client.ui
import dev.solyanka.client.SolyankaClient as S
import dev.solyanka.client.cs2.*
import dev.solyanka.client.feature.ChromeDraw
import dev.solyanka.client.ui.nofrills.*
import dev.solyanka.ui.*
import dev.solyanka.utility.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.input.*
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.*
/** A three-column photo browser over the feature list. No new Screen, world dimming or auto-download. */
class KnifeBrowserPanel(private val host:NoFrillsClickGui,private val model:InstantSettings):EmbeddedPanel {
 private val font get()=Minecraft.getInstance().font
 private val drag=PanelDrag();private var placement:PanelDrag.Point?=null
 private var screenW=0;private var screenH=0;private var scale=1f
 private val W=548;private val H=332
 private var knifeSearch:EditBox?=null;private var finishSearch:EditBox?=null;private var seedBox:EditBox?=null
 private var tab=0;private var knifeScroll=0;private var finishScroll=0;private var bindingScroll=0
 private var editingUuid:String?=null;private var notice:String?=null
 private val previews=KnifePreviewCache();private var previewChanged=0L;private var previewKey=""
 private data class Box(val x:Int,val y:Int,val w:Int,val h:Int){fun has(px:Double,py:Double)=px>=x && py>=y && px<x+w && py<y+h}
 private data class Hit(val box:Box,val action:()->Unit)
 private val hits=mutableListOf<Hit>();private var sliding:((Double)->Unit)?=null
 private val ink=0xfff2f4f7.toInt();private val muted=0xffa4a8ae.toInt();private val accent=0xff9bcaff.toInt()
 private fun settings()=model.general.features.cs2Knife
 private fun binding()=settings().bindings.firstOrNull {it.uuid==editingUuid}
 private fun selection()=binding()?.choice() ?: settings().let {KnifeChoice(it.knife,it.finish,it.seed,it.wear)}
 private fun update(change:(Cs2KnifeSettings)->Cs2KnifeSettings){model.updateGeneral {it.copy(features=it.features.copy(cs2Knife=change(it.features.cs2Knife)))}}
 private fun edit(change:(KnifeChoice)->KnifeChoice) {
  val next=change(selection());val id=editingUuid
  update {s->if(id==null)s.copy(knife=next.knife,finish=next.finish,seed=next.seed,wear=next.wear)else s.copy(bindings=s.bindings.map {if(it.uuid==id)it.copy(knife=next.knife,finish=next.finish,seed=next.seed,wear=next.wear)else it})}
  previewChanged=System.nanoTime()
 }
 private fun held()=Minecraft.getInstance().player?.mainHandItem
 private fun heldUuid()=Cs2ItemIdentity.uuid(held())
 override fun position()=drag.position
 override fun placeAt(point:PanelDrag.Point?){placement=point}
 override fun mount(width:Int,height:Int) {
  screenW=width;screenH=height;scale=minOf(1f,(width-16).coerceAtLeast(1)/W.toFloat(),(height-16).coerceAtLeast(1)/H.toFloat()).coerceAtLeast(.01f)
  drag.layout(width,height,ceil(W*scale).toInt(),ceil(H*scale).toInt());placement?.let {drag.place(it)};placement=null
  fun box(x:Int,y:Int,w:Int,hint:String,value:String,change:(String)->Unit)=EditBox(font,x,y,w,12,Component.literal(hint)).apply {
   setBordered(false);setMaxLength(64);setHint(Component.literal(hint).withColor(0xa4a8ae));setTextColor(ink);setValue(value);setResponder(change)
  }
  val k=knifeSearch?.value ?: "";val f=finishSearch?.value ?: ""
  knifeSearch=box(18,51,110,"Search knives",k){knifeScroll=0}
  finishSearch=box(150,73,164,"Search finishes",f){finishScroll=0}
  seedBox=box(366,203,66,"Seed",selection().seed.toString()){v->v.toIntOrNull()?.takeIf {it in 0..1000}?.let {n->edit {it.copy(seed=n)}}}.apply {setMaxLength(4)}
 }
 private fun xy(x:Double,y:Double):Pair<Double,Double>{val p=drag.position ?: PanelDrag.Point(0,0);return (x-p.x)/scale to (y-p.y)/scale}
 private fun event(e:MouseButtonEvent):MouseButtonEvent {val p=xy(e.x(),e.y());return MouseButtonEvent(p.first,p.second,e.buttonInfo())}
 private fun fields()=listOfNotNull(knifeSearch,finishSearch,seedBox).filter {it.visible}
 override fun focused():GuiEventListener?=fields().firstOrNull {it.isFocused}
 fun focusSearch(){fields().forEach {it.isFocused=false};knifeSearch?.isFocused=true}
 private fun text(g:GuiGraphicsExtractor,value:String,x:Int,y:Int,color:Int=ink,max:Int=1000){g.text(font,font.plainSubstrByWidth(value,max),x,y,color,false)}
 private fun surface(g:GuiGraphicsExtractor,b:Box,fill:Int=0xd92b2d30.toInt(),border:Int=0x305d6269){ChromeDraw.panel(g,b.x,b.y,b.w,b.h,fill,border,6)}
 private fun button(g:GuiGraphicsExtractor,label:String,b:Box,active:Boolean=false,action:()->Unit){surface(g,b,if(active)0xffdfe8f2.toInt()else 0xff303237.toInt(),if(active)0xffeef4fa.toInt()else 0x405d6269);text(g,label,b.x+5,b.y+(b.h-8)/2,if(active)0xff111820.toInt()else ink,b.w-10);hits.add(Hit(b,action))}
 private fun slider(g:GuiGraphicsExtractor,label:String,b:Box,value:Double,min:Double,max:Double,step:Double,change:(Double)->Unit) {
  text(g,label,b.x,b.y,muted,b.w-44);text(g,"%.3f".format(java.util.Locale.ROOT,value),b.x+b.w-39,b.y,ink,39)
  val y=b.y+13;g.fill(b.x,y,b.x+b.w,y+3,0xff555b64.toInt());val pos=((value-min)/(max-min)).coerceIn(0.0,1.0)
  g.fill(b.x,y,b.x+(b.w*pos).roundToInt(),y+3,accent);ChromeDraw.panel(g,b.x+(b.w*pos).roundToInt()-3,y-2,7,7,ink,ink,3)
  val apply:(Double)->Unit={x->change(NumberRange(min,max,step).snap(min+(x-b.x)/b.w*(max-min)))}
  hits.add(Hit(Box(b.x,b.y,b.w,23)){sliding=apply;apply(lastX)})
 }
 private var lastX=0.0
 private fun photo(g:GuiGraphicsExtractor,image:KnifePreviewCache.Image?,b:Box) {
  if(image==null){text(g,"Loading / unavailable",b.x+5,b.y+b.h/2,muted,b.w-10);return}
  val f=minOf(b.w.toFloat()/image.width,b.h.toFloat()/image.height);val w=(image.width*f).roundToInt();val h=(image.height*f).roundToInt()
  g.blit(RenderPipelines.GUI_TEXTURED,image.id,b.x+(b.w-w)/2,b.y+(b.h-h)/2,0f,0f,w,h,w,h)
 }
 override fun render(g:GuiGraphicsExtractor,mx:Int,my:Int,delta:Float) {
  previews.poll();hits.clear();val mouse=xy(mx.toDouble(),my.toDouble());lastX=mouse.first
  val point=drag.position ?: PanelDrag.Point(0,0)
  g.pose().pushMatrix()
  try {
   g.pose().translate(point.x.toFloat(),point.y.toFloat());g.pose().scale(scale,scale)
   surface(g,Box(0,0,W,H),0xf0181b20.toInt(),0xff41464e.toInt())
   val choice=selection();val names=Cs2Knives.knives();val current=names.firstOrNull {it.id()==choice.knife}
   text(g,"CS2 Knife",12,10);text(g,if(editingUuid==null)"Global selection"else "Item: ${binding()?.label ?: "removed"}",12,25,muted,325)
   button(g,if(S.isEnabled(S.CS2_KNIFE))"Enabled"else "Enable",Box(148,8,65,18),S.isEnabled(S.CS2_KNIFE)) {val states=S.states().toMutableMap();states[S.CS2_KNIFE]=!S.isEnabled(S.CS2_KNIFE);notice=S.applySettings(S.currentProfile(),states)}
   button(g,"Inspect",Box(222,8,59,18)){Cs2Viewmodel.inspect()}
   button(g,"Global",Box(347,8,56,18),editingUuid==null){editingUuid=null;seedBox?.value=selection().seed.toString();previewChanged=System.nanoTime()}
   button(g,"Assets / More",Box(410,8,104,18)){FinalCosmeticsUi.open(S.CS2_KNIFE,host,model,true)}
   button(g,"X",Box(520,8,18,18)){host.closePanel()}
   g.fill(12,38,W-12,39,0xff50565f.toInt())
   surface(g,Box(12,46,122,18));knifeSearch?.visible=true
   val filtered=names.filter {it.name().contains(knifeSearch?.value ?: "",true)}
   val rows=14;knifeScroll=knifeScroll.coerceIn(0,(filtered.size-rows).coerceAtLeast(0))
   filtered.drop(knifeScroll).take(rows).forEachIndexed {i,k->val y=70+i*16;val active=k.id()==choice.knife
    if(active)surface(g,Box(12,y-2,122,15),0xff41464d.toInt(),0xffaab8c8.toInt())
    text(g,k.name(),18,y,if(active)ink else muted,110)
    hits.add(Hit(Box(12,y-2,122,16)){edit {it.copy(knife=k.id(),finish="")};finishScroll=0;seedBox?.value=selection().seed.toString()})
   }
   if(filtered.size>rows)text(g,"Scroll for more knives",15,300,muted,120)
   button(g,"Finishes",Box(144,46,56,18),tab==0){tab=0}
   button(g,"View",Box(203,46,38,18),tab==1){tab=1}
   button(g,"Item Bindings",Box(244,46,78,18),tab==2){tab=2}
   finishSearch?.visible=tab==0
   when(tab) {
    0->{surface(g,Box(144,68,178,18))
     val finishes=(listOf(Cs2Library.Finish("","Vanilla"))+(current?.finishes() ?: emptyList())).filter {it.name().contains(finishSearch?.value ?: "",true)}
     finishScroll=finishScroll.coerceIn(0,((finishes.size+1)/2-3).coerceAtLeast(0))
     val visible=finishes.drop(finishScroll*2).take(6)
     visible.forEachIndexed {i,f->val b=Box(144+(i%2)*91,90+(i/2)*73,87,69);surface(g,b,0xff2c3036.toInt(),if(f.kit()==choice.finish)0xffdfe8f2.toInt()else 0xff424850.toInt())
      photo(g,previews.image(choice.knife,f.kit(),0,.01,true),Box(b.x+4,b.y+3,b.w-8,49));text(g,f.name(),b.x+5,b.y+56,ink,b.w-10)
      hits.add(Hit(b){edit {it.copy(finish=f.kit())}})
     }
     if(finishes.isEmpty())text(g,"No matching finishes",149,103,muted,168)
    }
    1->{text(g,"View model",148,77,ink,168)
     slider(g,"Horizontal",Box(148,98,166,23),settings().offsetX,-2.0,2.0,.01){v->update {it.copy(offsetX=v)}}
     slider(g,"Vertical",Box(148,132,166,23),settings().offsetY,-2.0,2.0,.01){v->update {it.copy(offsetY=v)}}
     slider(g,"Distance",Box(148,166,166,23),settings().distance,-2.0,2.0,.01){v->update {it.copy(distance=v)}}
     button(g,"Reset view",Box(148,204,166,20)){update {it.copy(offsetX=0.0,offsetY=0.0,distance=0.0)}}
     text(g,"Held Item Shader",148,240,ink,168)
     button(g,"Hand / Arm: ${if(model.general.features.heldItem.arms) "On"else "Off"}",Box(148,258,166,20),model.general.features.heldItem.arms){model.updateGeneral {it.copy(features=it.features.copy(heldItem=it.features.heldItem.copy(arms=!it.features.heldItem.arms)))}}
     text(g,"Enable Held Item Shader separately.",148,287,muted,168)
    }
    2->bindings(g)
   }
   val previewBox=Box(332,46,204,126);surface(g,previewBox,0xff252a31.toInt())
   val key=choice.key();if(key!=previewKey){previewKey=key;previewChanged=System.nanoTime()}
   val picture=previews.image(choice.knife,choice.finish,choice.seed,choice.wear,false,System.nanoTime()-previewChanged>=200_000_000L)
   photo(g,picture,Box(337,51,194,116))
   text(g,current?.name() ?: "Assets not loaded",332,182,ink,204)
   text(g,if(choice.finish.isBlank())"Vanilla"else current?.finishes()?.firstOrNull {it.kit()==choice.finish}?.name() ?: choice.finish,332,194,muted,204)
   surface(g,Box(360,200,72,16));text(g,"Seed",332,204,muted,27)
   val seed=seedBox;if(seed!=null && !seed.isFocused && seed.value!=choice.seed.toString())seed.value=choice.seed.toString()
   button(g,"Random",Box(440,200,96,17)){val n=kotlin.random.Random.nextInt(1001);edit {it.copy(seed=n)};seedBox?.value=n.toString()}
   slider(g,"Wear / Float",Box(332,224,204,23),choice.wear,0.0,1.0,.001){v->edit {it.copy(wear=v)}}
   val rules=listOf("weapons","held","empty");listOf("Swords / Axes","Any item","Empty hand").forEachIndexed {i,label->button(g,label,Box(332+i*68,252,66,19),settings().applyTo==rules[i]){update {it.copy(applyTo=rules[i])}}}
   button(g,"Animations: ${if(settings().animations)"On"else "Off"}",Box(332,278,204,18),settings().animations){update {it.copy(animations=!it.animations)}}
   button(g,"Third person: ${if(settings().thirdPerson)"On"else "Off"}",Box(332,300,204,18),settings().thirdPerson){update {it.copy(thirdPerson=!it.thirdPerson)}}
   fields().forEach {it.extractRenderState(g,mouse.first.toInt(),mouse.second.toInt(),delta)}
   text(g,model.error?.let {"Not saved: $it"} ?: notice ?: Cs2Knives.status(),12,320,if(model.error!=null)0xffff9090.toInt()else muted,310)
  }finally {g.pose().popMatrix()}
 }
 private fun bindings(g:GuiGraphicsExtractor) {
  text(g,"Per-item knife setups",148,76,ink,168)
  button(g,"Bind selection to held item",Box(148,94,166,22)) {
   val uuid=heldUuid()
   if(uuid==null)notice="Held item has no stable SkyBlock UUID"
   else {
    val c=selection();val label=held()?.hoverName?.string?.replace(Regex("\\s+")," ")?.take(96)?.ifBlank {"Item"} ?: "Item"
    if(settings().bindings.none {KnifeBindings.canonical(it.uuid)==uuid} && settings().bindings.size>=128)notice="Maximum 128 item bindings"
    else {update {it.copy(bindings=it.bindings.filterNot {b->KnifeBindings.canonical(b.uuid)==uuid}+KnifeItemBinding(uuid,label,c.knife,c.finish,c.seed,c.wear))};editingUuid=uuid;notice="Saved item-specific knife"}
   }
  }
  text(g,"Uses item UUID, not name/type.",148,123,muted,168)
  val entries=settings().bindings;bindingScroll=bindingScroll.coerceIn(0,(entries.size-4).coerceAtLeast(0))
  entries.drop(bindingScroll).take(4).forEachIndexed {i,b->val y=143+i*40
   text(g,b.label,148,y,if(b.uuid==editingUuid)accent else ink,166)
   button(g,"Edit",Box(148,y+12,46,18)){editingUuid=b.uuid;tab=0;finishScroll=0;seedBox?.value=b.seed.toString()}
   button(g,if(b.enabled)"On"else "Off",Box(198,y+12,43,18),b.enabled){update {s->s.copy(bindings=s.bindings.map {if(it.uuid==b.uuid)it.copy(enabled=!it.enabled)else it})}}
   button(g,"Delete",Box(245,y+12,69,18)){update {s->s.copy(bindings=s.bindings.filterNot {it.uuid==b.uuid})};if(editingUuid==b.uuid)editingUuid=null}
  }
  if(entries.isEmpty()){text(g,"Hold a unique SkyBlock item,",148,151,muted,168);text(g,"then bind your knife selection.",148,165,muted,168)}
 }
 override fun mouseClicked(event:MouseButtonEvent,doubled:Boolean):Boolean {
  val (x,y)=xy(event.x(),event.y());lastX=x
  fields().forEach {it.isFocused=it.isMouseOver(x,y)}
  fields().firstOrNull {it.isFocused}?.let {return it.mouseClicked(event(event),doubled)}
  if(event.button()==0){hits.lastOrNull {it.box.has(x,y)}?.let {it.action();return true}
   if(x in 0.0..W.toDouble() && y in 0.0..35.0)drag.begin(event.x(),event.y(),event.button(),true)
  }
  return true
 }
 override fun mouseReleased(event:MouseButtonEvent):Boolean {drag.end();sliding=null;fields().firstOrNull {it.isFocused}?.mouseReleased(event(event));return true}
 override fun mouseDragged(event:MouseButtonEvent,dx:Double,dy:Double):Boolean {
  if(drag.dragging){drag.move(event.x(),event.y());return true}
  sliding?.let {it(xy(event.x(),event.y()).first);return true}
  fields().firstOrNull {it.isFocused}?.mouseDragged(event(event),dx/scale,dy/scale);return true
 }
 override fun mouseScrolled(x:Double,y:Double,horizontal:Double,vertical:Double):Boolean {
  val (mx,my)=xy(x,y);if(my<65 || my>316)return true
  val step=if(vertical>0)-1 else if(vertical<0)1 else 0
  if(mx in 12.0..134.0)knifeScroll=(knifeScroll+step).coerceAtLeast(0)
  else if(mx in 144.0..322.0){if(tab==0)finishScroll=(finishScroll+step).coerceAtLeast(0)else if(tab==2)bindingScroll=(bindingScroll+step).coerceAtLeast(0)}
  return true
 }
 override fun keyPressed(event:KeyEvent):Boolean {
  if(event.key()==GLFW.GLFW_KEY_ESCAPE){host.closePanel();return true}
  return fields().firstOrNull {it.isFocused}?.keyPressed(event) ?: true
 }
 override fun charTyped(event:CharacterEvent):Boolean {
  val field=fields().firstOrNull {it.isFocused} ?: return true
  if(field===seedBox && event.codepoint() !in 48..57)return true
  return field.charTyped(event)
 }
 override fun dispose(){previews.close();hits.clear();sliding=null;drag.end()}
}
