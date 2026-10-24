# solyanka — 0.6.0-dev

**Minecraft 26.2 exactly · Fabric · Kotlin · Java 25 · GPL-3.0-only**

This development build has **29 real registered modules (3 new HUDs in this batch)**. `/solyanka` opens the NoFrills-style GUI directly. Right-click settings remain embedded in the same Screen; every valid setting change saves immediately. There is no Save / Save & back button. All master modules default off.

**The whole requested mod is NOT complete.** Every requested Odin/NoFrills feature category now has an implementation or explicit adaptation, but this is not full upstream option/behavior parity. Ambience, Aspect, Fog, Inventory HUD, Item Name HUD and Skill Progress HUD are adapted from Stray; only Mimic Chest is implemented from impl.zip. AetherOSSettings is explicitly excluded at the user’s request. The rest of Stray and the archive remain tracked in `docs/FEATURE_MATRIX.md` and `audit/archive-inventory.json`. No placeholder toggles are advertised as working.

## Install / upgrade

1. Back up `config/solyanka.json` and any existing local market/waypoint files. Remove older solyanka JARs.
2. Use **Minecraft 26.2**, **Java 25**, **Fabric Loader 0.20.1+**.
3. Install **Fabric API 0.161.0+26.2**, **Fabric Language Kotlin 1.14.2+kotlin.2.4.21**, and **owo-lib 0.13.1+26.2**.
4. Install the ordinary **solyanka-0.6.0-dev.jar**, not the sources JAR. Optional: **Mod Menu 20.0.3**.

YACL is not required by solyanka. Odin/NoFrills/Stray themselves are not dependencies. Do not install multiple solyanka builds. Existing solyanka settings/unknown config fields are preserved.

## GUI

- `/solyanka`, `/solyanka config`, Right Shift or Mod Menu open the same GUI.
- Left-click a card to toggle it; right-click for settings. Back/Escape closes the embedded panel, not a save operation.
- Settings write atomically before live commit. Numeric sliders are bounded; invalid text drafts and write errors remain inline. Last-good disk/live state is retained.
- GUI input, scrolling, key capture and tooltips remain modal; underlying cards/search cannot be activated through settings.
- Commands: `/solyanka status`, `/solyanka toggle <module-id>`, `/solyanka gui` (HUD editor). The timed preview commands were removed in 0.5.2.

## New in 0.6.0 — XP bar and three Stray HUDs

- **Player Display → Hide XP Bar:** independent from Hide XP Level. SkyBlock and Player Display master gates apply. Only ExperienceBar background/progress extraction is cancelled; INFO_BAR/mount/locator bars are not cancelled.
- **Inventory HUD:** live 27-slot inventory grid; optional hotbar with selected-slot outline, armor/offhand, filled /41 count, minimal style and background. Item counts/durability use vanilla decorations. Display only: no click, pickup, swap or item automation.
- **Item Name HUD:** persistent styled main-hand name, hidden with an empty hand; optional suppression of the vanilla fading name. This is the Item Name HUD request, not the separate Held Item shader/CS2 cosmetic request.
- **Skill Progress HUD:** action-bar gain parsing for ratio, percentage and MAX/MAXED; twelve skill families including Dungeoneering/Catacombs alias, source XP tables, observed tab/Skills-menu levels, skill icon, gain/level fields and optional timeout. Percent-only readings stay percentages when no denominator is known. No fabricated absolute values or API calls. It resets on world change, disable and leaving SkyBlock.
- Each new HUD has x/y/scale sliders, visibility and text/background colors. `/solyanka gui` now includes **17 entries** and uses actual inventory-grid dimensions with representative editor samples. No master module is enabled by editing.
- **AetherOSSettings is excluded**, not pending. Original archive inventory is retained only as audit/provenance. See `audit/scope.json` and `docs/PENDING_FEATURES.md`.

All new masters default off. Other unfinished features remain pending. These are adaptations, not full Stray theme/font/blur parity. Inventory and item-name HUDs can run outside SkyBlock; Skill Progress and XP-bar suppression are SkyBlock-only. Gameplay HUDs hide in container/other blocking screens (editor shows samples). No in-game or GPU acceptance is claimed.

## Previous patch: 0.5.2 — compact settings and HUD editor

