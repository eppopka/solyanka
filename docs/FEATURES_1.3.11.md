# solyanka 1.3.11 — Original eppopkaaddons hotbar painter

## Original source port
The supplied eppopkaaddons-26.2-project.zip contains the previously missing helpers. HotbarPainter and GlassRenderer are now ported with their implementation bodies unchanged; only packages/imports and SPDX attribution were adapted. Pure HotbarLayout, HotbarBox, HotbarViewport and HotbarSelection are retained too. Required Rect and drawRect are extracted without the unrelated global clocks or AetherOSSettings.

- Original Minimal, Glass, Neon and Vanilla backgrounds; native hotbar sprite/cropped cells for Vanilla.
- Original Glass translucent coating, top sheen and per-slot wells. The source itself does not use framebuffer blur.
- Original per-pixel rounded/clipped gradients and travelling shimmer, rounded ring borders that do not flatten the interior gradient, layered whole-panel glow/shadow and selection outline.
- Original floating-point selection smoothing; native Minecraft item icons, counts, durability, cooldowns and pickup animation remain.
- Geometry uses the original fixed four-pixel auxiliary gap and original offhand/attack-indicator placement, shared by editor and gameplay. Auxiliary wells no longer add the old oversized padding gap.
- All four styles route through the original painter; gameplay still draws the hotbar only once and delegates native tooltip/XP elements.

## How to get the original look
Enable Move Hotbar, open settings and select **Use Original Hotbar Defaults**, then **Restore Hotbar** to center it if needed. This selects Glass and restores the source's declared colors/opacity, glow/shadow/gradient, spacing and item defaults. It also enables **Archive Reference Sizing**, using the original 960x540 reference scale. The button intentionally resets appearance and follower options; existing position/scale are retained until Restore Hotbar is pressed.

Existing configurations do not get silently reset. Archive Reference Sizing is opt-in for old configs, so old absolute GUI sizes/positions remain usable. Position storage remains solyanka's native GUI coordinates; the reference scale only affects display size. Editor drag/scroll convert effective size back to stored requested size. The selector removal, non-destructive right click, top-right editor controls, instant saving and recovery buttons from 1.3.10 remain.

## Scope differences
This is the actual source painter and geometry, not a claim of entire-module parity. The archive's independent fading item-name HUD and global AetherOS animation-speed/reduced-motion controls are not imported. Solyanka keeps its existing tooltip/XP follower switches, native-pixel position storage, safe fitting/recovery policy and simple attack progress track. Pure original viewport and smoothing helpers are preserved, but the whole original HUD framework is not imported. No other modules from the uploaded project are enabled or added. CS2 menu work is not included.

## Verification and provenance
Original selected files, source archive SHA-256 and per-file SHA-256 are stored in audit/original-hotbar-source. Tests compare the painter, rounded renderer and pure helper bodies to their audited originals, cover geometry combinations, reference scaling, drag/resize conversion and old-config behavior. Existing JVM tests and offline checks are rerun. The original CC0 license is included under licenses.

No Minecraft launch, native GUI screenshot or real GPU/Sodium/Iris acceptance was available in this workspace. Source parity and compilation do not prove visual parity or frame rate; please test the four styles, both orientations, offhand/attack settings, dragging and resolution changes in-game.
