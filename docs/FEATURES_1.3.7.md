# 1.3.7 — Stable PiP selection, continuous Held Item animation, compact scoreboard

## Picture in Picture

The picker labels each window with its application executable name and current title (PID if Windows withholds the executable path). Selection stays bound to that exact window handle and its owning process, not its changing title. Switching browser tabs or navigating pages therefore does not invalidate the selected window. PiP shows whichever tab that window currently displays; this is not background capture of a pinned browser tab.

Windows from the same program remain separate choices. Minimized/invisible windows show no image; restoring the selected window can resume capture. Closing/reopening the program requires selecting again: there is no automatic executable-name matching or capture of some other window. Process creation time guards against reused process IDs when available; if Windows withholds it at selection time, handle+PID guards remain. If creation time was known and becomes unavailable, capture fails closed. Selection is session-only, clears on disable/world change, and never falls back to desktop capture. PrintWindow-only limits and capture-worker ownership remain unchanged. Some GPU/protected windows may still return black, and an unresponsive program may stall PrintWindow.

## Held Item Shader

All four styles now read a session-local monotonic animation phase, independent of Minecraft world/server time. Native 26.2 derives GameTime from world gameTime modulo 24,000 ticks; that dependency has been removed from the effect. The local phase is not looped at the old 20-minute boundary. Texture recreation/resource cleanup does not reset it. Speed edits change velocity without repositioning the animation; zero speed freezes the current phase.

The third texel of the settings texture carries the phase as exact float bytes; no new bind group or duplicate Globals uniform was introduced. Source noise functions, style math and original speed units remain. This addresses a confirmed clock-reset mechanism, not a visual reproduction of the user's particular smoke artifact. Perspective/screen-space motion remains part of the source effect. Please test while standing still, across a world change and at different speeds; report if snapping remains.

## Scoreboard / appearance

Removed the decorative SCOREBOARD text and its vertical space in both live HUD and HUD editor. The server's actual objective name (e.g. SKYBLOCK) remains by default. The old Header switch is now labelled Objective title and controls that real title; an existing false value now hides it. The unused decorative accent color remains readable in saved configs for compatibility but is no longer shown as a control.

Added an explicit Apply Stray classic colors button to Scoreboard and Themed Chat settings. It uses the pinned upstream Theme.java classic dark window/line palette, plus scoreboard text color. It preserves position/scale and behavior switches, and never silently replaces customized colors. This is a palette preset, not a complete Stray design-engine port: its custom fonts, glass/frost blur, theme engines and associated animations are still not reproduced. The title-free compact inventory from 1.3.6 is retained. Side-by-side screenshots are needed to identify which other visual differences should be matched next.

## Validation / manual checks

Build, JVM tests, native ABI/bind-layout guards and translated GLSL/software pixel checks are recorded in validation/. These do NOT launch Minecraft, apply Mixins in-game, test Windows capture, visually inspect native HUD screens, or certify Sodium/Iris compatibility/FPS. No Windows/GPU/native visual QA is claimed.

Manual acceptance:
1. Replace the old JAR; retain config. Confirm main menu still appears.
2. Select your browser's application window; switch tabs and navigate. Capture should continue without reselecting. Try a second browser window to confirm it is NOT substituted.
3. Minimize/restore, close/reopen: blank while minimized, resume after restore, explicit reselect after closing. Never desktop fallback.
4. Held Item: smoke/stars/portal/galaxy, speed edits and zero-speed freeze; resize/fullscreen, resource reload and world changes must not reset the local phase.
5. Scoreboard: no decorative label or vacant label row, actual objective optional, no row clipping/score overlap; /solyanka gui bounds match. Try classic colors only if desired.
6. Compare Stray/solyanka at matching resolution, GUI scale and settings; provide screenshots for remaining styling differences.

The 1.3.6 ItemAura/CS2 local-install setup and previously documented exclusions/partial ports remain unchanged. No additional modules were enabled by default or excluded modules introduced.
