from pathlib import Path
import json, shutil, hashlib, zipfile, xml.etree.ElementTree as ET,re
p=Path('/data/solyanka');v=p/'validation';version='0.7.0-dev'
log=Path('/data/world-work/final-clean-build.log');assert 'BUILD SUCCESSFUL' in log.read_text();shutil.copy2(log,v/'clean-build.log')
for f in v.glob('TEST-*.xml'):f.unlink()
roots=[]
for f in (p/'build/test-results/test').glob('TEST-*.xml'):
 roots.append(ET.parse(f).getroot());shutil.copy2(f,v/f.name)
totals={k:sum(int(x.attrib.get(k,0)) for x in roots) for k in ['tests','failures','errors','skipped']};assert totals==dict(tests=296,failures=0,errors=0,skipped=0),totals
for src,dst in [('hook-check.log','hook-check.log'),('world-hook-check.log','world-hook-check.log')]:shutil.copy2(Path('/data/world-work')/src,v/dst)
assert 'new target methods verified: 34' in (v/'hook-check.log').read_text()
world=json.loads((v/'world-visual-static.json').read_text());assert world['compiled_outline_callback_null_checked'];assert world['mixin_classes']==34
ui=p/'src/client/kotlin/dev/solyanka/client/ui';kit=(ui/'nofrills/GuiKit.kt').read_text();panel=(ui/'nofrills/SettingsPanel.kt').read_text();screen=(ui/'nofrills/NoFrillsClickGui.kt').read_text();client=(p/'src/client/kotlin/dev/solyanka/client/SolyankaClient.kt').read_text()
checks=dict(settings_use_createWithoutScreen='createWithoutScreen(' in panel,settings_navigation_calls_setScreen='setScreen' in panel,settings_root_transparent='root.surface(' not in panel,main_root_transparent='root.surface(' not in screen,swatch_and_embedded_palette='container.child(picker)' in kit,bounded_sliders='SlimSliderComponent(' in kit,explicit_right_click_previous='if(click.button()==1) {cycle(-1);return true}' in kit,success_status_removed='Changes save instantly.' not in panel,hud_editor_command_deferred='literal("gui").executes { openHudRequested=true;1 }' in client,new_world_settings_embedded='host.showPanel(SettingsPanel(' in (ui/'WorldVisualUi.kt').read_text(),new_world_settings_typed_instant_save='model.updateGeneral' in (ui/'WorldVisualUi.kt').read_text(),in_game_tested_here=False)
for k,value in checks.items():
 if k not in ['settings_navigation_calls_setScreen','in_game_tested_here']:assert value,k
assert not checks['settings_navigation_calls_setScreen']
(v/'instant-ui-static.json').write_text(json.dumps(checks,indent=2)+'\n')
scope=json.loads((p/'audit/scope.json').read_text());assert scope['excluded_features'][0]['implement']==False
jar=p/f'build/libs/solyanka-{version}.jar';sources=p/f'build/libs/solyanka-{version}-sources.jar'
def sha(f):return hashlib.sha256(f.read_bytes()).hexdigest()
with zipfile.ZipFile(jar) as z:
 assert json.loads(z.read('fabric.mod.json'))['version']==version
 names=z.namelist()
 for name in ['LICENSE','NOTICE','licenses/Odin-BSD-3-Clause.txt','licenses/Stray-CC0.txt','dev/solyanka/client/feature/WorldVisuals.class','dev/solyanka/client/feature/TitaniumScanner.class','dev/solyanka/client/feature/BlockSelection.class','dev/solyanka/client/mixin/VisualEntityStateMixin.class','dev/solyanka/client/mixin/VisualEntityRendererMixin.class','dev/solyanka/client/mixin/PlayerGlowMinecraftMixin.class','dev/solyanka/client/ui/WorldVisualUi.class','dev/solyanka/utility/TitaniumLogic.class','assets/solyanka/mining/titanium-regions.json']:assert name in names,name
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==34
 assert not any('AetherOS' in name for name in names)
 assert z.read('assets/solyanka/mining/titanium-regions.json')==(p/'src/main/resources/assets/solyanka/mining/titanium-regions.json').read_bytes()
with zipfile.ZipFile(sources) as z:
 for root in [p/'src/main/kotlin',p/'src/client/kotlin']:
  for source in root.rglob('*.kt'):assert z.read(source.relative_to(root).as_posix())==source.read_bytes(),source
versions=json.loads((v/'new-mixin-targets.json').read_text())
unique={(t['class'],t['method'],t['descriptor']) for t in versions}
unique.update((t['class'],t['method'],t['descriptor']) for t in json.loads((v/'experience-bar-static.json').read_text())['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in world['targets'])
rows=json.loads((p/'audit/features.json').read_text());remaining=[r['feature'] for r in rows if r['source']=='Stray' and r['status']=='AUDITED_ENTRY_POINTS_NOT_PORTED'];assert len(remaining)==7,remaining
summary=dict(version=version,minecraft='26.2',build='SUCCESS',tests=totals,registered_modules=33,new_modules=['render.player_glow','render.nametags','render.block_outline','mining.titanium'],mixin_classes=34,unique_target_methods_rechecked=len(unique),world_target_methods_checked=4,compiled_nullable_outline_guard=True,instant_save=True,settings_embedded_in_same_screen=True,hud_editor_elements=17,hide_xp_level_and_bar_independent=True,aetheros_settings_excluded=True,remaining_stray_features=remaining,remaining_archive_feature_objects=21,in_game_tested_here=False,mixin_application_tested_here=False,gpu_sodium_iris_tested_here=False,full_requested_feature_set_complete=False,source_blur_parity=False,jar_sha256=sha(jar),sources_jar_sha256=sha(sources),baseline_note='Older unchanged ABI/AW evidence retained; clean Gradle AW check and current descriptor checks. Static checks do not establish Mixin application or rendering compatibility. See docs/M6.md.')
(v/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
dist=p/'dist'
for f in dist.iterdir():
 if f.is_file():f.unlink()
for f in [jar,sources]:shutil.copy2(f,dist/f.name)
(dist/'SHA256SUMS').write_text(''.join(f'{sha(f)}  {f.name}\n' for f in [jar,sources]))
# Bundle this exact packaging script for reproducibility; hook checks run separately.
shutil.copy2(Path(__file__),v/'package-world-release.py')
out=Path('/data/world-work/solyanka-0.7.0-mc26.2-source.zip')
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted(p.rglob('*')):
  rel=f.relative_to(p)
  if f.is_file() and rel.parts[0] not in {'.gradle','.kotlin','build','.git','run'}:z.write(f,'solyanka/'+rel.as_posix())
with zipfile.ZipFile(out) as z:
 assert z.testzip() is None
 assert z.read('solyanka/dist/'+jar.name)==jar.read_bytes()
 assert json.loads(z.read('solyanka/audit/scope.json'))['excluded_features'][0]['implement']==False
 assert json.loads(z.read('solyanka/validation/summary.json'))['version']==version
print(json.dumps(dict(build='SUCCESS',tests=totals,unique_target_methods_rechecked=len(unique),remaining_stray=remaining,jar_bytes=jar.stat().st_size,source_zip_bytes=out.stat().st_size,jar_sha256=sha(jar)),indent=2))
