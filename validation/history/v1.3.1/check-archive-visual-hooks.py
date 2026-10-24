"""Native 26.2 API/compiled hook checks; not a launched Mixin or GPU test."""
from pathlib import Path
import json,re,subprocess,zipfile,hashlib
p=Path(__file__).resolve().parents[1]
cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar')
jar=str(p/'build/libs/solyanka-1.3.1.jar');cache={}
def dump(cls,classpath=cp,verbose=False):
 key=(cls,classpath,verbose)
 if key not in cache:cache[key]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c']+(['-v'] if verbose else [])+['-classpath',classpath,cls],text=True)
 return cache[key]
def verify(cls,name,desc,classpath=cp):
 text=dump(cls,classpath)
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);\n    descriptor: '+re.escape(desc)+r'\n'
 assert len(re.findall(pattern,text,re.M))==1,(cls,name,desc)
 return dict(class_name=cls,method=name,descriptor=desc,target_exists=True)
desc='(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V'
targets=[verify('net.minecraft.client.renderer.ItemInHandRenderer','applyItemArmTransform',desc)]
text=dump('net.minecraft.client.renderer.ItemInHandRenderer')
method=re.search(r'private void submitArmWithItem\(.*?(?=\n  (?:public|private|protected)|\Z)',text,re.S).group()
assert method.count('Method applyItemArmTransform:'+desc)==5
assert method.index('ItemStack.isEmpty:()Z')<method.index('renderPlayerArm:')<method.index('renderOneHandedMap:')<method.index('Method applyItemArmTransform:')
compiled=dump('dev.solyanka.client.mixin.RotatingItemMixin',jar,True)
handler='(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V'
for token in [handler,'org.spongepowered.asm.mixin.injection.Inject','applyItemArmTransform'+desc,'value="RETURN"','ArchiveVisuals.transform:(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;)V']:
 assert token in compiled,token
apis=[verify('net.minecraft.client.multiplayer.ClientLevel','addParticle','(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V')]
particles=dump('net.minecraft.core.particles.ParticleTypes')
for name in ['END_ROD','FIREFLY','CHERRY_LEAVES','SNOWFLAKE','SOUL','PORTAL','WITCH','HAPPY_VILLAGER']:
 assert f'public static final net.minecraft.core.particles.SimpleParticleType {name};' in particles,name
fabric=next(Path.home().glob('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-rendering-v1/*/*/fabric-rendering-v1-*.jar'))
apis.append(verify('net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry','replaceElement','(Lnet/minecraft/resources/Identifier;Ljava/util/function/Function;)V',str(fabric)))
apis.append(verify('net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement','extractRenderState','(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V',str(fabric)))
s=p/'src/client/kotlin/dev/solyanka/client';visual=(s/'feature/ArchiveVisuals.kt').read_text();hearts=(s/'feature/VanillaHearts.kt').read_text();ui=(s/'ui/ArchiveVisualUi.kt').read_text();client=(s/'SolyankaClient.kt').read_text();editor=(s/'ui/HudEditorScreen.kt').read_text()
for token in ['!S.isEnabled(S.AMBIENT_PARTICLES)','client.isPaused','client.level ?: return','client.player ?: return','repeat(s.amount)','level.addParticle(']:assert token in visual,token
assert 'sendCommand' not in visual and 'sendChat' not in visual
for token in ['HudElementRegistry.replaceElement','original.extractRenderState(g,d)','finally {g.pose().popMatrix()}','s.moveArmor','s.hud.enabled','HudEditorScreen']:assert token in hearts,token
assert client.index('FeatureRuntime.register()')<client.index('Suppression.register()')<client.index('VanillaHearts.register()')
assert 'key,width,height,block' in editor
assert 'host.showPanel(SettingsPanel(' in ui and 'model.updateGeneral' in ui and 'setScreen' not in ui
reg=json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'];assert len(reg)==41
with zipfile.ZipFile(jar) as z:
 assert json.loads(z.read('fabric.mod.json'))['version']=='1.3.1'
 for n in ['dev/solyanka/client/feature/ArchiveVisuals.class','dev/solyanka/client/feature/VanillaHearts.class','dev/solyanka/client/mixin/RotatingItemMixin.class','dev/solyanka/client/ui/ArchiveVisualUi.class','dev/solyanka/utility/HeartLayout.class','dev/solyanka/utility/RotatingItemLogic.class','dev/solyanka/utility/AmbientParticleLogic.class']:assert n in z.namelist(),n
 assert not any('AetherOS' in n for n in z.namelist())
archive=json.loads((p/'audit/archive-inventory.json').read_text());manifest=json.loads((p/'audit/archive-visual-source-manifest.json').read_text());assert len(manifest)==3
for m in manifest:
 a=next(r for r in archive if r['file']==m['file']);assert a['sha256']==m['sha256'] and a['module']==m['module'];assert a['status']=='IMPLEMENTED_ADAPTED_BUILD_TESTED_INGAME_PENDING'
result=dict(version='1.3.1',targets=targets,api_targets=apis,native_item_transform_callsites=5,compiled_injection_signature_verified=True,empty_hand_and_map_paths_bypass_hook=True,source_particle_types_available=8,client_only_bounded_spawns=True,heart_original_element_delegated=True,heart_pose_restored_in_finally=True,existing_hud_suppression_wrappers_preserved=True,editor_uses_live_resolution=True,embedded_instant_settings=True,archive_source_hashes_verified=True,mixin_classes=41,in_game_tested_here=False,mixin_application_tested_here=False,gpu_sodium_iris_tested_here=False)
(p/'validation/archive-visual-static.json').write_text(json.dumps(result,indent=2)+'\n')
print('Archive visuals: 1 native injection target, 5 callsites, 3 API methods, 8 particle types; compiled callback and wrapper guards checked; static only')
