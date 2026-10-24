"""Use native MC 26.2 metadata validator; no GPU/window or Fabric launch."""
from pathlib import Path
import subprocess,tempfile,json
p=Path(__file__).resolve().parents[1]
home=Path.home()/'.gradle/caches'
jars=[home/'fabric-loom/26.2/minecraft-client.jar',*sorted((home/'modules-2/files-2.1').rglob('*.jar'))]
source=(p/'src/client/kotlin/dev/solyanka/client/feature/HeldItemCosmetic.kt').read_text()
assert 'RenderPipeline.builder(RenderPipelines.ITEM_SNIPPET)' in source
assert 'RenderPipelines.ITEM_SNIPPET,RenderPipelines.GLOBALS_SNIPPET' not in source
with tempfile.TemporaryDirectory() as tmp:
 w=Path(tmp);cp=':'.join(map(str,jars))
 (w/'compile.args').write_text('-cp\n"'+cp+'"\n-d\n"'+tmp+'"\n"'+str(p/'validation/CheckBindings.java')+'"')
 (w/'run.args').write_text('-cp\n"'+tmp+':'+cp+'"\nCheckBindings')
 subprocess.run(['javac','@'+str(w/'compile.args')],check=True,capture_output=True,text=True)
 out=subprocess.check_output(['java','@'+str(w/'run.args')],text=True)
 assert 'Fixed native layouts accepted: 6' in out
 assert "Old layout reproduced: Duplicate bind name 'Globals'" in out
 assert "Knife native layouts accepted; samplers=[Sampler0, Sampler2, Sampler3]" in out
 assert "Aura native layouts accepted" in out
 print(out)
 (p/'validation/native-item-layout.json').write_text(json.dumps(dict(version='1.3.12',old_duplicate_reproduced=True,corrected_layout_accepted=True,knife_and_aura_layouts_accepted=True,native_bind_group_validator_used=True,black_screen_log_root_cause_confirmed=True,in_game_startup_verified=False,gpu_compilation_tested=False),indent=2)+'\n')
