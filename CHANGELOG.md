# 1.3.12

- Added embedded searchable CS2 knife/finish-photo browser, composed preview, seed/wear controls and View tab.
- Added Item Bindings: persistent per-item custom-data UUID knife/finish/seed/wear profiles, explicit edit/global modes and safe loading transitions.
- Added optional first-person Hand / Arm Held Item Shader hook; existing hand-side gates and default-off behavior retained.
- Preserved advanced asset setup/download behind Assets / More, instant saving and visible module list.
- 640 JVM tests and 22 offline validation scripts passed. Native in-game visual acceptance remains pending; see docs/FEATURES_1.3.12.md.

# 1.3.11

- Replaced the approximate hotbar painter with the supplied original HotbarPainter and GlassRenderer implementations.
- Adopted original auxiliary geometry, floating-point selection, glass coating/sheen, rounded ring borders, clipped gradients, shimmer and whole-panel effects.
- Added opt-in 960x540 reference sizing with editor scale conversion; retained legacy coordinates and all recovery fixes.
- Added original-source parity/provenance tests. No AetherOSSettings or unrelated archive modules imported.

# 1.3.10

- Removed Selected HUD and right-click hiding; moved editor controls away from the bottom hotbar.
- Added Restore Hotbar, safe native fallback for old hidden hotbars, fitted effective scaling and impl.zip appearance defaults.
- Style changes retain appearance controls; gradients no longer composite over a second opaque background layer.
- Exact original painter is still unavailable from the supplied archive; see release scope.

# 1.3.9

- Fixed reversed-Z depth testing for Halo, China Hat triangles and shared filled world overlays. Through Walls remains an explicit bypass.
- Added depth-routing and software occlusion regression checks; native visual acceptance remains unverified.

# 1.3.8 — Knife assets, depth and HUD workflow

- Explicit confirmed Stray pack download with progress/cancel, pinned size/SHA-256, bounded reader and generation-safe reload; no assets bundled or automatic network fetch.
- Corrected ItemAura for 26.2 reversed-Z, preserving no-depth-write and late hand pass.
- Central HUD Editor buttons replace numeric screen layout controls; editor filters enabled modules/visible HUDs and handles empty state.
- Ctrl+F focuses search through deferred navigation; placeholder clears while typing.
- MoveHotbar Vanilla/Minimal/Glass/Neon presets, horizontal/vertical layout, chrome/effects, smooth selection and native item decorations. Exact source-painter parity remains partial.
- See docs/FEATURES_1.3.8.md for setup, intentional differences and unverified native runtime/visual acceptance.

# 1.3.7 — PiP, shader timing and scoreboard fixes

- PiP selection survives browser-tab/window-title changes; application-labelled picker, explicit HWND/process selection and no desktop fallback retained.
- Held Item local monotonic phase replaces world-clock animation; speed edits preserve phase and zero speed freezes; no duplicate Globals layout.
- Removed decorative SCOREBOARD and reserved row; actual objective title remains configurable. Added opt-in source classic color palette for scoreboard/chat.
- Native Windows capture, Minecraft rendering and visual/FPS acceptance still require in-game testing; see docs/FEATURES_1.3.7.md.

# 1.2.0 — personal secrets and indoor rain

- Added archive Secret Counter with one post-run stats request, bounded personal-result window and local feedback; chat announcements explicitly opt-in.
- Added Always Water declared indoor-rain behavior using native client-only rain/type/roof extraction adapters; no blocks, server weather or heightmap mutation.
- 45 modules, 41 Mixin classes, 20 HUD editor entries. Eight archive objects adapted, 14 pending; AetherOSSettings excluded.
- Existing instant-save undimmed settings and `.gitignore` preserved. No full implementation, server or native GPU acceptance claim; docs/M12.md.

# 1.1.0 — archive visual batch

