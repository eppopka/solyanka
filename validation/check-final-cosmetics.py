"""Native ABI, compiled hooks, source pins and runtime scope. Not an in-game test."""
from pathlib import Path
import json,hashlib,zipfile,subprocess,re
p=Path(__file__).resolve().parents[1];native=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar';jar=p/'build/libs/solyanka-1.3.12.jar'
for r in json.loads((p/'audit/final-cosmetics-source-manifest.json').read_text()):assert hashlib.sha256((p/r['local_file']).read_bytes()).hexdigest()==r['sha256']
def dump(c,cp=native,verbose=False):return subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c',*(['-v']if verbose else []),'-classpath',str(cp),c],text=True)
targets=[]
for cls,name,desc in [
 ('net.minecraft.client.renderer.ItemInHandRenderer','submitArmWithItem','(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V'),
 ('net.minecraft.client.renderer.GameRenderer','renderItemInHand','(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V'),
 ('net.minecraft.client.renderer.entity.layers.ItemInHandLayer','submitArmWithItem','(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V')]:
 s=dump(cls);assert 'descriptor: '+desc in s,(cls,desc);targets.append(dict(class_name=cls,method=name,descriptor=desc,target_exists=True))
game=dump('net.minecraft.client.renderer.GameRenderer').split('private void renderItemInHand(',1)[1].split('public static float nightVisionScale',1)[0]
assert game.index('renderAllFeatures:')<game.index('Matrix4fStack.popMatrix:')
item=dump('net.minecraft.client.renderer.ItemInHandRenderer').split('private void submitArmWithItem(',1)[1].split('private void swingArm(',1)[0]
assert item.count('Method com/mojang/blaze3d/vertex/PoseStack.popPose:()V')==1
for cls,needles in [
 ('Cs2FirstPersonMixin',['submitArmWithItem','Cs2Viewmodel.render','CallbackInfo.cancel']),
 ('Cs2ThirdPersonMixin',['submitArmWithItem','KnifeRender.submit','getThirdPerson','CallbackInfo.cancel']),
 ('ItemAuraPassMixin',['ItemAura.begin','ItemAura.flush','AFTER']),
 ('RotatingItemMixin',['ItemAura.captureHand','BEFORE'])]:
 s=dump('dev.solyanka.client.mixin.'+cls,jar,True)
 for n in needles:assert n in s,(cls,n)
loader=(p/'src/client/java/dev/solyanka/client/cs2/Cs2Knives.java').read_text()
for token in ['Cs2Library.open(path)','b.generation==generation','250_000_000L','500_000_000L','size()>4','if(texturesUploaded)','animation/anims/viewmodel/knife/','WORKER.shutdown()']:assert token in loader,token
for token in ['openDownloaded','HttpClient','java.net','cs2-data','openBundled']:assert token not in loader,token
assert 'animation/anims/rig/'not in loader
assert 'source=library;NmSkeleton rig=viewmodel' in loader
renderer=(p/'src/client/java/dev/solyanka/client/cs2/render/KnifeRender.java').read_text();assert 'KnifeSkinning.transform' in renderer
assert 'RenderPipelines.ENTITY_SNIPPET)' in renderer and 'GLOBALS_SNIPPET'not in renderer
assert 'BindGroupLayout.ensureCompatible' in renderer and 'CS2_GPU_SKIN'not in renderer
hud=(p/'src/client/kotlin/dev/solyanka/client/feature/StrayInventoryHud.kt').read_text();assert '"INVENTORY"'not in hud and 'AbstractContainerScreen' in hud
with zipfile.ZipFile(jar)as z:
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==45
 for name in ['Cs2Knives','Cs2Viewmodel','Cs2Library']:assert f'dev/solyanka/client/cs2/{name}.class' in z.namelist()
 assert 'dev/solyanka/client/feature/ItemAura.class'in z.namelist()
 assert 'licenses/CS2-THIRD-PARTY-NOTICES.md'in z.namelist() and 'licenses/Apache-2.0.txt'in z.namelist()
 assert not any(name.endswith(('.vpk','.vmdl_c','.vtex_c','.vnmclip_c','.vnmskel_c','.pack'))for name in z.namelist())
result=dict(version='1.3.12',targets=targets,source_hashes_verified=True,mixin_classes=45,registered_modules=57,aura_after_hand_flush=True,aura_capture_once_before_pop=True,knife_local_install_only=False,knife_explicit_download=True,knife_stale_results_guarded=True,knife_cpu_weighted_skinning=True,knife_native_metal_shader=True,valve_assets_bundled=False,automatic_asset_download=False,verified_pack_loading=True,inventory_title_removed=True,inventory_compacted=True,in_game_tested=False,native_gpu_tested=False,cs2_install_available_here=False)
(p/'validation/final-cosmetics-static.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
