# solyanka 1.3.10 — Hotbar recovery and HUD editor cleanup

## Changes
- Removed the Selected HUD selector. Click a HUD directly to select it.
- Right-click now selects rather than toggling HUD visibility. Enabled-only filtering remains; the editor does not enable disabled modules.
- Moved Done and Restore Hotbar to the upper-right area so the bottom hotbar can actually receive drag and scroll input. Previously the entire bottom 43 pixels were reserved for controls.
- Added Restore Hotbar near the top of Move Hotbar settings and in the editor while that module is enabled. It restores visibility, centered automatic positioning, scale 1 and Keep On Screen, without changing style or colors.
- Removed the separate hotbar HUD Visible toggle. Old hidden configs delegate to the original native hotbar rather than hiding the entire hotbar; Restore Hotbar returns them to styled editing.
- Complete styled geometry, including offhand/attack indicator and effect margins, is fitted to normal Minecraft GUI sizes when Keep On Screen is enabled. Gameplay and preview use the same effective scale. Stored requested scale is not changed just by resizing the window. Extremely tiny unsupported viewports can still clip below the config minimum scale of 0.5.
- Added Use impl.zip appearance defaults: restores the declared Glass setup, glow/shadow/gradient enabled, original colors, spacing, padding and effect settings. It preserves layout coordinates/scale. Style selection itself now preserves colors and effects rather than applying divergent presets.
- Removed double-compositing of the background under gradient bands, which made translucent hotbars darker/more opaque than their configured opacity.

## Recovery
Enable Move Hotbar, open its settings and press Restore Hotbar. Optionally press Use impl.zip appearance defaults. Open /solyanka gui and drag the actual hotbar; scrolling changes its size. Existing config files can be retained.

## Exact archive appearance remains incomplete
The supplied impl.zip contains impl/visual/MoveHotbar.kt but not the imported dev.eppopkaaddons.ui.hud.hotbar helpers: HotbarPainter, HotbarLayout, HotbarSelection, HotbarViewport and HotbarBox. It also references the missing shared GlassRenderer. The present painter is an adaptation, not an exact reconstruction. The defaults button matches available setting declarations; it does not supply the missing painter, true glass blur, original 960x540 viewport behavior or independent fading item-name placement. Those source files or the full original project are needed for a faithful visual port. A screenshot helps establish expected appearance but cannot substitute for missing source behavior. AetherOSSettings remains excluded.

## Verification limits
JVM regressions cover recovery, appearance defaults, enabled-only editor discovery, normal viewport fitting, safe hidden-config fallback, removed selector/right-click hiding, bottom drag access and gradient composition guards. Existing offline rendering and native binding checks are rerun. No Minecraft launch or native GUI screenshot was available here, so actual visuals, dragging and other-mod compatibility need user testing. This release does not include the CS2 menu redesign.
