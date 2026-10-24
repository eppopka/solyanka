from pathlib import Path
import json, shutil, hashlib, zipfile, xml.etree.ElementTree as ET,re
p=Path('/data/solyanka');v=p/'validation';version='1.0.0'
log=Path('/data/v1-work/final-clean-build.log');assert 'BUILD SUCCESSFUL' in log.read_text();shutil.copy2(log,v/'clean-build.log')
for f in v.glob('TEST-*.xml'):f.unlink()
roots=[]
for f in (p/'build/test-results/test').glob('TEST-*.xml'):
 roots.append(ET.parse(f).getroot());shutil.copy2(f,v/f.name)
totals={k:sum(int(x.attrib.get(k,0)) for x in roots) for k in ['tests','failures','errors','skipped']};assert totals==dict(tests=373,failures=0,errors=0,skipped=0),totals
for src,dst in [('hook-check.log','hook-check.log'),('world-hook-check.log','world-hook-check.log'),('hud-hook-check.log','hud-hook-check.log'),('held-item-hook-check.log','held-item-hook-check.log'),('shader-check.log','shader-check.log'),('skybox-hook-check.log','skybox-hook-check.log'),('v1-hook-check.log','v1-hook-check.log'),('accumulation-shader-check.log','accumulation-shader-check.log')]:shutil.copy2(Path('/data/v1-work')/src,v/dst)
assert 'new target methods verified: 34' in (v/'hook-check.log').read_text()
world=json.loads((v/'world-visual-static.json').read_text());assert world['compiled_outline_callback_null_checked'];assert world['mixin_classes']==40
ui=p/'src/client/kotlin/dev/solyanka/client/ui';kit=(ui/'nofrills/GuiKit.kt').read_text();panel=(ui/'nofrills/SettingsPanel.kt').read_text();screen=(ui/'nofrills/NoFrillsClickGui.kt').read_text();client=(p/'src/client/kotlin/dev/solyanka/client/SolyankaClient.kt').read_text()
checks=dict(settings_use_createWithoutScreen='createWithoutScreen(' in panel,settings_navigation_calls_setScreen='setScreen' in panel,settings_root_transparent='root.surface(' not in panel,main_root_transparent='root.surface(' not in screen,swatch_and_embedded_palette='container.child(picker)' in kit,bounded_sliders='SlimSliderComponent(' in kit,explicit_right_click_previous='if(click.button()==1) {cycle(-1);return true}' in kit,success_status_removed='Changes save instantly.' not in panel,hud_editor_command_deferred='literal("gui").executes { openHudRequested=true;1 }' in client,new_world_settings_embedded='host.showPanel(SettingsPanel(' in (ui/'WorldVisualUi.kt').read_text(),new_world_settings_typed_instant_save='model.updateGeneral' in (ui/'WorldVisualUi.kt').read_text(),in_game_tested_here=False)
for k,value in checks.items():
 if k not in ['settings_navigation_calls_setScreen','in_game_tested_here']:assert value,k
