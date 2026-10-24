# 1.3.3 — GUI rendering cost, Stray frame blending and inventory HUD

## GUI performance work

The 1.3.1 rounded-panel implementation submitted one to three GUI fills for every pixel row. A single 400×420 settings panel generated **1,256 fill submissions**, before its shadow/header/widgets. This becomes **23 fill submissions** for the same panel. Coalescing adjacent identical scanlines preserves pixel coverage and translucent layering, while making straight edges one tall strip rather than hundreds of single-pixel strips. A bounded 128-entry, size-only geometry cache avoids rebuilding corners on every frame/drag. Colors and position are not cache keys.

Open modal settings no longer extract/render the underlying 55-card category tree. The same settings adapter still draws and owns mouse/key input; Back restores the feature list. No full-screen darkening or blur is introduced. Instant saves, dragging, sliders, palettes, reverse cycling and the 1.3.2 tooltip guard are retained.

**This is a geometry/work reduction, not an in-game FPS benchmark.** The reported FPS regression has not been reproduced here. If FPS still drops, capture before/open/closed FPS and whether the drop starts on the card list or only after opening a module.

## Motion Blur

Previous builds implemented Stray's Accumulation Mix/Max only. Their recursive history is intentionally capable of long trails; changing these equations would not reproduce the other Stray algorithms.

Added **Frame Blending (Stray)** using the pinned source's finite exposure window, measured/smoothed frame rate, monitor refresh rate, overlap weights, 12-sampler compaction and squared-color blend shader. It captures raw world frames before compositing, not the previous blurred output. The HUD remains outside the world-tail blur pass.

- New configuration default: `frame_blend`, strength 1. Existing saved `accum_mix` / `accum_max` selections and strength are **not overwritten**. Select Frame Blending explicitly to try it after upgrading.
- At strength 1, below/equal monitor refresh this algorithm may use just the current frame. This matches the source's exposure model, not a broken toggle. At higher FPS or strength it blends the relevant exposure interval.
- Lazy depthless history, up to 24 frames / 12 sampled frames. History plus its output target is bounded to **96 MiB** (texture payload; driver overhead excluded). Capacity adapts to pixel format/resolution. If two history frames plus output cannot fit, retain the native image. No full set of 24 eager depth targets.
- Size/pixel-format/mode/world/renderer reset releases history. Clock reversal and gaps over 0.5 s reseed the temporal ring. Pause in Menus defaults on; strength 0 releases resources. Accumulation retains its existing two-target path and pixel cap; the 96 MiB limit applies to Frame Blending.
- GLFW monitor selection uses greatest window overlap and refresh queries at most once per second. Main render-thread guard remains.
- **Velocity/pre-entity/F5 and Hybrid are still unimplemented.** No claim that every Stray motion-blur mode is present. The user's unspecified visual symptom cannot be declared resolved without comparison in-game.

## Inventory HUD

Reworked the earlier simplified renderer from the pinned Stray InventoryHudRenderer and classic HudChrome/GuiDraw/Theme source:

- 18px wells, 2px gaps, compact header and muted filled-slot counter.
- Armor/offhand row with centered empty-slot markers; 27 inventory slots; optional separated hotbar with selected-slot accent.
- Classic Stray dark panel/track/outline palette and rounded outer panel; compact header text without shadow.
- Two quads per non-minimal slot instead of a fill plus four separate border strips.
- Filled count cached once per player tick, keyed by weak player identity to avoid carrying the cache across worlds or retaining a disconnected world. Static slot ranges are reused.
- Existing minimal/background/armor/hotbar/count options, position/scale/color and `/solyanka gui` remain. Editor and live display use the same renderer. Existing custom colors/settings are preserved; only new defaults take the source palette.

This is the **classic native-font adaptation**, not Stray's full theme engine: frost blur, custom fonts, Control/glass/Hypr variants, animated starfield and Loadouts integration are not copied. No inventory click/packet automation.

## Verification and manual checks

Automated tests cover pixel/layer equivalence of coalesced bands over tiny and normal sizes, fill-count bound, frame timing/weight totals/compaction/ring resets/memory budget, old-mode config compatibility, and common inventory/editor renderer wiring. Native API/Mixin checks and translated GLSL tests are rerun. Multi-pixel frame-blending tests exercise 1/2/6/12 samples, UBO byte size and spatial orientation using the native fullscreen vertex source on software GL.

**No Minecraft launch, live FPS measurement, applied-Mixin test, visual acceptance, or native GPU/Sodium/Iris confirmation is claimed.** Software GLSL tests are not equivalent to those checks.

In game:
1. Replace the old JAR, keep only one solyanka version. Back up configuration first.
2. Compare FPS with GUI closed, on the feature list, and inside Held Item settings; drag/scroll, reopen, resize and test tooltips.
3. In Motion Blur choose Frame Blending (Stray), try strength 1 then 1.5/2, compare moving/stationary camera, open/close menus and resize. Check Mix/Max separately. Report the exact symptom if it remains.
4. Enable Inventory HUD and inspect armor/offhand, empty/enchanted/damaged stacks, counts, selected hotbar, minimal mode and `/solyanka gui` scaling/placement. No restart should be needed for a setting change.

The remaining feature backlog in M13.md is unchanged except that Frame Blending is now adapted and Inventory HUD classic styling is closer to source. Excluded modules and .gitignore are unchanged.
