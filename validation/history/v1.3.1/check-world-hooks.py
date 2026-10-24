"""Static 26.2 descriptor/callsite checks, not a Mixin runtime launch."""
from pathlib import Path
import subprocess,re,json
p=Path(__file__).resolve().parents[1]
cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar')
cache={}
def dump(cls):
 if cls not in cache:cache[cls]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c','-classpath',cp,cls],text=True)
 return cache[cls]
def verify(cls,name,descriptor):
 text=dump(cls);matches=list(re.finditer(r'^  (?:public|private|protected)[^\n]*\b'+name+r'\([^\n]*\);\n    descriptor: '+re.escape(descriptor)+r'\n',text,re.M));assert len(matches)==1,(cls,name,descriptor)
 return dict(class_name=cls,method=name,descriptor=descriptor,target_exists=True)
targets=[
 verify('net.minecraft.client.Minecraft','shouldEntityAppearGlowing','(Lnet/minecraft/world/entity/Entity;)Z'),
 verify('net.minecraft.client.renderer.entity.EntityRenderer','extractRenderState','(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V'),
 verify('net.minecraft.client.renderer.entity.EntityRenderer','affectedByCulling','(Lnet/minecraft/world/entity/Entity;)Z'),
 verify('net.minecraft.client.renderer.entity.EntityRenderer','submitNameDisplay','(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V')]
state=dump('net.minecraft.client.renderer.entity.state.EntityRenderState');assert 'public int outlineColor;\n    descriptor: I' in state
renderer=dump('net.minecraft.client.renderer.entity.EntityRenderer');assert 'Minecraft.shouldEntityAppearGlowing:(Lnet/minecraft/world/entity/Entity;)Z' in renderer
assert 'Field net/minecraft/client/renderer/entity/state/EntityRenderState.outlineColor:I' in renderer
outline=dump('net.minecraft.client.renderer.LevelRenderer');assert 'blockOutlineRenderState:' in outline
xp=json.loads((p/'validation/experience-bar-static.json').read_text())
for t in xp['targets']:verify(t['class'],t['method'],t['descriptor'])
source=p/'src/client/kotlin/dev/solyanka/client'
assert 'outline==null' in (source/'feature/BlockSelection.kt').read_text()
reg=json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'];assert len(reg)==41
jar=p/'build/libs/solyanka-1.3.1.jar'
mod=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-classpath',str(jar),'dev.solyanka.client.mixin.VisualEntityStateMixin'],text=True)
assert 'implements dev.solyanka.client.feature.VisualEntityState' in mod and 'getSolyankaHideName()' in mod and 'setSolyankaHideName(boolean)' in mod
selection=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-c','-classpath',str(jar),'dev.solyanka.client.feature.BlockSelection'],text=True)
callback=re.search(r'private static final boolean register\$lambda\$0.*?(?=\n  private static|\Z)',selection,re.S).group()
assert 'String outline' not in callback and ('ifnull' in callback or 'ifnonnull' in callback)
checks=dict(compiled_outline_callback_null_checked=True,targets=targets,state_outline_color_field=True,base_extraction_calls_minecraft_glow_query=True,duck_state_interface_compiled=True,nullable_outline_guard=True,xp_targets_rechecked=2,mixin_classes=41,in_game_tested_here=False,mixin_application_tested_here=False)
(p/'validation/world-visual-static.json').write_text(json.dumps(checks,indent=2)+'\n')
print('World targets verified:',len(targets),'XP targets rechecked: 2; mixins:',len(reg))