assert not checks['settings_navigation_calls_setScreen']
held_ui=(ui/'HeldItemUi.kt').read_text();assert 'host.showPanel(SettingsPanel(' in held_ui and 'model.updateGeneral' in held_ui and 'setScreen' not in held_ui
held=json.loads((v/'held-item-static.json').read_text());assert held['compiled_handler_signature_verified'] and held['mixin_classes']==40
translated=json.loads((v/'held-item-translated-glsl.json').read_text());assert translated['linked'] and all(r['compiled'] for r in translated['reports']);assert not translated['minecraft_pipeline_tested'] and not translated['actual_gpu_tested']
(v/'instant-ui-static.json').write_text(json.dumps(checks,indent=2)+'\n')
sky=json.loads((v/'skybox-static.json').read_text());assert sky['asset_count']==14 and sky['compiled_wrap_handler_signature_verified'] and sky['mixin_classes']==40
hud_checks=json.loads((v/'hud-style-static.json').read_text());assert hud_checks['compiled_chat_scope_has_finally'];assert len(hud_checks['targets'])==7
scope=json.loads((p/'audit/scope.json').read_text());assert scope['excluded_features'][0]['implement']==False
release_one=json.loads((v/'v1-static.json').read_text());assert len(release_one['targets'])==4 and release_one['compiled_tail_and_cleanup_callbacks'] and release_one['new_gui_embedded_instant_save'];assert release_one['mixin_classes']==40
accum=json.loads((v/'accumulation-translated-glsl.json').read_text());assert accum['pixel_tests']==24 and not accum['pixel_failures'];assert all(r['compiled'] and r['linked'] for r in accum['reports']);assert not accum['minecraft_pipeline_tested'] and not accum['actual_gpu_tested']
release_ui=(ui/'ReleaseOneUi.kt').read_text();assert 'setScreen' not in release_ui
archive_pending=[r['file'] for r in json.loads((p/'audit/archive-inventory.json').read_text()) if r.get('feature') and r['status']=='PENDING_IMPLEMENTATION' and not r['file'].endswith('/AetherOSSettings.kt')];assert len(archive_pending)==19
jar=p/f'build/libs/solyanka-{version}.jar';sources=p/f'build/libs/solyanka-{version}-sources.jar'
def sha(f):return hashlib.sha256(f.read_bytes()).hexdigest()
with zipfile.ZipFile(jar) as z:
 assert json.loads(z.read('fabric.mod.json'))['version']==version
 names=z.namelist()
 for name in ['dev/solyanka/client/feature/TemporalBlur.class','dev/solyanka/client/feature/ArchiveHuds.class','dev/solyanka/client/mixin/TemporalBlurMixin.class','dev/solyanka/client/ui/ReleaseOneUi.class','dev/solyanka/utility/MotionBlurSettings.class','dev/solyanka/utility/BlurPolicy.class','dev/solyanka/utility/StopwatchClock.class','dev/solyanka/utility/StopwatchSettings.class','dev/solyanka/utility/DvdSettings.class','dev/solyanka/utility/DvdMotion.class','assets/solyanka/shaders/post/accum_mix.fsh','assets/solyanka/shaders/post/accum_max.fsh','dev/solyanka/client/feature/SkyboxVisual.class','dev/solyanka/client/mixin/SkyboxMixin.class','dev/solyanka/client/ui/SkyboxUi.class','dev/solyanka/utility/SkyboxSettings.class','dev/solyanka/utility/SkyboxLogic.class','dev/solyanka/utility/SkyFrames.class','dev/solyanka/utility/SkyVertex.class','dev/solyanka/utility/SkyPoint.class','dev/solyanka/client/feature/HeldItemCosmetic.class','dev/solyanka/client/mixin/HeldItemFeatureMixin.class','dev/solyanka/client/ui/HeldItemUi.class','dev/solyanka/utility/HeldItemSettings.class','dev/solyanka/utility/HeldItemPolicy.class','assets/solyanka/shaders/core/held_item.vsh','assets/solyanka/shaders/core/held_item.fsh','dev/solyanka/client/feature/ScoreboardHud.class','dev/solyanka/client/feature/ThemedChat.class','dev/solyanka/client/feature/ChromeDraw.class','dev/solyanka/client/mixin/ThemedChatMixin.class','dev/solyanka/client/mixin/ThemedChatFillMixin.class','dev/solyanka/client/mixin/ThemedChatScreenMixin.class','dev/solyanka/client/ui/HudStyleUi.class','LICENSE','NOTICE','licenses/Odin-BSD-3-Clause.txt','licenses/Stray-CC0.txt','dev/solyanka/client/feature/WorldVisuals.class','dev/solyanka/client/feature/TitaniumScanner.class','dev/solyanka/client/feature/BlockSelection.class','dev/solyanka/client/mixin/VisualEntityStateMixin.class','dev/solyanka/client/mixin/VisualEntityRendererMixin.class','dev/solyanka/client/mixin/PlayerGlowMinecraftMixin.class','dev/solyanka/client/ui/WorldVisualUi.class','dev/solyanka/utility/TitaniumLogic.class','assets/solyanka/mining/titanium-regions.json']:assert name in names,name
 assert len(json.loads(z.read('solyanka.client.mixins.json'))['client'])==40
 for a in sky['assets']:
  key='assets/solyanka/textures/sky/end/'+a['file'];assert hashlib.sha256(z.read(key)).hexdigest()==a['sha256']
 assert not any('AetherOS' in name for name in names)
 for ext in ['vsh','fsh']:
  path=f'assets/solyanka/shaders/core/held_item.{ext}';assert z.read(path)==(p/'src/main/resources'/path).read_bytes()
 assert z.read('assets/solyanka/mining/titanium-regions.json')==(p/'src/main/resources/assets/solyanka/mining/titanium-regions.json').read_bytes()
 for mode in ['accum_mix','accum_max']:
  path=f'assets/solyanka/shaders/post/{mode}.fsh';assert z.read(path)==(p/'src/main/resources'/path).read_bytes()
with zipfile.ZipFile(sources) as z:
 for root in [p/'src/main/kotlin',p/'src/client/kotlin']:
  for source in root.rglob('*.kt'):assert z.read(source.relative_to(root).as_posix())==source.read_bytes(),source
