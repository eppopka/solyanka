# 1.3.6 — ItemAura, local CS2 knives and compact inventory

## ItemAura

A real first-person render module, not world-spawned particles. Ports archive Orbit Rings, DNA Helix, Comet Swarm, Vortex, Fireflies; Soft Orb / Sparkle Star / combined sprites; count/radius/size/speed; fading trails; soft glow; rainbow/hue spread/color; transforms, Y stretch and auto-spin; offhand and empty-hand controls. Captures the final vanilla hand transform, then uses a separate submit storage after normal hand features and before the hand view matrix is popped. The optional depth test uses the item's existing depth. Also attaches to the animated CS2 weapon rig when that replacement is active.

Differences: native 26.2 triangles replace UniversalCraft buffers. Ribbons are crossed gradient strips rather than the archive line buffer. Geometry is capped at 300,000 vertices per hand; extreme combinations truncate whole triangles. New default Only With Item is on. The archive's separate live calibration inspector is not ported; offsets can be edited with the world visible behind settings.

## CS2 Knives

Local asset workflow only. Uses Stray's open-source Source 2 VPK/KV/mesh/texture/animation decoders, skin composition, wear/seed logic, knife selection and viewmodel rig. Preserves the metallic knife shader; weighted bone skinning runs on the CPU through native custom-geometry submission, not Stray's global GPU buffer interception. Draw/idle/inspect/alternating swings, primary-hand left/right placement and local-player third person are wired. Scope guard and vanilla fallback apply when assets/rig are unavailable.

1. Enable CS2 Knives.
2. Let automatic discovery read common local Steam paths, or set the CS2 installation root / game/csgo folder.
3. Use Refresh list / status after loading. Select a knife and finish. Seed and wear changes debounce before composition.
4. Default Apply To is swords/axes; optional any-held-item and empty-main-hand modes.
5. Press F in the world to inspect. Animations/third-person and view offsets are configurable. Status: /solyanka knifestatus.

No Valve models, textures, rigs or packs are bundled, fetched, uploaded or redistributed. A local CS2 install is REQUIRED. This deliberately differs from upstream's bundled/downloaded data fallbacks. There is no install on this build computer, so real-asset decoding, appearance and in-game behavior remain unverified.

Not ported: per-sword skin profiles, custom grip editor (source defaults used), blade-only Held Item shader pass, original GPU queue and animated picker thumbnail UI. These are not presented as completed.

Lifecycle: one low-priority daemon handles local file reads/decompression/composition; native textures upload on the client/render thread. Source-path changes debounce 500ms, skin settings 250ms. Generation guards discard stale completed jobs; jobs capture their own install/rig references. Four-entry knife cache; install handles close after queued work; no retry loop for a failed selection. Disabling clears loaded state/textures; worker shuts down on client exit.

## Inventory HUD

Removed INVENTORY and its 14px header strip. Default height reduces from 132px to 118px. Count sits in the unused right side of the armor row; without armor it gets an 11px footer. Retains Stray's 18px wells, 2px gaps, armor/offhand row and separated hotbar; selected slot now respects the chosen accent. Hides in inventory/container screens like Stray. Position/scale, saved colors and toggles are retained. No frost/glass/post-blur renderer was added.

## Regression / acceptance

Clean build + unit tests; source hashes and compiled native hook signatures; native BindGroupLayout validator for Held Item, knives and aura; translated knife GLSL compile/link on software GL. The previous duplicate-Globals regression remains covered. These are NOT a Minecraft launch, GPU or visual/FPS acceptance test. No native HUD visual QA was possible here.

Manual acceptance: startup with modules off; each aura style on sword and offhand; empty-hand control; depth on/off; all toggles/offsets; loaded local knife with draw/inspect/swings and both main-arm settings; third person only replaces local player; rapid knife/wear/path changes then disable/disconnect; enable Inventory HUD and verify count, armor/hotbar toggles, HUD editor bounds and container hiding. If a problem remains, supply latest.log and module settings.
