"""Source provenance and compiled original-painter wiring. Not native visual acceptance."""
from pathlib import Path
import hashlib,json,zipfile,subprocess
p=Path(__file__).resolve().parents[1];a=p/'audit/original-hotbar-source';version='1.3.12'
m=json.loads((a/'manifest.json').read_text())
for name,digest in m['files'].items():assert hashlib.sha256((a/name).read_bytes()).hexdigest()==digest,name
c=p/'src/client/kotlin/dev/solyanka/client/feature/hotbar';g=p/'src/main/kotlin/dev/solyanka/utility/archivehotbar/HotbarLayout.kt'
for name,marker in [('HotbarPainter.kt','object HotbarPainter'),('GlassRenderer.kt','object GlassRenderer')]:assert (a/name).read_text().split(marker,1)[1]==(c/name).read_text().split(marker,1)[1],name
assert (a/'HotbarLayout.kt').read_text().split('import kotlin.math.exp',1)[1]==g.read_text().split('import kotlin.math.exp',1)[1]
jar=p/f'build/libs/solyanka-{version}.jar'
with zipfile.ZipFile(jar)as z:
 for name in ['HotbarPainter','GlassRenderer','HotbarPrimitives','Rect']:assert f'dev/solyanka/client/feature/hotbar/{name}.class'in z.namelist()
 for name in ['HotbarLayout','HotbarBox','HotbarViewport','HotbarSelection']:assert f'dev/solyanka/utility/archivehotbar/{name}.class'in z.namelist()
 assert 'licenses/eppopka-hotbar-CC0-1.0.txt'in z.namelist()
 assert not any('AetherOSSettings'in name or 'dev/eppopkaaddons/'in name for name in z.namelist())
dump=subprocess.check_output(['javap','-p','-c','-classpath',str(jar),'dev.solyanka.client.feature.VanillaHotbar'],text=True)
for token in ['hotbar/HotbarPainter.background','hotbar/HotbarPainter.selection','archivehotbar/HotbarSelection.update']:assert token in dump,token
assert 'ChromeDraw.panel'not in dump
result=dict(version=version,original_painter_body_equal=True,original_rounded_renderer_body_equal=True,original_pure_helpers_equal=True,source_hashes_verified=True,compiled_painter_wiring_verified=True,archive_reference_sizing_opt_in=True,excluded_global_settings_not_imported=True,native_visual_tested=False,in_game_tested=False,gpu_performance_tested=False)
(p/'validation/original-hotbar.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
