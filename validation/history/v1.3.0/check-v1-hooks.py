"""Native 26.2 static hook/API evidence, not an applied-Mixin or in-game GPU test."""
from pathlib import Path
import subprocess,re,json,zipfile,hashlib
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');cache={}
def dump(cls,jar=cp,verbose=False):
 key=(cls,jar,verbose)
 if key not in cache:cache[key]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c']+(['-v'] if verbose else [])+['-classpath',jar,cls],text=True)
 return cache[key]
def verify(cls,name,desc):
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);\n    descriptor: '+re.escape(desc)+r'\n'
 assert len(re.findall(pattern,dump(cls),re.M))==1,(cls,name,desc)
 return dict(class_name=cls,method=name,descriptor=desc,target_exists=True)
game='net.minecraft.client.renderer.GameRenderer'
targets=[verify(game,'renderLevel','(Lnet/minecraft/client/DeltaTracker;)V'),verify(game,'close','()V'),verify(game,'resize','(II)V'),verify(game,'resetData','()V')]
text=dump(game);assert 'private final com.mojang.blaze3d.pipeline.RenderTarget mainRenderTarget;\n    descriptor: Lcom/mojang/blaze3d/pipeline/RenderTarget;' in text
render=re.search(r'public void render\(net.minecraft.client.DeltaTracker, boolean\);.*?(?=\n  (?:public|private|protected)|\Z)',text,re.S).group()
assert render.index('Method renderLevel:')<render.index('GuiRenderer.render:()V')
pass_text=dump('com.mojang.blaze3d.systems.RenderPass',verbose=True);draw=re.search(r'public void draw\(int, int, int, int\);.*?(?=\n  public|\Z)',pass_text,re.S).group();assert all(n in draw for n in ['vertexCount','instanceCount','firstVertex','firstInstance'])
verify('com.mojang.blaze3d.systems.CommandEncoder','copyTextureToTexture','(Lcom/mojang/blaze3d/textures/GpuTexture;Lcom/mojang/blaze3d/textures/GpuTexture;IIIIIII)V')
jar=str(p/'build/libs/solyanka-1.3.0.jar');mixin=dump('dev.solyanka.client.mixin.TemporalBlurMixin',jar,True)
for token in ['private com.mojang.blaze3d.pipeline.RenderTarget mainRenderTarget;','org.spongepowered.asm.mixin.Shadow','org.spongepowered.asm.mixin.Final','TemporalBlur.apply:(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V','TemporalBlur.close:()V']:assert token in mixin,token
s=p/'src/client/kotlin/dev/solyanka/client';blur=(s/'feature/TemporalBlur.kt').read_text();hud=(s/'feature/ArchiveHuds.kt').read_text();client=(s/'SolyankaClient.kt').read_text();ui=(s/'ui/ReleaseOneUi.kt').read_text()
for token in ['false,format)','GpuFormat.RGBA8_UNORM,GpuFormat.RGBA16_FLOAT','copy(next,main);read=next;write=previous','pass.draw(3,1,0,0)','now-lastFrame>500_000_000L','RenderSystem.isOnRenderThread()','if(!seeded','params?.close()','destroyBuffers()']:assert token in blur,token
assert blur.index('encoder.writeToBuffer')<blur.index('encoder.createRenderPass')
assert 'getMainRenderTarget' not in blur and 'copyDepthFrom' not in blur
assert client.index('ArchiveHuds.input(device,key)')<client.index('if (!isEnabled(KEYBINDS))')
assert 'if (action == GLFW.GLFW_RELEASE) return ownedInputs.remove(token)' in client
assert 'if (action == GLFW.GLFW_REPEAT) return token in ownedInputs' in client
for token in ['c.gui.screen()!=null','!c.isWindowActive','sendSystemMessage','SimpleSoundInstance.forUI','soundPending=false']:assert token in hud,token
assert 'sendCommand' not in hud and 'sendChat' not in hud
assert 'host.showPanel(SettingsPanel(' in ui and 'model.updateGeneral' in ui and 'panel.inputRow(' in ui and 'setScreen' not in ui
reg=json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'];assert len(reg)==41
with zipfile.ZipFile(jar) as z:
 for mode in ['accum_mix','accum_max']:
  key=f'assets/solyanka/shaders/post/{mode}.fsh';assert z.read(key)==(p/'src/main/resources'/key).read_bytes()
 assert not any('AetherOS' in n for n in z.namelist())
archive=json.loads((p/'audit/archive-inventory.json').read_text());manifest=json.loads((p/'audit/v1-source-manifest.json').read_text())
for path in ['impl/general/Timer.kt','impl/visual/DvdBounce.kt']:
 a=next(r for r in archive if r['file']==path);m=next(r for r in manifest if r['file']==path);assert a['sha256']==m['sha256'];assert a['status']=='IMPLEMENTED_ADAPTED_BUILD_TESTED_INGAME_PENDING'
result=dict(targets=targets,owning_main_target_shadow_verified=True,compiled_tail_and_cleanup_callbacks=True,native_draw_parameter_order_checked=True,world_render_before_gui=True,history_targets_depthless_and_matching_format=True,first_frame_reseed_guard=True,history_long_gap_guard=True,zero_strength_releases_history=True,history_formats_supported=['RGBA8_UNORM','RGBA16_FLOAT'],history_pixel_budget=8388608,release_repeat_input_ownership_preserved=True,timer_local_feedback_only=True,archive_source_hashes_verified=True,new_gui_embedded_instant_save=True,mixin_classes=41,in_game_tested_here=False,mixin_application_tested_here=False,gpu_sodium_iris_tested_here=False)
(p/'validation/v1-static.json').write_text(json.dumps(result,indent=2)+'\n');print('v1.0 native hook methods checked:',len(targets),'; blur target/draw order and timer input guards verified; static only')
