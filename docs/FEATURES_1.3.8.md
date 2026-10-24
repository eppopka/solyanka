# 1.3.8 — Knife assets, ItemAura depth, HUD workflow, search and styled hotbar

## CS2 Knives

The previous release deliberately supported only a local CS2 installation. This release also supports the pinned pack published by Stray's cs2-data branch. No Valve assets are in the mod JAR or source ZIP, and enabling the module never starts a download.

1. Enable CS2 Knives; open its settings.
2. Choose Download knife assets…, review the third-party host/size notice, then Confirm download (134.6 MB). This selects Downloaded Stray pack as the source. A progress line and Cancel asset download are available. Refresh list / status after completion if the picker was already open.
3. Once downloaded, a client tick reloads the library. Select a knife/finish and hold an eligible item. The default applies to swords/axes; other Apply To modes are available. Inspect is F by default.
4. If loading fails, inspect `/solyanka knifestatus` and latest.log. Canceling a download leaves the old verified cache intact and removes the incomplete file.

Source options: Auto uses a local install first, then an already-cached verified pack; Local CS2 install never falls back; Downloaded Stray pack uses only the verified cache. Auto is offline-safe and does not download. Successful downloads trigger a generation-safe reload. Cached assets work offline, including after restart. Existing manually obtained copies of this exact pinned pack can be placed at config/solyanka/cs2.pack.

Published manifest: https://raw.githubusercontent.com/camberX/Stray/cs2-data/cs2.json
Pinned size: 134557137 bytes. SHA-256: 8e726f0f3f6429c27841ccae3a37d050f9a620f8a83c4018ce571d1ba4152166. Parts: cs2.pack.00, cs2.pack.01, cs2.pack.02. If upstream replaces the data, verification fails rather than accepting an unreviewed pack. Network access occurs only after confirmation; the host sees the user's IP, with no account credentials sent. The game assets are not covered by solyanka's GPL license; use only as permitted by the respective owners' terms.

Download work runs on a daemon, has cancellation and bounded writes, refuses redirects/symlink destinations, verifies exact size/hash, validates the pack index and publishes the file only after validation. The pack reader bounds file/index/entry/texture sizes, validates offsets/types/duplicate paths and rejects stalled/truncated decompression. Local install decoding remains available. No real game assets were downloaded or exercised on the build machine; actual download/render acceptance still needs user testing.

## ItemAura Depth Sorting

Fixed the comparison: Minecraft 26.2 uses reversed depth, with a zero clear value and native GREATER_THAN_OR_EQUAL comparison. The old LESS_THAN_OR_EQUAL rejected particles against empty pixels while accepting those behind the model—the reported silhouette-only behavior. The corrected pass keeps depth writes off, uses the already-rendered hand depth and stays before the hand view-matrix pop. Offset/radius can again place particles around the item; those behind its actual pixels are occluded, not particles against empty background. Depth Sorting off remains always-visible. This fixes the confirmed comparison error; native in-game rendering is still unverified here.

## HUD layout workflow

Removed numeric HUD X/Y/overall-Scale controls from timer/player, extra HUDs, scoreboard, map, Move Hearts, Stopwatch and MoveHotbar settings. They now use Open HUD Editor buttons, which invoke the same local screen as `/solyanka gui` without sending a server command. Timer rows go straight to the editor; player colors remain configurable. Existing positions/scales are retained. Effect sizes, world-space waypoint dimensions, nametag scale, particle offsets and CS2 3D view offsets are NOT screen-layout settings and remain.

The editor only lists HUDs whose parent module is currently enabled and whose individual HUD visibility is on; Route Progress also respects its own progress switch. Disabled functions cannot be accidentally previewed/dragged. The full internal catalog still exists for safe config migration. An empty editor is valid and explains that modules/HUDs must be enabled in settings. Drag LMB, scroll to resize, RMB to hide; hiding removes it from the current list and safely selects another remaining entry. Samples still do not start timers, generate fake live data or enable modules.

## GUI search

Ctrl+F queues modal close at the normal safe frame boundary, focuses the existing feature search and selects the current query. It works even with a settings panel open. Search… (Ctrl+F) is a placeholder only while the query is empty; typing clears it, clearing text restores it. A tooltip also explains the shortcut. No screen recreation on each keystroke or forced world darkening.

## MoveHotbar

Added archive-inspired Vanilla, Minimal, Glass and Neon style presets; horizontal/vertical layout; slot gap, padding, rounded corners and border thickness; background/opacity/gradient/direction; accent and smooth sliding selection; selection opacity/radius; shadow/glow strengths and radius; animated shimmer/rainbow/effect speed; item icon size/pickup animation; offhand and attack indicator slots; position reset and horizontal item-name/XP followers. Counts, durability and cooldown decorations use native item APIs. Styled editor previews share the live layout and include current offhand/attack cells. Spectator and disabled behavior fall back to the original HUD, and F1 still hides it.

Existing configs default to Vanilla for compatibility. Vanilla horizontal retains the original hotbar rendering; choose a styled preset to use the custom painter. Preset changes reset the appearance defaults (not layout/follower behavior), then all appearance controls can be customized. Glass means translucent gradient chrome, NOT a framebuffer blur. Native fonts/rounded geometry replace archive UI dependencies; the archive's AetherOSSettings is deliberately NOT included. There is no claim of pixel-perfect archive parity: the separate independently movable fading item-name follower, reference-viewport migration and exact helper/shader painter are not copied. The existing Item Name HUD provides a separate persistent-name module. Extremely large scale/vertical layouts can exceed very small viewports; position is clamped, not automatically shrunk. The native Vanilla main-track editor box does not include native offhand/indicator extensions.

## Acceptance and exclusions

JVM tests, synthetic pack decoding/corruption checks, native ABI/bind-layout guards and software depth/shader checks are in validation/. None constitutes a Fabric launch, actual Mixin application, native GUI visual QA, Windows capture test, real CS2 asset/download test, Sodium/Iris compatibility test or FPS measurement. Please verify in-game: startup, knife asset workflow and animations; aura behind/in front/outside item; empty/enabled-only HUD editor; dragging/resizing/hiding; Ctrl+F and placeholder; every hotbar style/layout, offhand/left-handed selection, item decorations and spectator/F1 behavior.

Preserves 1.3.7 PiP selection, local Held Item clock and compact scoreboard fixes. All new modules remain disabled by default. Archive Cosmetics, AutoRag, AutoReel and AetherOSSettings remain excluded. No full source-parity claim.
