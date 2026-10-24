from pathlib import Path
import json,hashlib,shutil,re,zipfile,xml.etree.ElementTree as ET,argparse
p=Path('/data/solyanka');v=p/'validation';work=Path('/data/v133-work');version='1.3.3'
def sha(f):return hashlib.sha256(f.read_bytes()).hexdigest()
log=work/'clean-build.log';assert 'BUILD SUCCESSFUL' in log.read_text();assert 'FAILED' not in log.read_text();shutil.copy2(log,v/'clean-build.log')
results=json.loads((work/'validation-run.log').read_text());assert len(results)==14 and all(code==0 for _,code in results),results
for f in v.glob('TEST-*.xml'):f.unlink()
roots=[]
for f in (p/'build/test-results/test').glob('TEST-*.xml'):
 roots.append(ET.parse(f).getroot());shutil.copy2(f,v/f.name)
totals={k:sum(int(x.attrib.get(k,0))for x in roots)for k in ['tests','failures','errors','skipped']};assert totals==dict(tests=540,failures=0,errors=0,skipped=0),totals
for name,code in results:shutil.copy2(work/(Path(name).stem+'.log'),v/(Path(name).stem+'.log'))
assert sha(p/'.gitignore')==(Path('/data/v132-work')/'gitignore.sha256').read_text().split()[0]
jar=p/f'build/libs/solyanka-{version}.jar';sources=p/f'build/libs/solyanka-{version}-sources.jar'
required=['HuntingFeatures','ArchiveCosmetics','SimonSolver','PetDisplay','PictureInPicture','ShitterList','VanillaHotbar','SecretCounter','TemporalBlur','SkyboxVisual','HeldItemCosmetic']
with zipfile.ZipFile(jar)as z:
 assert z.testzip()is None
 assert json.loads(z.read('fabric.mod.json'))['version']==version
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==41
 for name in required:assert f'dev/solyanka/client/feature/{name}.class'in z.namelist(),name
 assert 'dev/solyanka/client/pip/PipWin32.class'in z.namelist()
 for path in ['NOTICE','LICENSE','licenses/Odin-BSD-3-Clause.txt','licenses/Stray-CC0.txt','licenses/NoammAddons-CC0.txt']:assert z.read(path)==(p/path).read_bytes(),path
 for root in [p/'src/main/resources']:
  for f in root.rglob('*'):
   if f.is_file()and f.name!='fabric.mod.json':assert z.read(f.relative_to(root).as_posix())==f.read_bytes(),f
 assert not any(any(x in name for x in ['AetherOSSettings','AutoRag','AutoReel','Cs2Knives','ItemAura'])for name in z.namelist())
with zipfile.ZipFile(sources)as z:
 for root in [p/'src/main/kotlin',p/'src/client/kotlin',p/'src/client/java']:
  for f in root.rglob('*'):
   if f.is_file():assert z.read(f.relative_to(root).as_posix())==f.read_bytes(),f
catalog=(p/'src/main/kotlin/dev/solyanka/ui/GuiModels.kt').read_text();assert len(re.findall(r'FeatureCardInfo\("',catalog))==55
kit=(p/'src/client/kotlin/dev/solyanka/client/ui/nofrills/GuiKit.kt').read_text();panel=(p/'src/client/kotlin/dev/solyanka/client/ui/nofrills/SettingsPanel.kt').read_text();screen=(p/'src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt').read_text()
assert 'createWithoutScreen('in panel and 'root.surface('not in panel and 'root.surface('not in screen
assert 'container.child(palette)'in kit and 'SlimSliderComponent('in kit and 'if(click.button()==1) {cycle(-1);return true}'in kit
assert 'Changes save instantly.'not in panel
archive=json.loads((p/'audit/archive-inventory.json').read_text());features=[r for r in archive if r.get('feature')]
assert len(features)==23
assert sum(r['status']=='EXCLUDED_BY_USER'for r in features)==4
pending=[r['file']for r in features if r['status']=='PENDING_IMPLEMENTATION'];assert len(pending)==1 and pending[0].endswith('ItemAura.kt')
unique=set()
for f in v.glob('*.json'):
 if f.name in ['summary.json']:continue
 data=json.loads(f.read_text())
 ts=data if f.name=='new-mixin-targets.json'else data.get('targets',[])if isinstance(data,dict)else []
 for t in ts:
  cls=t.get('class_name',t.get('class'));method=t.get('method');desc=t.get('descriptor')
  if cls and method and desc:unique.add((cls,method,desc))