versions=json.loads((v/'new-mixin-targets.json').read_text())
unique={(t['class'],t['method'],t['descriptor']) for t in versions}
unique.update((t['class'],t['method'],t['descriptor']) for t in json.loads((v/'experience-bar-static.json').read_text())['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in world['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in hud_checks['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in held['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in sky['targets'])
unique.update((t['class_name'],t['method'],t['descriptor']) for t in release_one['targets']);assert len(unique)==55,len(unique)
rows=json.loads((p/'audit/features.json').read_text());remaining=[r['feature'] for r in rows if r['source']=='Stray' and r['status']=='AUDITED_ENTRY_POINTS_NOT_PORTED'];assert len(remaining)==2,remaining
summary=dict(version=version,minecraft='26.2',build='SUCCESS',tests=totals,registered_modules=40,new_modules=['render.motion_blur','custom.stopwatch','custom.dvd_bounce'],mixin_classes=40,unique_target_methods_rechecked=len(unique),world_target_methods_checked=4,compiled_nullable_outline_guard=True,instant_save=True,settings_embedded_in_same_screen=True,hud_editor_elements=19,hide_xp_level_and_bar_independent=True,aetheros_settings_excluded=True,remaining_stray_features=remaining,remaining_archive_feature_objects=len(archive_pending),in_game_tested_here=False,mixin_application_tested_here=False,gpu_sodium_iris_tested_here=False,full_requested_feature_set_complete=False,source_blur_parity=False,chat_animation_ported=False,chat_security_handlers_preserved=True,nametags_preserve_styled_components=True,block_outline_layered_halo=True,jar_sha256=sha(jar),sources_jar_sha256=sha(sources),held_item_styles=['smoke','stars','portal','galaxy'],held_item_first_person_baked_quads_only=True,held_item_arms_and_silhouette_ported=False,translated_offline_glsl_linked=True,held_item_config_texture_pixels=2,held_item_atlas_cache_limit=64,cs2_knife_ported=False,skybox_styles=['decorated_end','end','dimension'],skybox_source_assets=14,skybox_raw_texture_bytes=sky['raw_texture_bytes'],skybox_native_target_owned=True,skybox_global_matrix_stack_modified=False,skybox_gpu_cleanup=True,skybox_terrain_world_tint_ported=False,skybox_live_visuals_verified=False,baseline_note='Older unchanged ABI/AW evidence retained; clean Gradle AW check and current descriptor checks. Static checks do not establish Mixin application or rendering compatibility. See docs/M6.md, docs/M7.md, docs/M8.md and docs/M9.md.')
summary.update(motion_blur_modes=['accum_mix','accum_max'],motion_blur_remaining_algorithms=['velocity','pre_entity','f5','refresh_scaling','weighted_frame_blend','hybrid'],motion_blur_pixel_tests=24,motion_blur_pixel_failures=0,motion_blur_history_pixel_budget=8388608,motion_blur_disjoint_read_write_targets=True,motion_blur_depthless_matching_format=True,stopwatch_three_press_cycle=True,stopwatch_keyboard_mouse_capture=True,stopwatch_local_feedback_only=True,dvd_corner_counter_sound_flash=True,remaining_archive_features=archive_pending,release_is_milestone_not_full_completion=True)
(v/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
dist=p/'dist'
for f in dist.iterdir():
 if f.is_file():f.unlink()
for f in [jar,sources]:shutil.copy2(f,dist/f.name)
(dist/'SHA256SUMS').write_text(''.join(f'{sha(f)}  {f.name}\n' for f in [jar,sources]))
(v/'package-world-release.py').unlink(missing_ok=True)
(v/'package-hud-release.py').unlink(missing_ok=True)
# Bundle this exact packaging script for reproducibility; hook checks run separately.
(v/'package-held-item-release.py').unlink(missing_ok=True)
(v/'package-skybox-release.py').unlink(missing_ok=True)
shutil.copy2(Path(__file__),v/'package-v1-release.py')
out=Path('/data/v1-work/solyanka-1.0.0-mc26.2-source.zip')
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted(p.rglob('*')):
  rel=f.relative_to(p)
  if f.is_file() and rel.parts[0] not in {'.gradle','.kotlin','build','.git','run'}:z.write(f,'solyanka/'+rel.as_posix())
with zipfile.ZipFile(out) as z:
 assert z.testzip() is None
 assert all(x in z.namelist() for x in ['solyanka/docs/M10.md','solyanka/audit/v1-source-manifest.json','solyanka/validation/history/v0.10.0/summary.json','solyanka/src/main/resources/assets/solyanka/shaders/post/accum_mix.fsh'])
 assert z.read('solyanka/.gitignore')==(p/'.gitignore').read_bytes()
 assert b'/dist/' in z.read('solyanka/.gitignore')
 assert 'solyanka/gradle/wrapper/gradle-wrapper.jar' in z.namelist()
 assert not any(x.startswith(('solyanka/.gradle/','solyanka/.kotlin/','solyanka/build/')) for x in z.namelist())
 assert z.read('solyanka/dist/'+jar.name)==jar.read_bytes()
 assert json.loads(z.read('solyanka/audit/scope.json'))['excluded_features'][0]['implement']==False
 assert json.loads(z.read('solyanka/validation/summary.json'))['version']==version
print(json.dumps(dict(build='SUCCESS',tests=totals,unique_target_methods_rechecked=len(unique),remaining_stray=remaining,jar_bytes=jar.stat().st_size,source_zip_bytes=out.stat().st_size,jar_sha256=sha(jar)),indent=2))
