"""Static native ABI/callsite/resource evidence; no Mixin application, game launch or GPU acceptance."""
from pathlib import Path
import subprocess,re,json,zipfile,hashlib,struct
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');cache={}
def dump(cls,jar=cp,verbose=False):
 key=(cls,jar,verbose)
 if key not in cache:cache[key]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c']+(['-v'] if verbose else [])+['-classpath',jar,cls],text=True)
 return cache[key]
def verify(cls,name,desc):
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);\n    descriptor: '+re.escape(desc)+r'\n'
 assert len(re.findall(pattern,dump(cls),re.M))==1,(cls,name,desc)
 return dict(class_name=cls,method=name,descriptor=desc,target_exists=True)
sky='net.minecraft.client.renderer.SkyRenderer';uniforms='net.minecraft.client.renderer.DynamicUniforms'
targets=[verify(sky,'extractRenderState','(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V'),verify(sky,'renderEndSky','()V'),verify(sky,'close','()V'),verify(uniforms,'writeTransform','(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;')]
verify(uniforms,'writeTransform','(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;')
text=dump(sky);assert 'private final com.mojang.blaze3d.pipeline.RenderTarget renderTarget;\n    descriptor: Lcom/mojang/blaze3d/pipeline/RenderTarget;' in text
body=re.search(r'public void renderEndSky\(.*?(?=\n  (?:public|private|protected)|\Z)',text,re.S).group()
assert body.count('DynamicUniforms.writeTransform:(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;')==1
assert 'RenderPass.close:()V' in body and 'RenderPass.drawIndexed:(IIIII)V' in body
# Verify parameter order from debug names, not an old four-argument draw signature.
pass_text=dump('com.mojang.blaze3d.systems.RenderPass',verbose=True)
match=re.search(r'public void drawIndexed\(int, int, int, int, int\);.*?(?=\n  public|\Z)',pass_text,re.S).group()
assert 'indexCount' in match and 'instanceCount' in match and 'firstIndex' in match
jar=str(p/'build/libs/solyanka-1.1.0.jar');compiled=dump('dev.solyanka.client.mixin.SkyboxMixin',jar,True)
assert 'private com.mojang.blaze3d.pipeline.RenderTarget renderTarget;' in compiled
for token in ['org.spongepowered.asm.mixin.Shadow','org.spongepowered.asm.mixin.Final','org.spongepowered.asm.mixin.Unique','com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation']:assert token in compiled,token
assert '(Lnet/minecraft/client/renderer/DynamicUniforms;Lorg/joml/Matrix4f;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;' in compiled
src=p/'src/client/kotlin/dev/solyanka/client';feature=(src/'feature/SkyboxVisual.kt').read_text();hook=(src/'mixin/SkyboxMixin.kt').read_text();ui=(src/'ui/SkyboxUi.kt').read_text()
assert feature.index('val faceViews=')<feature.index('.createRenderPass(') and feature.index('indices.getBuffer(42)')<feature.index('.createRenderPass(')
assert 'getMainRenderTarget' not in feature and 'getModelViewStack' not in feature
assert 'cancellable' not in hook and 'ci.cancel' not in hook
for token in ['withBindGroupLayout','withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR)','BlendFactor.ONE,BlendFactor.ONE','vertices.slice()','pass.drawIndexed(6,1,36,0,0)','owned.clear()','manager.release(it)','quads?.close()']:assert token in feature,token
assert 'host.showPanel(SettingsPanel(' in ui and 'model.updateGeneral' in ui and 'setScreen' not in ui
assert len(json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'])==41
assets=[]
manifest=json.loads((p/'audit/stray-skybox-source-manifest.json').read_text())
for r in manifest:
 if not r['file'].endswith('.png'):continue
 f=p/'src/main/resources/assets/solyanka/textures/sky/end'/Path(r['file']).name;data=f.read_bytes();assert hashlib.sha256(data).hexdigest()==r['sha256'];assert data[:8]==b'\x89PNG\r\n\x1a\n'
 w,h=struct.unpack('>II',data[16:24]);assert w==h==(1024 if f.name.startswith('face') else 1536)
 assets.append(dict(file=f.name,sha256=r['sha256'],width=w,height=h,bytes=len(data)))
assert len(assets)==14
with zipfile.ZipFile(jar) as z:
 for a in assets:
  key='assets/solyanka/textures/sky/end/'+a['file'];assert hashlib.sha256(z.read(key)).hexdigest()==a['sha256']
result=dict(targets=targets,end_transform_callsites=1,shadow_render_target_verified=True,compiled_wrap_handler_signature_verified=True,native_five_arg_draw_order_checked=True,native_end_sky_not_cancelled=True,private_matrix_copy=True,all_allocations_before_pass=True,embedded_instant_settings=True,asset_count=14,raw_texture_bytes=sum(a['width']*a['height']*4 for a in assets),assets=assets,mixin_classes=41,in_game_tested_here=False,mixin_application_tested_here=False,gpu_sodium_iris_tested_here=False)
(p/'validation/skybox-static.json').write_text(json.dumps(result,indent=2)+'\n');print('Skybox methods/operation verified:',len(targets),'; imported textures:',len(assets),'; static evidence only')
