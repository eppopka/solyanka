# Source audit — initial pass

This milestone is a source/dependency audit and original infrastructure, not a completed feature port or a comprehensive security review.

## Source locks and versions

- Odin: `19118e15bfb57a953c3264373a32b42171c5f281` — https://github.com/odtheking/Odin/tree/19118e15bfb57a953c3264373a32b42171c5f281 — BSD-3-Clause
- NoFrills: `74e9317c1a7b8484a575b2f177479f5e3f2bfb9a` — https://github.com/WhatYouThing/NoFrills/tree/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a — GPL-3.0 (check file headers for only/or-later before porting)
- Stray: `d91fcd4515b4eda647ee8fd7fa99408f473e5718` — https://github.com/camberX/Stray/tree/d91fcd4515b4eda647ee8fd7fa99408f473e5718 — CC0-1.0
- noammaddons: `b552eb85050c9d12ff3aabf11ab0c793d1565361` — https://github.com/noamm9/noammaddons/tree/b552eb85050c9d12ff3aabf11ab0c793d1565361 — CC0-1.0

The audited default branches target Odin 26.3, NoFrills 26.1, Stray 26.1.2 and noammaddons 26.1.2. No source is assumed binary/API-compatible with 26.2. Matching older branches/tags should be considered at each port rather than blindly backporting current HEAD.

## Important findings

1. Minecraft 26.2 requires Java 25. Its official Fabric example uses the non-remapping `net.fabricmc.fabric-loom` plugin with `implementation` dependencies and no Yarn declaration. The starter follows that structure.
2. `impl.zip` is not a complete mod: no build scripts, entrypoint, framework, mixins or asset tree. Imported eppopkaaddons event/config/renderer/GUI classes are absent. noammaddons is a reference, not an automatic drop-in replacement for that customized framework.
3. Several archive features expose hook methods without subscribing to events. Merely copying their Feature objects would produce inert toggles. Recover the external call sites before claiming those ports work.
4. Odin Creeper Beams is `BeamsSolver.kt` and consumes `assets/odin/puzzles/creeper-beams-solutions.json`. Copying only the solver loses required data.
5. Odin TickTimers contains multiple boss/event timers (including Goldor and Storm), not a generic stopwatch. Archive `Timer.kt` is separate. Keep both.
6. NoFrills PriceTooltips depends on NoFrillsAPI, item metadata, recipe/pricing state and more than one price type. Do not replace it with a single Bazaar lookup while claiming parity.
7. Stray picture-in-picture captures another desktop window into a HUD texture. Its current backend uses Windows APIs via JNA; it is not an alternate in-game camera. Keep platform-gating explicit. Linux/macOS capture would be additional work.
8. Stray has a Sodium-specific fog mixin and GPU-heavy glow/blur paths. Optional integrations must not class-load missing APIs; test real compatible Sodium/Iris versions, window resize and render-state restoration.
9. Waypoints/mimic handling overlap between Odin and the archive. Share underlying state but preserve both sets of settings/codec semantics; do not silently delete one implementation's behavior.
10. AutoRag and AutoReel automate gameplay actions. Keep them disabled by default, visibly opt-in and distinguish them from passive hints. No claim is made that any feature is permitted by Hypixel rules.
11. Archive comments identify additional upstream provenance: Odin waypoints and MIT-derived Shitterlist. Parent repository licensing is not enough to resolve all embedded code/assets.

## Licensing / distribution gate

- Odin: BSD-3-Clause; preserve full notice, conditions and disclaimer for incorporated code.
- NoFrills: repository GPLv3 text. Before incorporating code, check source headers for exact grant and plan compliant distribution of the combined derivative work, including corresponding source. A closed-source combined distribution cannot simply be assumed permissible.
- Stray and noammaddons: repository CC0-1.0; inspect bundled assets and third-party notices separately, especially knife models/textures and cosmetic content.
- impl.zip has no standalone license. User-supplied authorship context does not resolve all third-party provenance. Confirm rights/permissions before redistribution.
- No upstream implementation/assets are compiled or bundled at this stage. A project-wide license is not selected on the owner's behalf; metadata is conservative until a decision is made.

## Inventory limits

`audit/features.json` locates all requested named features. `audit/archive-inventory.json` records every Kotlin file, SHA-256, imports, detected event registrations and URL strings. These are static observations, not proof of runtime reachability, correctness, safety or full dependency closure. The next module audit must inspect each caller, mixin, asset, network dependency and license header.