- Timer/Player HUD visibility and Position/Scale buttons now share one compact line where it fits; small windows use tight stacking.
- All numeric settings exposed by the GUI now use bounded sliders with live value labels: HUD positions/scales, environment, waypoint sizes, cursor timeout and chat limits. Text fields remain for names, commands, pack names and filters.
- All mode buttons support left-click forward, right-click backward, with wraparound. This includes Clock, Weather, No Render, waypoint type and keybind modifiers. Keyboard activation advances.
- All 15-second preview buttons, commands and timer state were removed. Runtime HUD behavior is otherwise unchanged.
- `/solyanka gui` opens a transparent dedicated HUD-layout canvas with the six timer HUDs, seven Player Display HUDs and Map Info. LMB drag moves; mouse wheel scales; RMB toggles individual HUD visibility; arrow keys nudge the selected HUD. Bottom selection cycles through overlapping/hidden elements (LMB next, RMB previous). Done/Escape closes; changes save immediately.
- Map Info is position-only: its scale/visibility remain controlled by its existing feature settings/master switch. The other editor elements have independent visibility and 0.5–4x scaling. Editing never automatically enables master modules.
- Editor uses labelled sample values even outside SkyBlock or with a master disabled. These are editor-only samples, not timed previews and not live dungeon data. Last-good live positions remain displayed on a failed write; errors stay visible.
- No additional backlog gameplay port is claimed in this UI-focused patch. The exact pending list is in `docs/PENDING_FEATURES.md`.

Manual acceptance: test at your normal GUI scale and smaller sizes; drag/reopen each HUD; overlap and select using the bottom picker; scroll-scale; toggle visibility; move Map Info; verify Clock Noon -> Dusk by LMB and Dusk -> Noon by RMB; test sliders and restart persistence. In-game/GPU testing has not been performed here.

## Previous patch: 0.5.1 — settings cleanup

- Transparent GUI/settings roots: no full-screen dimming layer; cards and panel remain readable.
- Colors show a small swatch and **Edit color**, opening an inline owo palette (including alpha).
- Clock, weather, waypoint type, Health Bar, Vignette and burger count use named cycling buttons, not numeric mode codes.
- Ambience, Aspect, Fog, Door Highlight, Map Info and each puzzle/mimic feature have scoped settings.
- Ice Fill, Blaze and Creeper Beams now have independent depth-test switches. Old shared values migrate automatically; beam connecting lines retain their existing through-wall behavior.
- Removed Player Display preview buttons, including nested HUD editors, and the success-status text. Inline save errors remain. (Timer previews were subsequently removed in 0.5.2.)
- Instant saving and same-Screen input isolation are unchanged. This patch does not add the unfinished Stray/archive features.

Build/unit/static checks only; no in-game or GPU/Sodium/Iris testing was performed here. Test palette dragging, scrolling, resizing, save/reopen, and each menu in Minecraft 26.2.

## Previous feature batch: 0.5.0

### Odin dungeon adapters

- **Dungeon Waypoints:** room-relative packs, 1x1 blue-marker and multi-tile geometry transforms, colored outlined/filled boxes, world titles, per-pack selection/editing, manual placement/removal, secret and manually initiated etherwarp hiding, collection reset. JSON and Base64+GZIP Odin pack import/export via clipboard. Bounded, validated, atomic local files; no online pack download or bundled secret pack.
- **Ice Fill:** source easy/optimized route data, rotated into recognized rooms; all three floors must match before a route is shown.
- **Blaze:** Higher/Lower ordering from armor-stand maximum HP, priority colors and connecting lines; entity-death exclusion.
- **Creeper Beams:** rotated bundled lantern-pair solutions; pairs disappear when either block is no longer a sea lantern.
- **Door Highlight:** Magical Map layout parsing, wither/blood doors, chat-tracked available keys, openable colors and key entity/spawn alerts.
- **Map Info:** tab/sidebar secrets, deaths, crypts, elapsed time, bonus flags, estimated score, remaining required secrets, room-secret samples and blood portal highlight. Missing inputs show `?`; Paul bonus is manual. This is not an authoritative completion guarantee.
- **Mimic:** baby-zombie death heuristic on F6/F7, baby-zombie outlines, prince/bat messages and optional explicit party announcements. A highlighted baby zombie is not proof of a mimic.

Previously implemented Tick Timers and Player Display remain. The source room/solver JSON and Odin BSD notice are bundled. See `docs/M4.md` for exact differences and unsupported behavior.

### NoFrills

- **Experiment Solver:** Chronomatron sequence memory, Ultrasequencer count ordering, Superpairs remembered reward overlays, next/following hints and optional wrong-click prevention. Private Island/menu gates, packet/tick snapshots and reset. No automatic clicking.
- **No Render:** independent particle/entity/GUI/overlay/fog/nausea/vignette filters, all off by default. Health Bar modes: Nowhere / Outside Rift / Everywhere. Vignette modes: None / Ambient / Danger / Both.
- **Price Tooltips:** auction/Bazaar/NPC/mote fields where providers supply them, stack/storage quantity, locally observed purchase costs and crafting recipes with cycle/unknown-price protection. **Network is separately opt-in and off by default.**