- Added Ambient Particles: eight source styles with amount/radius sliders and native client-only spawning.
- Added Rotating Item: all five source patterns, speed/amplitude and mirrored hand offsets.
- Added Move Hearts: native vanilla health/optional armor transform, responsive reset and HUD editor support.
- 43 modules, 41 mixins and 20 HUD editor entries; embedded instant-save UI and `.gitignore` preserved.
- Six archive feature objects adapted, 16 remaining (AetherOSSettings excluded). No full parity/runtime/GPU acceptance claim; see docs/M11.md.

# 1.0.0 — requested release milestone

- Added Stray Accumulation Mix/Max temporal Motion Blur; velocity/frame-blend/hybrid modes remain pending.
- Added archive Stopwatch/Timer with key/mouse capture, local stop report and HUD editor support.
- Added archive DVD Bounce with source motion/colors and optional counter/sound/flash.
- 40 modules, 40 mixins, 19 HUD editor entries. Masters off by default; existing instant-save GUI and `.gitignore` preserved.
- Not full feature completion or runtime/GPU acceptance. See docs/M10.md and PENDING_FEATURES.md.

# 0.10.0-dev — Skybox

- Added source-derived decorated End sky, six cube faces and eight-frame animated black-hole patch.
- Added Vanilla End/Dimension Default styles, sky tint palette/strength, rotation/animation sliders and skyless-dimension opt-in.
- Ported sky rendering to native 26.2 target ownership, bind groups, vertex slices and indexed draws; added feature-owned GPU cleanup.
- Preserved embedded instant-save GUI, HUD editor, existing features, `.gitignore` and AetherOSSettings exclusion.
- Build/static validation only; native rendering and Sodium/Iris acceptance pending. See docs/M9.md.

# 0.6.0-dev — XP-bar visibility and Stray HUD batch

- Added independent Player Display Hide XP Bar, preserving mount/locator bars and the existing XP-level option.
- Added Inventory HUD, persistent Item Name HUD, Skill Progress HUD (29 registered modules total).
- Added bounded skill parsing, source XP tables and observed level hints, monotonic optional expiry and reset behavior; no API calls or invented live samples.
- All three HUDs integrate with instant-save settings/palettes/sliders and the layout editor (17 entries).
- Explicitly excluded impl.zip AetherOSSettings from future implementation scope.
- Additional regression/static API checks; in-game acceptance remains pending.

# 0.5.2-dev — compact settings, sliders and draggable HUD editor

- Compact one-line HUD toggle/edit rows with a narrow-screen fallback.
- Bounded numeric sliders with live value labels instead of numeric text entry.
- Bidirectional mode buttons: LMB next, RMB previous, wrapping both ways.
- Removed all timed preview controls, commands and preview state.
- Added deferred `/solyanka gui`: direct drag, scroll-scale, visibility toggle and selection of existing HUDs, including position-only Map Info.
- Editor samples never enable modules or mutate timer/stat data; live atomic saving/errors retained.
- Added slider, mode, layout, persistence and source-wiring regression checks. Runtime acceptance remains pending.

# 0.5.1-dev — settings cleanup

- Removed full-screen dim surfaces from GUI and settings roots.
- Added color swatch + Edit color with an inline owo palette; no new Screen.
- Replaced numeric modes with named cycling buttons (weather, clock, waypoint type, No Render modes, burger count).
- Scoped settings to each selected feature instead of showing entire shared sections.
- Split solver depth controls and migrated old shared depthTest values automatically.
- Removed Player Display preview buttons and the success footer; kept instant-save and inline errors.
- Clean build: 231 tests passed; 34 Minecraft target methods statically rechecked. Not in-game/GPU tested.

# 0.5.0-dev

- 14 additional real modules (26 total): Odin dungeon waypoints/solvers/doors/map/mimic, NoFrills experiments/NoRender/prices, Stray ambience/aspect/fog, archive Mimic Chest.
- Shared bounded loaded-room scanner, world line/fill/text snapshot renderer and official AW, dungeon score/key state and local Odin-compatible pack persistence/editor.
- Opt-in bounded public market providers with explicit unavailable/stale behavior, locally observed recipes/purchase costs, strict unknown-price/cycle guards.
- Same-Screen NoFrills-style GUI and immediate validated persistence retained; no Save button. Explicit source adaptations and pending backlog remain documented.
- New pure regression tests and target/callsite/thread-guard/asset checks. No in-game/GPU acceptance claimed.

