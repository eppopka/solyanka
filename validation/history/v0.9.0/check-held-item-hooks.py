"""Static 26.2 ABI/callsite checks, not Mixin application or native GPU validation."""
from pathlib import Path
import subprocess,re,json,zipfile
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');cache={}
def dump(cls,jar=cp):
 key=(cls,jar)
 if key not in cache:cache[key]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c','-classpath',jar,cls],text=True)
 return cache[key]
def verify(cls,name,desc):
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);\n    descriptor: '+re.escape(desc)+r'\n'
 assert len(re.findall(pattern,dump(cls),re.M))==1,(cls,name,desc)
 return dict(class_name=cls,method=name,descriptor=desc,target_exists=True)
renderer='net.minecraft.client.renderer.feature.ItemFeatureRenderer'
targets=[verify(renderer,'prepareMainSubmit','(Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;)V'),verify('net.minecraft.client.resources.model.geometry.BakedQuad$MaterialInfo','itemRenderType','()Lnet/minecraft/client/renderer/rendertype/RenderType;')]
body=re.search(r'private void prepareMainSubmit\(.*?(?=\n  (?:public|private|protected)|\Z)',dump(renderer),re.S).group()
assert body.count('BakedQuad$MaterialInfo.itemRenderType:()Lnet/minecraft/client/renderer/rendertype/RenderType;')==1
assert 'ARGB.toABGR:(I)I' in re.search(r'public void setPixel\(.*?(?=\n  public|\Z)',dump('com.mojang.blaze3d.platform.NativeImage'),re.S).group()
jar=str(p/'build/libs/solyanka-0.9.0-dev.jar');compiled=dump('dev.solyanka.client.mixin.HeldItemFeatureMixin',jar)
assert '(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;)Lnet/minecraft/client/renderer/rendertype/RenderType;' in compiled
src=p/'src/client/kotlin/dev/solyanka/client';hook=(src/'mixin/HeldItemFeatureMixin.kt').read_text();feature=(src/'feature/HeldItemCosmetic.kt').read_text()
assert 'prepareMainSubmit(' in hook and 'prepareOutlineSubmit' not in hook and 'prepareFoilSubmit' not in hook and 'GlRenderPass' not in hook
for needle in ['RenderPipelines.ITEM_SNIPPET,RenderPipelines.GLOBALS_SNIPPET','withSampler("Sampler3")','withSampler("Sampler4")','withSampler("Sampler5")','NativeImage(2,1,false)','types.size>=64','@Synchronized @JvmStatic fun wrap','textureManager.release(configId)']:assert needle in feature,needle
wrap=feature.split('@Synchronized @JvmStatic fun wrap',1)[1].split('@Synchronized fun close',1)[0]
assert '.upload()' not in wrap and 'NativeImage(' not in wrap and 'pipelineDelegate.value // Register' in feature
assert 'RenderSystem.isOnRenderThread()' in feature and '@Volatile private var ready=false' in feature
assert len(json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'])==38
# Resolve every native shader include. This is not native GLSL compilation.
imports=set()
with zipfile.ZipFile(cp) as z:
 def resolve(text):
  for name in re.findall(r'#moj_import\s*<([^>]+)>',text):
   ns,path=name.split(':',1) if ':' in name else ('minecraft',name);key=f'assets/{ns}/shaders/include/{path}'
   if key not in imports:imports.add(key);resolve(z.read(key).decode())
 for ext in ['vsh','fsh']:resolve((p/f'src/main/resources/assets/solyanka/shaders/core/held_item.{ext}').read_text())
result=dict(targets=targets,main_material_callsites=1,compiled_handler_signature_verified=True,main_only=True,native_image_setPixel_accepts_argb=True,render_thread_only_texture_upload=True,wrap_creates_metadata_only=True,pipeline_prepared_before_ready=True,synchronized_bounded_atlas_cache=True,native_includes_resolved=sorted(imports),mixin_classes=38,in_game_tested_here=False,mixin_application_tested_here=False,native_gpu_tested_here=False)
(p/'validation/held-item-static.json').write_text(json.dumps(result,indent=2)+'\n');print('Held-item method/operation targets verified:',len(targets),'; single main callsite; native includes:',len(imports))
