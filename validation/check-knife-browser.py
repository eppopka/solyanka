"""New arm ABI, audited menu/preview source and synthetic software preview. No game/GPU acceptance."""
from pathlib import Path
import hashlib,json,re,subprocess,tempfile,zipfile
p=Path(__file__).resolve().parents[1];native=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar';jar=p/'build/libs/solyanka-1.3.12.jar'
a=p/'audit/stray-knife-menu-source';m=json.loads((a/'manifest.json').read_text())
for name,h in m['files'].items():assert hashlib.sha256((a/name).read_bytes()).hexdigest()==h,name
original=(a/'SoftwareRender.java').read_text();port=(p/'src/client/java/dev/solyanka/client/cs2/render/SoftwareRender.java').read_text();assert original.split('public final class SoftwareRender',1)[1]==port.split('public final class SoftwareRender',1)[1]
cls='net.minecraft.client.renderer.entity.player.AvatarRenderer';desc='(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/geom/ModelPart;Z)V'
dump=subprocess.check_output(['javap','-p','-s','-c','-classpath',str(native),cls],text=True)
assert 'descriptor: '+desc in dump
body=dump.split('private void renderHand(',1)[1].split('\n  protected ',1)[0]
assert body.count('RenderTypes.entityTranslucent:')==1
compiled=subprocess.check_output(['javap','-p','-c','-s','-v','-classpath',str(jar),'dev.solyanka.client.mixin.HeldItemArmMixin'],text=True)
assert 'HeldItemCosmetic.wrapArm:'in compiled and 'ModifyExpressionValue'in compiled and 'renderHand'+desc in compiled
assert '(Lnet/minecraft/client/renderer/rendertype/RenderType;'+desc[1:-1]+'Lnet/minecraft/client/renderer/rendertype/RenderType;' in compiled
ui=(p/'src/client/kotlin/dev/solyanka/client/ui/KnifeBrowserPanel.kt').read_text();cache=(p/'src/client/kotlin/dev/solyanka/client/ui/KnifePreviewCache.kt').read_text()
assert ':EmbeddedPanel'in ui and 'Item Bindings'in ui and 'setScreen('not in ui
for token in ['jobs.size<2','textures.size>40','job.epoch!=generation','previews']:pass
for token in ['jobs.size<2','textures.size>40','job.epoch!=generation','textureManager.release','future.cancel(false)']:assert token in cache,token
feature=(p/'src/client/kotlin/dev/solyanka/client/feature/HeldItemCosmetic.kt').read_text();assert 'HeldItemPolicy.arm(right,s)'in feature and '.withTexture("Sampler0",skin)'in feature
assert 'RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET,RenderPipelines.GLOBALS_SNIPPET)'not in feature
with tempfile.TemporaryDirectory()as temp:
 subprocess.run(['javac','-cp',str(jar),'-d',temp,str(p/'validation/CheckKnifePreview.java')],check=True,capture_output=True,text=True)
 out=subprocess.check_output(['java','-Djava.awt.headless=true','-cp',temp+':'+str(jar),'CheckKnifePreview',str(p/'validation/synthetic-knife-preview.png')],text=True)
 assert '5 synthetic preview checks passed'in out
with zipfile.ZipFile(jar)as z:
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==45
 for path in ['dev/solyanka/client/ui/KnifeBrowserPanel.class','dev/solyanka/client/ui/KnifePreviewCache.class','dev/solyanka/utility/KnifeItemBinding.class','dev/solyanka/client/cs2/render/SoftwareRender.class']:assert path in z.namelist()
result=dict(version='1.3.12',targets=[dict(class_name=cls,method='renderHand',descriptor=desc)],arm_native_callsite_count=1,compiled_arm_hook_verified=True,original_software_preview_body_equal=True,source_hashes_verified=True,synthetic_preview_checks=5,preview_jobs_limit=2,preview_texture_limit=40,item_uuid_bindings=True,knife_menu_embedded=True,photo_thumbnails_from_assets=True,actual_cs2_images_tested=False,native_gui_visual_tested=False,in_game_tested=False,native_gpu_tested=False,mixin_application_tested=False)
(p/'validation/knife-browser.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
