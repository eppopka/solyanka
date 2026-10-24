"""Verify the reported dependency callsite and the compiled local null-style guard."""
from pathlib import Path
import subprocess,re,json,hashlib
p=Path(__file__).resolve().parents[1];cache=Path.home()/'.gradle/caches'
owo=next(cache.glob('modules-2/files-2.1/maven.modrinth/owo-lib/0.13.1+26.2/*/*.jar'))
jar=p/'build/libs/solyanka-1.3.12.jar'
def dump(cls,cp):return subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-c','-s','-classpath',str(cp),cls],text=True)
lib=dump('io.wispforest.owo.ui.component.LabelComponent',owo)
block=re.search(r'public void drawTooltip\(.*?(?=\n  (?:public|private|protected)|\Z)',lib,re.S).group()
assert 'styleAt:(II)Lnet/minecraft/network/chat/Style;' in block
assert 'componentHoverEffect:(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Style;II)V' in block
assert 'ifnull'not in block and 'ifnonnull'not in block
local=dump('dev.solyanka.client.ui.nofrills.PlainLabel',jar)
method=re.search(r'(?:public|protected) net.minecraft.network.chat.Style styleAt\(.*?(?=\n  (?:public|private|protected)|\Z)',local,re.S).group()
assert 'descriptor: (II)Lnet/minecraft/network/chat/Style;' in method
assert 'LabelComponent.styleAt:(II)Lnet/minecraft/network/chat/Style;' in method
assert 'ifnonnull' in method and 'Style.EMPTY:Lnet/minecraft/network/chat/Style;' in method
assert method.index('ifnonnull')<method.index('Style.EMPTY:')<method.index('areturn')
assert 'drawTooltip('not in local # inherited tooltip behavior is retained
result=dict(version='1.3.12',reported_version='1.3.0',dependency_version='owo 0.13.1+26.2',dependency_unguarded_style_call_confirmed=True,compiled_local_fallback_to_empty_style=True,non_null_style_retained=True,normal_tooltip_rendering_inherited=True,global_mixin_added=False,crash_report_received=True,reported_null_path_addressed=True,live_crash_reproduced=False,in_game_fix_confirmed=False,raw_user_report_bundled=False)
(p/'validation/tooltip-null-guard-static.json').write_text(json.dumps(result,indent=2)+'\n')
print('Confirmed owo unguarded nullable lookup; compiled PlainLabel substitutes EMPTY only on null. Live in-game confirmation pending.')
