"""1.2 native weather and message API checks. Does not launch Minecraft/apply Mixins."""
from pathlib import Path
import json,re,subprocess,zipfile
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');jar=str(p/'build/libs/solyanka-1.3.0.jar');cache={}
def dump(cls,classpath=cp,verbose=False):
 key=(cls,classpath,verbose)
 if key not in cache:cache[key]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c']+(['-v'] if verbose else [])+['-classpath',classpath,cls],text=True)
 return cache[key]
def verify(cls,name,desc,classpath=cp):
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);\n    descriptor: '+re.escape(desc)+r'\n'
 assert len(re.findall(pattern,dump(cls,classpath),re.M))==1,(cls,name,desc)
 return dict(class_name=cls,method=name,descriptor=desc,target_exists=True)
weather='net.minecraft.client.renderer.WeatherEffectRenderer';desc='(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V'
targets=[verify(weather,'extractRenderState',desc),verify('net.minecraft.world.level.Level','getRainLevel','(F)F')]
text=dump(weather);method=re.search(r'public void extractRenderState\(.*?(?=\n  (?:public|private|protected)|\Z)',text,re.S).group()
height='ClientLevel.getHeight:(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I'
assert method.count(height)==1
assert method.count('ClientLevel.getPrecipitationAt:')==1 and method.count('ClientLevel.getRainLevel:(F)F')==1
assert method.count('Method java/lang/Math.max:(II)I')==3
assert method.index(height)<method.index('Method java/lang/Math.max:(II)I')<method.index('LightCoordsUtil.getLightCoords:')
compiled=dump('dev.solyanka.client.mixin.AmbiencePrecipitationMixin',jar,True)
for token in ['solyankaIndoorRain(int);','descriptor: (I)I','AlwaysWaterPolicy.roofHeight:(ZI)I','getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I','org.spongepowered.asm.mixin.injection.At']:
 assert token in compiled,token
rain=dump('dev.solyanka.client.mixin.AmbienceWeatherMixin',jar,True)
assert 'AlwaysWaterPolicy.rain:(ZF)F' in rain and 'Environment.alwaysWater:()Z' in rain
messagejar=str(next(Path.home().glob('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-message-api-v1/*/*/fabric-message-api-v1-*.jar')))
apis=[verify('net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents$Game','onReceiveGameMessage','(Lnet/minecraft/network/chat/Component;Z)V',messagejar),verify('net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents$GameCanceled','onReceiveGameMessageCanceled','(Lnet/minecraft/network/chat/Component;Z)V',messagejar),verify('net.minecraft.client.multiplayer.ClientPacketListener','sendCommand','(Ljava/lang/String;)V'),verify('net.minecraft.client.multiplayer.ClientPacketListener','sendChat','(Ljava/lang/String;)V')]
flow=dump('net.fabricmc.fabric.mixin.client.message.ChatListenerMixin',messagejar)
branch=re.search(r'private void fabric_allowSystemMessage\(.*?(?=\n  (?:public|private|protected)|\Z)',flow,re.S).group()
assert 'ClientReceiveMessageEvents.GAME:' in branch and 'ClientReceiveMessageEvents.GAME_CANCELED:' in branch and 'goto' in branch and 'CallbackInfo.cancel:()V' in branch
s=p/'src/client/kotlin/dev/solyanka/client';counter=(s/'feature/SecretCounter.kt').read_text();client=(s/'SolyankaClient.kt').read_text();ui=(s/'ui/ArchiveDungeonUi.kt').read_text()
for token in ['ClientReceiveMessageEvents.GAME.register','ClientReceiveMessageEvents.GAME_CANCELED.register','if(overlay || !active()) return','!ctx.dungeon','scope!=next','System.nanoTime()/1_000_000L','announcement.poll(now,s.autoAnnounce)','connection.sendCommand("showextrastats")','connection.sendCommand("pc $text")','connection.sendChat(text)']:assert token in counter,token
assert 'ClientReceiveMessageEvents.CHAT.register' not in counter and 'Thread(' not in counter
assert client.index('FeatureRuntime.tick(client);')<client.index('SecretCounter.tick();')
assert 'ModuleInfo(SECRET_COUNTER' in client and 'SecretCounter.reset()' in client
assert 'host.showPanel(SettingsPanel(' in ui and 'model.updateGeneral' in ui and 'setScreen' not in ui
assert '.isClientSide) {if(Environment.alwaysWater())' in (s/'mixin/AmbienceWeatherMixin.kt').read_text()
with zipfile.ZipFile(jar) as z:
 assert json.loads(z.read('fabric.mod.json'))['version']=='1.3.0';assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==41
 for name in ['dev/solyanka/client/feature/SecretCounter.class','dev/solyanka/client/ui/ArchiveDungeonUi.class','dev/solyanka/utility/SecretCounterState.class','dev/solyanka/utility/SecretAnnouncement.class','dev/solyanka/utility/AlwaysWaterPolicy.class','licenses/NoammAddons-CC0.txt']:assert name in z.namelist(),name
 assert not any('AetherOS' in n for n in z.namelist())
archive=json.loads((p/'audit/archive-inventory.json').read_text());manifest=json.loads((p/'audit/archive-dungeon-source-manifest.json').read_text());assert len(manifest)==3
for path in ['impl/dungeon/SecretCounter.kt','impl/visual/AlwaysWater.kt']:
 a=next(r for r in archive if r['file']==path);m=next(r for r in manifest if r['file']==path);assert a['sha256']==m['sha256'] and a['module']==m['module'];assert a['status']=='IMPLEMENTED_ADAPTED_BUILD_TESTED_INGAME_PENDING'
result=dict(version='1.3.0',targets=targets,api_targets=apis,native_roof_height_callsites=1,native_precipitation_callsites=1,native_height_uses_clamped_against_camera=True,compiled_indoor_rain_handler_verified=True,compiled_client_rain_policy_verified=True,server_world_guard_preserved=True,existing_ambience_opacity_preserved=True,game_and_canceled_message_paths_verified=True,player_chat_not_observed=True,fixed_stats_command=True,announcements_opt_in=True,pending_state_resets=True,embedded_instant_settings=True,mixin_classes=41,in_game_tested_here=False,mixin_application_tested_here=False,server_run_command_tested_here=False,gpu_sodium_iris_tested_here=False)
(p/'validation/archive-dungeon-static.json').write_text(json.dumps(result,indent=2)+'\n')
print('Archive dungeon/rain: 2 native targets, 1 roof callsite, 4 message/command API methods; compiled weather callbacks and fixed command gates checked; static only')
