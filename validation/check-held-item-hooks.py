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
jar=str(p/'build/libs/solyanka-1.3.9.jar');compiled=dump('dev.solyanka.client.mixin.HeldItemFeatureMixin',jar)
assert '(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;)Lnet/minecraft/client/renderer/rendertype/RenderType;' in compiled
src=p/'src/client/kotlin/dev/solyanka/client';hook=(src/'mixin/HeldItemFeatureMixin.kt').read_text();feature=(src/'feature/HeldItemCosmetic.kt').read_text()
assert 'prepareMainSubmit(' in hook and 'prepareOutlineSubmit' not in hook and 'prepareFoilSubmit' not in hook and 'GlRenderPass' not in hook
for needle in ['RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET)','withSampler("Sampler3")','withSampler("Sampler4")','withSampler("Sampler5")','NativeImage(3,1,false)','types.size>=64','@Synchronized @JvmStatic fun wrap','textureManager.release(configId)']:assert needle in feature,needle
wrap=feature.split('@Synchronized @JvmStatic fun wrap',1)[1].split('@Synchronized fun close',1)[0]
assert '.upload()' not in wrap and 'NativeImage(' not in wrap and 'pipelineDelegate.value // Register' in feature
assert 'HeldItemClock.texel(animationClock.sample(System.nanoTime()' in feature
assert 'lastPhase==phase' in feature and 'pixels.setPixel(2,0,phase)' in feature
shader=(p/'src/main/resources/assets/solyanka/shaders/core/held_item.fsh').read_text()
assert 'uintBitsToFloat(clockBytes.r' in shader and 'shaderTime = GameTime' not in shader
assert 'RenderSystem.isOnRenderThread()' in feature and '@Volatile private var ready=false' in feature
assert len(json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'])==44
# Resolve every native shader include. This is not native GLSL compilation.
imports=set()
with zipfile.ZipFile(cp) as z:
 def resolve(text):
  for name in re.findall(r'#moj_import\s*<([^>]+)>',text):
   ns,path=name.split(':',1) if ':' in name else ('minecraft',name);key=f'assets/{ns}/shaders/include/{path}'
   if key not in imports:imports.add(key);resolve(z.read(key).decode())
 for ext in ['vsh','fsh']:resolve((p/f'src/main/resources/assets/solyanka/shaders/core/held_item.{ext}').read_text())
# Confirm source recheck and the new render-head preparation callback, not just the old main hook.
import hashlib
for r in json.loads((p/'audit/held-item-recheck-source-manifest.json').read_text())['files']:
 assert hashlib.sha256((p/r['local_file']).read_bytes()).hexdigest()==r['sha256']
assert 'withColorTargetState(ColorTargetState.DEFAULT)' in feature
assert 'precompilePipeline(pipeline)' in feature and 'if(!result.isValid) {ready=false;failed=true;' in feature
assert feature.index('image.setPixel(0,0,values[0])')<feature.index('DynamicTexture({')
client=(src/'SolyankaClient.kt').read_text();assert 'HeldItemCosmetic.register()' in client and 'HeldItemCosmetic.tick()' not in client
assert 'literal("helditemstatus")' in client
native=dump('net.minecraft.client.renderer.GameRenderer')
level=re.search(r'public void renderLevel\(.*?(?=\n  (?:public|private|protected)|\Z)',native,re.S).group()
assert 'Method renderItemInHand:' in level
lifecycle=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-c','-s','-v','-classpath',jar,'dev.solyanka.client.mixin.TemporalBlurMixin'],text=True)
handler=re.search(r'private (?:final )?void solyankaHeldFrame\(.*?(?=\n  (?:public|private|protected)|\Z)',lifecycle,re.S).group()
assert 'HeldItemCosmetic.prepareFrame:()V' in handler and 'HEAD' in handler
result=dict(targets=targets,main_material_callsites=1,compiled_handler_signature_verified=True,main_only=True,native_image_setPixel_accepts_argb=True,render_thread_only_texture_upload=True,render_head_preparation_verified=True,pipeline_registered_at_client_init=True,native_compile_result_checked=True,opaque_fill_matches_stray=True,settings_initialized_before_first_upload=True,diagnostic_command=True,upstream_head_rechecked=True,shader_symptom_root_cause_confirmed=False,world_clock_dependency_removed=True,monotonic_phase_texture=True,phase_preserved_on_texture_recreation=True,wrap_creates_metadata_only=True,pipeline_prepared_before_ready=True,synchronized_bounded_atlas_cache=True,native_includes_resolved=sorted(imports),mixin_classes=44,in_game_tested_here=False,mixin_application_tested_here=False,native_gpu_tested_here=False)
(p/'validation/held-item-static.json').write_text(json.dumps(result,indent=2)+'\n');print('Held-item method/operation targets verified:',len(targets),'; single main callsite; native includes:',len(imports))