## Previous releases

# Changelog

## 0.4.0-dev — Odin + NoFrills feature batch

- Added Odin Tick Timers: all six source HUDs, server-ping state machine and formatting/options.
- Added Odin Player Display: seven stat HUDs, action-bar parsing/hiding and vanilla HUD visibility.
- Added NoFrills Animation Fix, Middle Click Fix/Override, No Cursor Reset and safety-adapted No Loading Screen.
- Added shared sidebar/tab/floor/boss context, bounded/validated settings, independent HUD controls and local previews.
- Retained instant-save embedded settings; registered 12 real modules total, not placeholder toggles.
- Added 52 domain/migration regression tests; 162 total. In-game acceptance remains pending.
- Bundled full Odin BSD-3-Clause notice alongside GPL LICENSE/NOTICE in binary/source artifacts.
- Remaining requested features are explicitly not claimed complete.

## 0.3.1-dev — Instant-save embedded settings

- Removed Save & back, Done (stage), Cancel and separate Minecraft settings/error screens.
- Added embedded modal panels in the same /solyanka Screen with isolated input/tooltips and owned adapter cleanup.
- All valid setting/list/editor changes persist and apply immediately; Back/Escape navigates without undoing saved changes.
- Invalid/transient text and write failures stay inline; last-good configuration is preserved and correction retries automatically.
- Added 14 instant-save/model/disk-persistence regression tests; 110 total passing. In-game GUI acceptance remains pending.

## 0.3.0-dev — Chat Tweaks and Info Tooltips

- Added all eight upstream Chat Tweaks settings: two copy binds, trim, confirmation, preview cap, keep history, extra lines and history cap.
- Ported all five Info Tooltips options and item-ID variants/shard aliases.
- Updated chat hooks to Minecraft 26.2 Hud and GuiMessage.Line.parent APIs.
- Added opt-in, bounded/background Hypixel museum-category resource loading; original NoFrills endpoint unavailable during audit.
- Added staged settings screens and additive/unknown-field-preserving config migration.
- Added 36 regression tests: 96 total passing. New runtime hooks still need in-game acceptance.
- Previous 0.2.1 build was reported working by the user; no new 0.3.0 in-game test is claimed.

## 0.2.1-dev — NoFrills GUI update

- Replaced YACL with an adapted Kotlin port of NoFrills category cards, search, flat controls and settings panels.
- /solyanka now opens the GUI directly; /solyanka config remains an alias.
- Added inline key/mouse capture and stable-ID staged list/editor drafts.
- New external dependency: owo-lib 0.13.1+26.2; YACL is no longer required.
- Preserved existing command profiles, master toggles and optional Mod Menu/Right Shift entrypoints.
- Added 10 GUI catalog/draft tests; in-game appearance and interaction still require testing.

## 0.2.0-dev — M1 (Minecraft 26.2)

- Added required YACL settings and optional Mod Menu integration.
- Added typed command keybind and shortcut models, UI, capture, focus/filter policies and live routing.
- Added validation, bounded expansion and transactional profile migration/persistence.
- Added Kotlin keyboard/mouse mixins; static target ABI checked, not runtime-tested.
- Selected GPL-3.0-only for original/adapted project code with NoFrills attribution; included full license and source.
- Added command/input/parser/profile regression tests and in-game handoff.
- Alias completion nodes and broader tab-format support remain pending; all other source features remain in the backlog.

## 0.1.0-dev — M0

- Kotlin Fabric foundation, lifecycle registry, JSON settings, diagnostic commands.
- Initial audit and roadmap for 38 named upstream entries and all 34 attached Kotlin files.