assert len(unique)==58,len(unique)
new=json.loads((v/'expansion-static.json').read_text());assert new['exact_window_identity'] and new['thread_guard_before_both_updates']
accum=json.loads((v/'accumulation-translated-glsl.json').read_text());assert accum['pixel_tests']==24 and not accum['pixel_failures']
stray=[r['feature']for r in json.loads((p/'audit/features.json').read_text())if r['source']=='Stray'and r['status']=='AUDITED_ENTRY_POINTS_NOT_PORTED'];assert stray==['CS2 Knife'],stray
summary=dict(version=version,minecraft='26.2',build='SUCCESS',tests=totals,registered_modules=55,mixin_classes=41,hud_editor_elements=25,unique_target_methods_rechecked=58,gitignore_bytes_unchanged=True,instant_save=True,settings_embedded_in_same_screen=True,hide_xp_level_and_bar_independent=True,new_modules=['custom.bestiary_esp','custom.invisibug_esp','custom.tiki_esp','custom.china_hat','custom.halo','custom.simon_says','custom.pet_display','render.picture_in_picture','custom.move_hotbar','custom.shitter_list'],extended_modules=['dungeon.waypoints'],excluded_archive_modules=['Cosmetics','AutoRag','AutoReel','AetherOSSettings'],remaining_unimplemented_archive_features=pending,remaining_unimplemented_stray_features=stray,adapted_archive_feature_objects=18,archive_feature_objects_with_partial_options=['TikiESP','SimonSays','PetDisplay','MoveHotbar','ShitterList','OdinDungeonWaypoints'],full_requested_feature_set_complete=False,source_parity_complete=False,in_game_tested_here=False,mixin_application_tested_here=False,windows_capture_tested_here=False,gpu_sodium_iris_tested_here=False,pip_windows_only=True,pip_explicit_session_target=True,pip_identity_fields=['hwnd','pid','title'],pip_desktop_fallback=False,pip_worker_owns_gdi=True,pip_capture_max_edge=640,blocklist_network_default=False,blocklist_kick_default=False,blocklist_atomic_writes=True,blocklist_corrupt_file_overwrite=False,cs2_knife_ported=False,valve_assets_bundled=False,item_aura_ported=False,shader_validation='offline software GL only',motion_blur_pixel_cases=24,jar_sha256=sha(jar),sources_jar_sha256=sha(sources),scope_document='docs/M13.md')
summary.update(new_modules=[],extended_modules=[],gui_title_bar_drag=True,gui_navigation_deferred=True,lazy_color_palette=True,held_item_crash_root_cause_confirmed=True,held_item_crash_fix_verified=False,minecraft_gui_launched_here=False,gui_notes='docs/GUI_1.3.3.md',reported_tooltip_null_path_addressed=True,compiled_tooltip_guard_verified=True,crash_report_version='1.3.0')
frame=json.loads((v/'frame-blending-static.json').read_text())
assert frame['pixel_cases']==16 and not frame['failures'] and frame['source_hashes_verified']
summary.update(validation_scripts=14,gui_coalesced_geometry=True,gui_modal_skips_hidden_cards=True,gui_panel_fill_calls_before=1256,gui_panel_fill_calls_after=23,gui_fps_measured_in_game=False,frame_blending_ported=True,frame_blending_raw_history=True,frame_blending_history_budget_bytes=96*1024*1024,frame_blending_max_history=24,frame_blending_max_samplers=12,frame_blending_pixel_cases=16,velocity_blur_ported=False,hybrid_blur_ported=False,motion_blur_symptom_resolved_in_game=False,inventory_classic_stray_port=True,inventory_frost_blur_ported=False,saved_algorithm_preserved=True,new_config_blur_default='frame_blend')
(v/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
# Old package recipe preserved in history/v1.2.0, not advertised for this release.
(v/'package-archive-dungeon-release.py').unlink(missing_ok=True)
(v/'package-combined-release.py').unlink(missing_ok=True)
shutil.copy2(Path(__file__),v/'package-gui-release.py')
dist=p/'dist';dist.mkdir(exist_ok=True)
for f in dist.iterdir():
 if f.is_file():f.unlink()
for f in [jar,sources]:shutil.copy2(f,dist/f.name)
(dist/'SHA256SUMS').write_text(''.join(f'{sha(f)}  {f.name}\n'for f in [jar,sources]))
for f in v.glob('*.json'):
 if f.name=='summary.json':continue
 # Every main evidence artifact has been rerun, except older manually retained reports.
 assert f.stat().st_size>0
out=work/f'solyanka-{version}-mc26.2-source.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED)as z:
 for f in sorted(p.rglob('*')):
  rel=f.relative_to(p)
  if f.is_file()and rel.parts[0]not in {'.gradle','.kotlin','build','.git','run','logs','crash-reports'}and not rel.parts[0].startswith('run-'):z.write(f,'solyanka/'+rel.as_posix())
with zipfile.ZipFile(out)as z:
 assert z.testzip()is None
 for path in ['docs/GUI_1.3.3.md','validation/history/v1.3.2/summary.json','audit/stray-ui-blur-source-manifest.json','docs/GUI_1.3.2.md','validation/history/v1.3.1/summary.json','docs/GUI_1.3.1.md','validation/history/v1.3.0/summary.json','docs/M13.md','audit/combined-source-manifest.json','validation/summary.json','validation/history/v1.2.0/summary.json','gradle/wrapper/gradle-wrapper.jar','.gitignore','src/client/java/dev/solyanka/client/pip/PipWin32.java']:assert 'solyanka/'+path in z.namelist(),path
 assert z.read('solyanka/.gitignore')==(p/'.gitignore').read_bytes()
 assert z.read('solyanka/dist/'+jar.name)==jar.read_bytes()
 assert not any(x.startswith(('solyanka/.gradle/','solyanka/.kotlin/','solyanka/build/'))for x in z.namelist())
print(json.dumps(dict(version=version,tests=totals,registered_modules=55,unique_native_targets=58,jar_sha256=sha(jar),jar_bytes=jar.stat().st_size,source_zip_bytes=out.stat().st_size,complete=False),indent=2))
