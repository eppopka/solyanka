# Iterative implementation roadmap

Every requested feature is mandatory backlog scope. Ordering is not permission to omit features. Full parity will require multiple milestones.

## M0 — foundation and initial audit (this delivery)
Build scripts/wrapper, Kotlin client entrypoint, module lifecycle, persistence, diagnostics, core tests, source locks and complete requested-feature/attachment inventory. Validate compiler/test results separately from in-game testing.

## M1 — usable settings + input/commands
YACL + Mod Menu integration, typed settings, translation keys, input focus routing, command keybinds and shortcuts. Keep fixed command boundaries/loop prevention. Acceptance: bind, execute, persist, reopen, restart, no triggering while typing.

## M2 — shared context and low-risk information
SkyBlock/location/item parsing, chat event adapter, basic Chat Tweaks + Info Tooltips; pricing provider contract/cache/error handling before full Price Tooltips. License gate before importing NoFrills code.

## M3 — dungeon state + first overlays
Room/map model, coordinate rotation fixtures, room enter/exit and secret/run events, renderer adapter. Port door highlight, mimic/chests and map info; then both waypoint implementations/codec/editor, SecretCounter and Player Display. Acceptance: rotated rooms, reconnect/world changes, collection/reset, import/export parity.

## M4 — puzzle/experiment state machines
Ice Fill, Blaze, Creeper Beams, Experiment Solver and archive SimonSays. Pure replay fixtures first; integrate block/entity/container events and render output second. Solving hints must not accidentally become automated clicking.

## M5 — timers and HUD
All source TickTimers variants, archive Timer/PetDisplay; shared HUD editor, Skill Progress, Inventory, Scoreboard, Item Name, themed chat, DvdBounce, MoveHearts and MoveHotbar. Preserve source-specific options and reset rules.

## M6 — input and rendering fixes
No Render, Animation Fix, Middle Click Fix/Override with explicit precedence, No Cursor Reset and No Loading Screen. Check mouse focus, loading readiness and disconnect behavior in each screen transition.

## M7 — ESP and environment
Player Glow, Nametags, Block Outline, Titanium, Bestiary, Invisibug, Tiki; then Skybox/Ambience/Fog/Aspect/AlwaysWater/AmbientParticles. Verify vanilla renderer first, then actual 26.2 Sodium and Iris/shader combinations; no untested compatibility promises.

## M8 — advanced visuals and cosmetics
Motion Blur resource lifecycle, Windows window-capture PiP, CS2 Knife assets/materials, Held Item/RotatingItem/ItemAura, ChinaHat/Halo/Cosmetics, badges/wings/text replacement. Complete missing hook/asset provenance first. Platform gating and performance profiling are release criteria.

## M9 — remaining archive behavior and hardening
ShitterList/profile I/O, AetherOS settings fidelity, AutoRag/AutoReel behind explicit opt-in; verify all helper files consumed. Full feature parity checklist, rule warnings, notice/source distribution, migration, resource leaks and regression/performance tests.

## Per-feature completion gate
1. Trace all settings, events, mixins, assets, remote data and license notices.
2. Establish expected behavior from pinned source, not just the feature name.
3. Implement core logic and fixtures; add platform adapters.
4. Build + unit tests; inspect packaged metadata.
5. User in-game testing on exact MC 26.2 with minimal mods.
6. Repeat with required performance/shader mods and document versions.
7. Mark implemented only after acceptance; unresolved items remain visible in the matrix.
