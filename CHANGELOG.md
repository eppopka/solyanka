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
