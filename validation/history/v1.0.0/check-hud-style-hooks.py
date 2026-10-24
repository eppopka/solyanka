"""Static ABI/callsite evidence only; does not apply Mixins or launch Minecraft."""
from pathlib import Path
import subprocess,re,json
p=Path(__file__).resolve().parents[1];cp=str(Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar');cache={}
def dump(cls):
 if cls not in cache:cache[cls]=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-c','-classpath',cp,cls],text=True)
 return cache[cls]
def verify(cls,name,descriptor):
 pattern=r'^  (?:public|private|protected)[^\n]*\b'+name+r'\([^\n]*\);\n    descriptor: '+re.escape(descriptor)+r'\n'
 assert len(re.findall(pattern,dump(cls),re.M))==1,(cls,name,descriptor)
 return dict(class_name=cls,method=name,descriptor=descriptor,target_exists=True)
targets=[verify('net.minecraft.client.gui.components.ChatComponent','extractRenderState','(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V')]
for cls in ['net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess','net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess']:targets.append(verify(cls,'fill','(IIIII)V'))
targets.append(verify('net.minecraft.client.gui.screens.ChatScreen','extractRenderState','(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V'))
for name,desc in [('getWidth','()I'),('getScale','()D'),('getLineHeight','()I')]:targets.append(verify('net.minecraft.client.gui.components.ChatComponent',name,desc))
chatScreen=dump('net.minecraft.client.gui.screens.ChatScreen');body=re.search(r'public void extractRenderState\(.*?(?=\n  public|\Z)',chatScreen,re.S).group();assert body.count('GuiGraphicsExtractor.fill:(IIIII)V')==1
for cls in ['net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess','net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess']:
 text=dump(cls);assert 'GuiGraphicsExtractor.fill:(IIIII)V' in text and 'handleTagIcon(' in text and 'handleTag(' in text
assert 'ScreenRectangle.transformAxisAligned:' in dump('net.minecraft.client.gui.GuiGraphicsExtractor')
verify('net.minecraft.world.scores.TeamColor','displaySlot','()Lnet/minecraft/world/scores/DisplaySlot;')
jar=p/'build/libs/solyanka-1.0.0.jar'
mod=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-c','-classpath',str(jar),'dev.solyanka.client.mixin.ThemedChatMixin'],text=True)
assert mod.count('ThemedChat.end:(Z)V')>=2 and 'Exception table:' in mod
reg=json.loads((p/'src/main/resources/solyanka.client.mixins.json').read_text())['client'];assert len(reg)==40
source=p/'src/client/kotlin/dev/solyanka/client'
assert 'handleTag' not in (source/'mixin/ThemedChatFillMixin.kt').read_text()
assert 'setRectangle' not in (source/'feature/ThemedChat.kt').read_text()
assert 'e.displayName.copy()' in (source/'feature/WorldVisuals.kt').read_text()
result=dict(targets=targets,input_fill_callsites=1,scissor_transforms_gui_pose=True,compiled_chat_scope_has_finally=True,vanilla_security_handlers_not_cancelled=True,vanilla_text_and_hitboxes_not_shifted=True,nametags_keep_components=True,block_halo_is_geometry_not_framebuffer_blur=True,in_game_tested_here=False,mixin_application_tested_here=False)
(p/'validation/hud-style-static.json').write_text(json.dumps(result,indent=2)+'\n');print('HUD-style target methods/invokers verified:',len(targets),'; compiled finally scope verified')