The NoFrills pricing endpoint returned HTTP 401 during audit. Fallbacks use public Hypixel Bazaar/item resources and Moulberry lowest-bin data; the latter also failed during audit. Missing prices stay unavailable, never zero. Motes require primary data. PricePaid records a manual confirmation click, not a server-confirmed purchase. Craft costs require complete locally observed recipes and ingredient prices. See M4.md for stale/error behavior and privacy.

Existing command keybinds/shortcuts, Chat Tweaks, Info Tooltips, Animation Fix, Middle Click Fix/Override, No Cursor Reset and visual-only No Loading Screen remain. Loading readiness/input blocking are deliberately preserved.

### Stray and archive

- **Ambience:** client-only clock presets, clear/rain/thunder/snow and precipitation intensity. No integrated-server time/weather mutation.
- **Aspect:** matching projection/culling horizontal multiplier; 1.0 is vanilla.
- **Fog:** atmospheric distance/color/density with liquid/status fog preserved. No Render fog suppression wins. Sodium uses native uniforms only: the upstream custom Sodium shader patch is NOT included; Iris/shader parity is unverified.
- **Mimic Chest (impl.zip):** loaded-chunk trapped-chest scan every 20 client ticks and depth-tested filled block-shape overlay, configurable signed ARGB color. Display refreshes every five client ticks. No chunk loads; not every trapped chest is a mimic.

## Waypoint quick start

1. Enable Dungeon Waypoints; right-click settings. On first use an empty `default` pack is created under `config/solyanka-waypoints/`.
2. To import an Odin pack, copy raw JSON or its Base64+GZIP share string. Enter a **new** pack name and click **Import clipboard**. URLs are not fetched.
3. Set **Selected Packs** to comma-separated existing names and **Edit Pack** to the editable name. Valid settings save instantly. Click **Reload packs** after editing files outside the game.
4. In a recognized dungeon room, enable **Allow Edits**, close the GUI, and right-click a block within normal reach to add/remove a waypoint. Editing consumes the click. `/solyanka waypoint add <title>` adds/replaces the targeted waypoint with a title. Disable Allow Edits for normal gameplay.
5. Types: 0=None, 1=Normal, 2=Secret, 3=Etherwarp. Select block bounds or custom dimensions. Colors are signed Java ARGB integers (default green `-11141291`); filled/depth/title settings are independent. Title size is configurable.
6. Export a named pack to the clipboard. Create/import do not automatically change selection. Rename/delete apply only to inactive packs; switch Selected/Edit Pack first. The only pack cannot be deleted. Editing requires an existing readable pack; corrupt files are not silently overwritten.

This is a solyanka embedded editor, not Odin's Compose editor/HUD or editor hotkey cycling. Etherwarp uses a bounded vanilla raycast, not Odin's full tuned targeting validator. Manual interactions are observed, never automatically executed. Review server rules for client assistance/ESP/click filters before use.

## Validation boundary

`./gradlew clean build` compiles both source sets and runs pure regression tests. See `validation/summary.json` for exact counts, artifact checksum and static checks. **No full Minecraft launch, Mixin application in a launched client, Hypixel session, GPU visual test, Sodium or Iris acceptance has been performed here.** Successful compilation/static target checks are not proof of in-game behavior.

Run the new feature checklist in `docs/TESTING.md` with minimal required mods first, then your actual performance/shader setup. Toggle modules one at a time. Report module ID, steps, screenshot and sanitized latest.log/crash report.

## Build / source

Use a full JDK 25 and run `./gradlew clean build` (Windows: `gradlew.bat clean build`). Gradle 9.7.1, Loom 1.18.4, Kotlin 2.4.21; official unobfuscated 26.2 names, no Yarn/legacy mappings. Dev launch: `./gradlew runClient` in a graphical environment.

Source ZIP includes source, wrapper/build scripts, notices/licenses, audit/source locks, validation evidence and ordinary/sources JARs in `dist/`. Dependencies and upstream frameworks are not vendored. `NOTICE`, `docs/M4.md`, `docs/FEATURE_MATRIX.md` and `docs/ROADMAP.md` describe attribution, adaptations and remaining work. Historical M1–M3 documents describe their original release scope, not the current feature count.
