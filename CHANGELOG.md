# Changelog

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
