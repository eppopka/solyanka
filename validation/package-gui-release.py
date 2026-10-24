from pathlib import Path
import json,zipfile,hashlib,shutil,xml.etree.ElementTree as E,re
p=Path('/data/solyanka');v=p/'validation';w=Path('/data/v139-work');version='1.3.9'
def sha(f):return hashlib.sha256(f.read_bytes()).hexdigest()
log=(w/'clean-build.log').read_text();assert 'BUILD SUCCESSFUL'in log and 'FAILED'not in log
results=json.loads((w/'validation-run.log').read_text());assert len(results)==20 and all(code==0 for _,code in results),results
roots=[E.parse(f).getroot()for f in(p/'build/test-results/test').glob('TEST-*.xml')]
tests={k:sum(int(x.attrib.get(k,0))for x in roots)for k in ['tests','failures','errors','skipped']};assert tests==dict(tests=602,failures=0,errors=0,skipped=0),tests
for f in v.glob('TEST-*.xml'):f.unlink()
for f in(p/'build/test-results/test').glob('TEST-*.xml'):shutil.copy2(f,v/f.name)
shutil.copy2(w/'clean-build.log',v/'clean-build.log')
for name,code in results:shutil.copy2(w/(Path(name).stem+'.log'),v/(Path(name).stem+'.log'))
jar=p/f'build/libs/solyanka-{version}.jar';sources=p/f'build/libs/solyanka-{version}-sources.jar'
assert sha(p/'.gitignore')==Path('/data/v132-work/gitignore.sha256').read_text().split()[0]
with zipfile.ZipFile(jar)as z:
 assert z.testzip()is None
 assert json.loads(z.read('fabric.mod.json'))['version']==version
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==44
 for path in ['NOTICE','LICENSE',*('licenses/'+f.name for f in(p/'licenses').iterdir()if f.is_file())]:assert z.read(path)==(p/path).read_bytes(),path
 for f in(p/'src/main/resources').rglob('*'):
  if f.is_file()and f.name!='fabric.mod.json':assert z.read(f.relative_to(p/'src/main/resources').as_posix())==f.read_bytes(),f
 for path in ['dev/solyanka/client/feature/ItemAura.class','dev/solyanka/utility/ItemAuraMesh.class','dev/solyanka/utility/KnifeSkinning.class','dev/solyanka/utility/HeldItemClock.class','dev/solyanka/client/cs2/pack/Cs2Pack.class','dev/solyanka/client/cs2/Cs2AssetDownload.class','dev/solyanka/utility/PipTargetIdentity.class','dev/solyanka/client/cs2/Cs2Knives.class','dev/solyanka/client/cs2/render/KnifeRender.class']:assert path in z.namelist()
 assert not any(any(x in f for x in ['AetherOSSettings','AutoRag','AutoReel','dev/stray/'])for f in z.namelist())
 assert not any(f.endswith(('.vpk','.vmdl_c','.vtex_c','.vnmclip_c','.vnmskel_c','.pack'))for f in z.namelist())
with zipfile.ZipFile(sources)as z:
 for root in[p/'src/main/kotlin',p/'src/main/java',p/'src/client/kotlin',p/'src/client/java']:
  for f in root.rglob('*'):
   if f.is_file():assert z.read(f.relative_to(root).as_posix())==f.read_bytes(),f
catalog=(p/'src/main/kotlin/dev/solyanka/ui/GuiModels.kt').read_text();assert len(re.findall(r'FeatureCardInfo\("',catalog))==57
archive=[x for x in json.loads((p/'audit/archive-inventory.json').read_text())if x.get('feature')];assert len(archive)==23 and sum(x['status']=='EXCLUDED_BY_USER'for x in archive)==4
assert not any(x['status']=='PENDING_IMPLEMENTATION'for x in archive)
stray=[r['feature']for r in json.loads((p/'audit/features.json').read_text())if r['source']=='Stray'and r['status']=='AUDITED_ENTRY_POINTS_NOT_PORTED'];assert not stray
unique=set()
for f in v.glob('*.json'):
 if f.name=='summary.json':continue
 data=json.loads(f.read_text());targets=data if f.name=='new-mixin-targets.json'else data.get('targets',[])if isinstance(data,dict)else []
 for t in targets:
  cls=t.get('class_name',t.get('class'));method=t.get('method');desc=t.get('descriptor')
  if cls and method and desc:unique.add((cls,method,desc))
