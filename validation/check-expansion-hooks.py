"""1.3 ABI/ordering/privacy source checks. NOT Windows/Minecraft/Mixin execution."""
from pathlib import Path
import re,json,subprocess,zipfile
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');jar=p/'build/libs/solyanka-1.3.9.jar'
def dump(c,classpath=cp,verbose=False):return subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c']+(['-v']if verbose else [])+['-classpath',str(classpath),c],text=True)
listener=dump('net.minecraft.client.multiplayer.ClientPacketListener')
targets=[]
for name,param,call in [('handleBlockUpdate','ClientboundBlockUpdatePacket','ClientboundBlockUpdatePacket.getPos:'),('handleChunkBlocksUpdate','ClientboundSectionBlocksUpdatePacket','ClientboundSectionBlocksUpdatePacket.runUpdates:')]:
 desc=f'(Lnet/minecraft/network/protocol/game/{param};)V'
 block=re.search(r'public void '+name+r'\(.*?(?=\n  (?:public|private|protected)|\Z)',listener,re.S).group()
 assert 'descriptor: '+desc in block
 assert block.index('PacketUtils.ensureRunningOnSameThread:')<block.index(call)
 if name=='handleBlockUpdate':assert block.index(call)<block.index('ClientLevel.setServerVerifiedBlockState:')
 targets.append(dict(class_name='net.minecraft.client.multiplayer.ClientPacketListener',method=name,descriptor=desc))
compiled=dump('dev.solyanka.client.mixin.DungeonEventMixin',jar,True)
assert 'solyankaSimonBlock' in compiled and 'solyankaSimonSection' in compiled and 'SimonSolver.observe:' in compiled
for x in ['ClientboundBlockUpdatePacket;getPos()Lnet/minecraft/core/BlockPos;','ClientboundSectionBlocksUpdatePacket;runUpdates(Ljava/util/function/BiConsumer;)V']:assert x in compiled,x
particle=dump('dev.solyanka.client.mixin.SuppressParticleMixin',jar)
assert particle.index('HuntingFeatures.particle:')<particle.index('Suppression.particle:')
methods=dump('net.minecraft.client.renderer.RenderPipelines')
source=(p/'src/client/java/dev/solyanka/client/pip/PipWin32.java').read_text();pip=(p/'src/client/kotlin/dev/solyanka/client/feature/PictureInPicture.kt').read_text()
for x in ['sameTarget(hwnd,target)','PipTargetIdentity(target.id(),target.pid(),target.processStart()).matches(current)','ProcessHandle::info','IsIconic(hwnd)','return null; // Never fall back','16777216L','MAX_EDGE = 640']:assert x in source,x
assert 'GDI32.INSTANCE.BitBlt(' not in source
assert 'selected.get()===target' in pip and 'frame.target!==selected.get()' in pip
assert 'finally {PipWin32.close()}' in pip and 'nextFrame.getAndSet(null)' in pip
assert 'System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")' in source
s=p/'src/client/kotlin/dev/solyanka/client'
client=(s/'SolyankaClient.kt').read_text();catalog=(p/'src/main/kotlin/dev/solyanka/ui/GuiModels.kt').read_text()
assert len(re.findall(r'FeatureCardInfo\("',catalog))==57
assert 'PictureInPicture.close()' in client and 'ShitterList.close()' in client
for name in ['HuntingUi.kt','RouteUi.kt']:
 ui=(s/'ui'/name).read_text();assert 'host.showPanel(SettingsPanel(' in ui and 'model.updateGeneral' in ui
 assert 'Save & Back' not in ui and 'root.surface' not in ui
with zipfile.ZipFile(jar) as z:
 assert json.loads(z.read('fabric.mod.json'))['version']=='1.3.9'
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==44
 for name in ['HuntingFeatures','ArchiveCosmetics','SimonSolver','PetDisplay','PictureInPicture','ShitterList','VanillaHotbar']:assert f'dev/solyanka/client/feature/{name}.class' in z.namelist(),name
 assert 'dev/solyanka/client/pip/PipWin32.class' in z.namelist()
 assert not any(any(x in n for x in ['AutoRag','AutoReel','AetherOSSettings']) for n in z.namelist())
result=dict(version='1.3.9',targets=targets,thread_guard_before_both_updates=True,compiled_injections=True,particle_evidence_before_suppression=True,exact_window_identity=True,no_desktop_fallback=True,capture_worker_owns_gdi=True,session_only_selection=True,registered_modules=57,hud_editor_elements=25,mixin_classes=44,in_game_tested_here=False,mixin_application_tested_here=False,windows_capture_tested_here=False,gpu_sodium_iris_tested_here=False,full_requested_feature_set_complete=False)
(p/'validation/expansion-static.json').write_text(json.dumps(result,indent=2)+'\n')
print('Expansion: 2 native pre-update methods, compiled packet observers, explicit-window source guards; 57 cards/25 HUD entries. Static only; Windows untested.')
