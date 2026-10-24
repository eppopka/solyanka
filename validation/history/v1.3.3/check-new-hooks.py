from pathlib import Path
import re,subprocess,json
p=Path(__file__).resolve().parents[1];cp=':'.join(str(x) for x in Path.home().glob('.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/*/26.2/*.jar'));results=[]
cache={}
def javap(cls):
 if cls not in cache:cache[cls]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c','-classpath',cp,cls],text=True)
 return cache[cls]
def method(cls,name):
 text=javap(cls);m=re.search(r'^  (?:public|private|protected)[^\n]*\b'+re.escape(name)+r'\([^\n]*\);(.*?)(?=\n  (?:public|private|protected)|\Z)',text,re.M|re.S);assert m,(cls,name);return m.group()
checks=[('net.minecraft.client.gui.screens.inventory.AbstractContainerScreen',m) for m in ['extractSlot','slotClicked','extractTooltip']]+[('net.minecraft.client.gui.Hud',m) for m in ['extractBossOverlay','extractEffects','extractSelectedItemName','extractConfusionOverlay','extractVignette']]+[('net.minecraft.client.renderer.extract.LevelExtractor','isEntityVisible'),('net.minecraft.client.renderer.entity.EntityRenderer','extractRenderState'),('net.minecraft.client.gui.screens.inventory.EffectsInInventory','extractRenderState'),('net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen','initButton')]+[('net.minecraft.client.multiplayer.ClientLevel',m) for m in ['addBreakingBlockEffect','addDestroyBlockEffect']]+[('net.minecraft.client.multiplayer.ClientPacketListener',m) for m in ['handleParticleEvent','handleContainerSetSlot','handleContainerContent']]+[('net.minecraft.client.renderer.ScreenEffectRenderer','submit'),('net.minecraft.client.renderer.entity.layers.StuckInBodyLayer','submit'),('net.minecraft.client.renderer.GameRenderer','renderLevel'),('net.minecraft.client.renderer.fog.FogRenderer','setupFog'),('net.minecraft.client.renderer.entity.EntityRenderer','extractNameTags')]+[('net.minecraft.client.multiplayer.ClientPacketListener',m) for m in ['handleEntityEvent','handleMovePlayer','handleTakeItemEntity','handleRemoveEntities','handleSoundEvent']]+[('net.minecraft.client.ClientClockManager','getTotalTicks'),('net.minecraft.world.level.Level','getRainLevel'),('net.minecraft.world.level.Level','getThunderLevel'),('net.minecraft.client.renderer.WeatherEffectRenderer','extractRenderState'),('net.minecraft.client.renderer.WeatherEffectRenderer','render'),('net.minecraft.client.renderer.Projection','setupPerspective'),('net.minecraft.client.Camera','createProjectionMatrixForCulling')]
for cls,m in checks:body=method(cls,m);results.append({'class':cls,'method':m,'descriptor':re.search(r'descriptor: (.*)',body).group(1),'target_exists':True})
nausea=method('net.minecraft.client.renderer.GameRenderer','renderLevel')
for needle in ['LocalPlayer.oPortalEffectIntensity:F','LocalPlayer.portalEffectIntensity:F','LocalPlayer.getEffectBlendFactor:(Lnet/minecraft/core/Holder;F)F']:assert nausea.count(needle)==1
fire=method('net.minecraft.client.renderer.ScreenEffectRenderer','submit');assert fire.count('Method submitFire:')==1
vignette=method('net.minecraft.client.gui.Hud','extractVignette');assert vignette.count('GuiGraphicsExtractor.blit:(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V')==1
particle=method('net.minecraft.client.multiplayer.ClientPacketListener','handleParticleEvent');assert particle.index('PacketUtils.ensureRunningOnSameThread:')<particle.index('ClientboundLevelParticlesPacket.getCount:()I')
for m in ['handleContainerSetSlot','handleContainerContent']:assert 'PacketUtils.ensureRunningOnSameThread:' in method('net.minecraft.client.multiplayer.ClientPacketListener',m)
for m in ['handleEntityEvent','handleMovePlayer','handleSoundEvent']:assert 'PacketUtils.ensureRunningOnSameThread:' in method('net.minecraft.client.multiplayer.ClientPacketListener',m)
for m,target in [('handleTakeItemEntity','ClientboundTakeItemEntityPacket.getItemId:()I'),('handleRemoveEntities','ClientboundRemoveEntitiesPacket.getEntityIds:()Lit/unimi/dsi/fastutil/ints/IntList;')]:
 body=method('net.minecraft.client.multiplayer.ClientPacketListener',m);assert body.index('PacketUtils.ensureRunningOnSameThread:')<body.index(target)
weather=method('net.minecraft.client.renderer.WeatherEffectRenderer','render');assert weather.count('WeatherRenderState.intensity:F')==2
snow=method('net.minecraft.client.renderer.WeatherEffectRenderer','extractRenderState');assert snow.count('ClientLevel.getPrecipitationAt:')==1
aspect=method('net.minecraft.client.Camera','createProjectionMatrixForCulling');assert aspect.count('Matrix4f.perspective:(FFFFZ)')==1
jar=p/'build/libs/solyanka-1.3.3.jar';reg=json.loads(next((p/'src').rglob('solyanka.client.mixins.json')).read_text())['client'];print('Mixin classes:',len(reg),'new target methods verified:',len(results))
(p/'validation/new-mixin-targets.json').write_text(json.dumps(results,indent=2)+'\n')
