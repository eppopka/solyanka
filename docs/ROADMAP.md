# Current roadmap — 1.2.0

Current delivery: 45 registered modules; Odin/NoFrills requested categories and many Stray features adapted, plus eight archive feature objects. Build/static verification is distinct from runtime acceptance. See FEATURE_MATRIX.md and PENDING_FEATURES.md for exact scope.

Next unresolved work:
1. Archive dungeon state: SimonSays and OdinDungeonWaypoints route-step extensions.
2. Archive PetDisplay, Bestiary/Invisibug/Tiki ESP, MoveHotbar and remaining cosmetics.
3. Stray Picture in Picture, CS2 knife backend and remaining blur algorithms.
4. Source option/behavior parity, prices/provider coverage and live native/Mixin/Sodium/Iris acceptance.
5. Automatic-action archive features require explicit opt-in and server-rule warnings. AetherOSSettings stays excluded.

## Historical roadmap (original milestone ordering)

# Historical roadmap — 0.5.0-dev

Delivered 14 additional real modules: remaining seven requested Odin feature categories, three NoFrills categories, three Stray environment categories and archive Mimic Chest. These are adapted implementations, not full parity/acceptance. All remaining scope is retained.

Next unresolved implementation work:
1. Stray Player Glow/Nametags/Block Outline/Titanium, then Skill/Inventory/Scoreboard/Item Name/Themed Chat.
2. Stray Skybox/Motion Blur resource lifecycle, real Windows window-capture PiP, CS2 knife asset/material provenance and Held Item shader.
3. Archive SecretCounter/SimonSays/OdinDungeonWaypoints route-step extensions, PetDisplay/Timer and ESP features.
4. Remaining archive cosmetics/settings/list/automatic-action features with explicit opt-ins and version-specific adapters.
5. Complete source option parity and acceptance of current adaptations: map bonus/party state, full editor widgets, pricing/recipe coverage, NoRender slayer state, Sodium fog shader path and all GPU/in-game checks.

No future feature is presented as implemented; see FEATURE_MATRIX.md and archive inventory. Existing older milestone ordering is retained below as historical planning, not current status.

## Original milestone plan (historical)

# Iterative implementation roadmap

Every requested feature is mandatory backlog scope. Ordering is not permission to omit features. Full parity will require multiple milestones.

## M0 — foundation and initial audit (delivered in 0.1)
Build scripts/wrapper, Kotlin client entrypoint, module lifecycle, persistence, diagnostics, core tests, source locks and complete requested-feature/attachment inventory. Validate compiler/test results separately from in-game testing.

## M1 — usable settings + input/commands (implemented/build-tested in 0.2; in-game acceptance pending)

The initial YACL GUI was replaced in 0.2.1 by the adapted NoFrills/owo GUI; /solyanka opens it directly. Version 0.3.1 removes explicit save/staging and embeds all settings inside the same Screen. See GUI_PORT.md and historical M1.md for delivered behavior and differences. Alias completion/help nodes remain a follow-up. The user reported 0.2.1 working; no exhaustive runtime compatibility claim is made.

NoFrills/owo + optional Mod Menu integration, typed settings, translation keys, input focus routing, command keybinds and shortcuts. Keep fixed command boundaries/loop prevention. Acceptance: bind, execute, persist, reopen, restart, no triggering while typing.

## M2 — shared context and low-risk information (M2a delivered in 0.3.0)
M2a implements Chat Tweaks + Info Tooltips, item-ID/shard parsing and opt-in museum resource loading. See M2.md. Remaining M2: shared SkyBlock/location context, pricing provider contract/cache/error handling and full Price Tooltips. GPLv3 project decision is now recorded; retain attribution/source and inspect additional file/asset provenance before each port.

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

## 0.4.0 delivered cross-source batch

Odin Tick Timers and Player Display were pulled forward using the new context/HUD adapters. Five NoFrills utilities are now implemented: Animation Fix, Middle Click Fix/Override, No Cursor Reset and safety-adapted No Loading Screen. These remain pending in-game acceptance. Their completion does not mark whole M3–M6 milestones complete. Seven Odin feature entries, Price Tooltips/No Render/Experiment Solver, all Stray and archive features still require their own ports. See M3.md for exact boundaries.