assert len(unique)==61,len(unique)
layout=json.loads((v/'native-item-layout.json').read_text());assert layout['knife_and_aura_layouts_accepted'] and layout['old_duplicate_reproduced']
shader=json.loads((v/'cs2-translated-glsl.json').read_text());assert shader['linked'] and all(r['compiled']for r in shader['reports'])
parsers=json.loads((v/'cs2-parsers.json').read_text());assert parsers['synthetic_checks']==8
new=json.loads((v/'gui-hotbar-assets.json').read_text());assert new['synthetic_pack_checks']==10 and new['reversed_z_native_default_verified'] and new['depth_pixels']['gl_error']==0
assert not new['assets_bundled'] and not new['actual_asset_download_tested']
summary=json.loads((v/'history/v1.3.8/summary.json').read_text())
summary.update(version=version,tests=tests,validation_scripts=20,registered_modules=57,mixin_classes=44,unique_target_methods_rechecked=len(unique),jar_sha256=sha(jar),sources_jar_sha256=sha(sources),new_modules=[],extended_modules=['cosmetic.cs2_knife','custom.item_aura','custom.move_hotbar','hud.editor','gui.search'],scope_document='docs/FEATURES_1.3.9.md',remaining_unimplemented_archive_features=[],remaining_unimplemented_stray_features=[],adapted_archive_feature_objects=19,item_aura_ported=True,item_aura_vertex_budget_per_hand=300000,item_aura_in_game_verified=False,cs2_knife_ported=True,cs2_knife_local_install_required=False,cs2_knife_assets_bundled=False,cs2_knife_automatic_download=False,cs2_real_asset_decode_tested_here=False,cs2_synthetic_parser_cases=8,cs2_cpu_weighted_skinning=True,cs2_knife_native_metal_shader=True,cs2_gpu_queue_ported=False,cs2_per_sword_profiles_ported=False,cs2_blade_shader_ported=False,cs2_grip_editor_ported=False,cs2_picker_preview_ported=False,cs2_in_game_verified=False,inventory_title_removed=True,inventory_header_removed=True,inventory_default_height=118,inventory_container_hide=True,inventory_native_visual_qa=False,full_requested_feature_set_complete=False,source_parity_complete=False)
summary.update(pip_title_independent=True,pip_application_labels=True,pip_process_lifetime_guard_when_available=True,pip_no_automatic_retarget=True,pip_windows_runtime_tested=False,held_item_monotonic_phase=True,held_item_phase_texture_width=3,held_item_phase_speed_continuous=True,held_item_world_time_independent=True,held_item_native_visual_qa=False,scoreboard_decorative_label_removed=True,scoreboard_header_controls_objective=True,scoreboard_classic_palette_opt_in=True,themed_chat_classic_palette_opt_in=True,stray_fonts_and_glass_parity=False,native_visual_qa=False)
summary.update(cs2_explicit_pack_download=True,cs2_pack_sha256='8e726f0f3f6429c27841ccae3a37d050f9a620f8a83c4018ce571d1ba4152166',cs2_pack_bytes=134557137,cs2_asset_download_performed_here=False,cs2_asset_download_enabled_by_default=False,cs2_pack_synthetic_cases=10,cs2_cached_pack_bounds_validated=True,item_aura_reversed_z_fixed=True,item_aura_software_depth_tested=True,hud_editor_enabled_only=True,hud_editor_empty_state=True,hud_numeric_position_controls_removed=True,search_ctrl_f=True,search_placeholder_clears=True,hotbar_styles=['Vanilla','Minimal','Glass','Neon'],hotbar_vertical_layout=True,hotbar_native_decorations=True,hotbar_exact_archive_painter=False,hotbar_independent_fading_name_follower=False,hotbar_native_visual_qa=False,native_gui_visual_qa=False)
summary.update(extended_modules=['custom.halo','custom.china_hat','world.overlay'],halo_world_reversed_z_fixed=True,world_fills_reversed_z_fixed=True,halo_depth_writes=False,halo_through_walls_bypass_preserved=True,halo_in_game_verified=False,cs2_picker_menu_changed_in_this_release=False)
(v/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');shutil.copy2(Path(__file__),v/'package-gui-release.py')
dist=p/'dist';dist.mkdir(exist_ok=True)
for f in dist.iterdir():
 if f.is_file():f.unlink()
for f in[jar,sources]:shutil.copy2(f,dist/f.name)
(dist/'SHA256SUMS').write_text(''.join(f'{sha(f)}  {f.name}\n'for f in[jar,sources]))
out=w/f'solyanka-{version}-mc26.2-source.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED)as z:
 for f in sorted(p.rglob('*')):
  rel=f.relative_to(p)
  if f.is_file()and rel.parts[0]not in{'.gradle','.kotlin','build','.git','run','logs','crash-reports'}and not rel.parts[0].startswith('run-'):z.write(f,'solyanka/'+rel.as_posix())
with zipfile.ZipFile(out)as z:
 assert z.testzip()is None
 for path in ['docs/FEATURES_1.3.9.md','audit/final-cosmetics-source-manifest.json','licenses/CS2-THIRD-PARTY-NOTICES.md','validation/check-final-cosmetics.py','validation/CheckCs2Parsers.java','src/main/java/dev/solyanka/utility/KnifeSkinning.java','src/main/kotlin/dev/solyanka/utility/HeldItemClock.kt','src/main/java/dev/solyanka/utility/PipTargetIdentity.java','src/client/java/dev/solyanka/client/cs2/Cs2AssetDownload.java','src/client/java/dev/solyanka/client/cs2/pack/Cs2Pack.java','validation/check-gui-hotbar-assets.py']:
  assert z.read('solyanka/'+path)==(p/path).read_bytes(),path
 assert z.read('solyanka/dist/'+jar.name)==jar.read_bytes()
print(json.dumps(dict(version=version,tests=tests,validation_scripts=20,registered_modules=57,mixin_classes=44,native_targets=len(unique),jar_sha256=sha(jar),jar_bytes=jar.stat().st_size,source_zip_bytes=out.stat().st_size,in_game_verified=False),indent=2))
