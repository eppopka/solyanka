# In-game handoff — 0.5.0-dev

**Not performed here.** Compilation/tests/static signatures do not establish Mixin transformation, in-game render correctness or Sodium/Iris compatibility. Use exact MC26.2/Java25/runtime pins in README. Remove older JARs; back up config and packs first.

## New modules: acceptance checklist

1. Open /solyanka and every new settings panel, change a value, inspect disk immediately, reopen/restart. No Save button/new Screen. Invalid numeric fields must retain an error; last-good live value survives. Test resize/modal input and old bindings/chat/HUD regression.
2. Experiments: each of Chronomatron/Ultrasequencer/Superpairs on Private Island; repeated colors, next round, duplicate rewards, hidden/visible pair/powerup, bulk/per-slot updates, wrong click block on/off, close/reopen/disable/world change. Verify no extra outgoing clicks or real slot mutation.
3. No Render: test every option individually (all start off). Include Rift exceptions, slayer boss cocoon heuristic, mob name/fire layers, liquids/status fog, recipe/effects/tooltips, warning vs ambient vignette, nausea and selected-name/HUD interactions with Player Display.
4. Prices: module on + Network off must not request data; enable network and check source status/time. Primary401, Moulberry failure, missing IDs, stale successful snapshots, stored quantities, stack values, motes unavailable, crafting unknown/cycle, failed purchase after click, corrupted/unwritable local market file. No missing price shown as zero.
5. Rooms: recognized 1x1 plus all straight/square/L rotations and partially unloaded multi-rooms. Unknown core/rotation must not draw guessed routes. Reconnect/change floors must clear old geometry/state.
6. Ice Fill easy/optimized all variants/floors, Blaze high/low ordering/kills, Creeper Beams active/completed pairs and depth toggle. Test F1 and camera movement/GUI scale; watch for GPU artifacts/pose leaks.
7. Waypoints: new empty default, import/export raw/compressed Odin pack, two selected packs, edit inactive pack protection, custom color/size/shape/title/depth, manual add/remove, named command, per-room delete, secret chest/lever/essence and matching secret item/bat sound hiding, manually used etherwarp and timeout, reset collected, room/world reset. Verify no normal block use in edit mode and no automatic movement/click. Import corruption/oversize/path names must reject without overwriting good files. Rename/delete inactive packs only; only-pack deletion blocked.
8. Door Highlight: valid Magical Map vs unrelated map/missing map, wither/blood key chat and spawning entities, consumed counters clamp, openable color, opened door disappears, world reset. Map Info: missing inputs ?, floor/Master thresholds, deaths/puzzles/crypts/mimic/prince/bats, manual Paul off/on, 300 local alert only once, Blood portal rotation, room secret first-sample guard and boss hiding. Estimated score is not a guaranteed boss-entry decision.
9. Archive Mimic Chest: dungeon including boss, configurable alpha/color, loaded chunk boundary, no forced chunk requests, removal within five ticks, disable/re-enable/world change.
10. Ambience: every time/weather preset, no-change, snow/intensity, disable/rejoin and integrated-server simulation unchanged. Aspect: extremes plus resize/FOV/culling entity/terrain consistency, disable restores vanilla. Fog: start/end/color/density, water/lava/powder snow/blindness/darkness preserved, No Render wins.
11. Repeat minimal-mod acceptance with actual MC26.2 Sodium/Iris/shader versions. Native Sodium fog differs from upstream custom shader patch. Do not infer compatibility from compile success.

For reports: include module ID, options, exact world/room/menu, dependency versions, sanitized latest.log/crash report, screenshot and reproduction. All new masters start off; enable one at a time. Review server rules before assistance/ESP/click filtering or party announcements.

## Earlier-release regression checklist (historical wording follows)

# In-game handoff — Odin + NoFrills feature batch 0.4.0-dev

**Not yet performed here.** Compilation and unit tests do not establish in-game visuals, mixin application or server messaging behavior.

## Upgrade

1. Back up `config/solyanka.json`; remove the older solyanka JAR.
2. Exact Minecraft 26.2, Java 25, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2, Fabric Language Kotlin 1.14.2+kotlin.2.4.21.
3. Install **owo-lib 0.13.1+26.2**. YACL is no longer required for solyanka; do not remove it if another mod needs it.
4. Optional Mod Menu 20.0.3; install the ordinary 0.4.0 JAR, not sources.

## GUI / instant-save acceptance

- `/solyanka`, `/solyanka config`, Right Shift and optional Mod Menu must open the same category GUI as before.
- Right-click each feature: its embedded panel appears over the existing GUI, not as a different Minecraft Screen. There must be no Save & back, Save, Done (stage), Cancel or separate AlertScreen.
- With the panel open, clicks outside it, scrolling, Tab/Shift+Tab, typing and tooltips must never act on background feature cards/search. Esc/Back returns to the main view; keybind-editor Back returns to the bind list without replacing the outer Screen.
- Toggle/rename/add/edit/delete command rows, change modifiers/filters, bind keyboard/mouse/unbind, edit chat limits and tooltip toggles. For each valid action, inspect `config/solyanka.json` **before closing the panel** and verify the new value is present. Live module behavior should follow immediately.
- Back/Escape/closing the GUI/restarting must retain successful edits; these are not cancel/undo actions.
- Temporarily empty a numeric field or an enabled command/alias. Red inline Not saved status should appear; disk/live settings remain last-good. Correct it: the pending edits save automatically without any button. Until corrected, other pending changes in that feature editor remain held. Closing drops only invalid pending edits, not earlier saved edits.
- Test duplicate/reserved aliases, cycles, invalid limits and unwritable config permissions. No crash, dialog replacement, partial disk overwrite or false saved status. Restore permissions/correct values, then edit again to retry.
- Key capture Escape must only cancel capture. A captured printable key must not also type into a previously focused textbox. Captured mouse input must not activate the background GUI.
- Scroll lists/panels, resize/GUI-scale/fullscreen, and repeatedly open/close/list/edit/back. Verify search view remains intact, panel values survive resize, scroll works and native cursors are released without stale handles.
- No new runtime dependency versus 0.3.0; old command/feature settings and unknown config fields must survive.

## Command/input regression

Use harmless test targets first: commands/chat can reach the server.

- Try a shortcut target /solyanka status and a keybind to that command; enable master module and entry separately.
- Tap/hold keys and mouse: one send per initial press, correct modifiers, no repeated sends/vanilla side effects after consumed presses.
- No binding while typing in GUI/search/chat/anvil; container use only when opted in; unfocused/disconnected input ignored.
- Check standard Hypixel Area/Dungeon/player-class tab labels and configured filters. Unknown/dead/mismatched context blocks filtered bindings.
- Check alias arguments, chains, cycle rejection, unrelated server commands unchanged and other client commands protected. Alias completion nodes remain pending.
- Change worlds/rejoin and check no stale owned input/context state.
- Repeat with actual 26.2 Sodium/Iris versions and other input/client-command mods. Rendering compatibility and signed message dispatch remain unverified until tested.

Report exact dependency versions, Java/OS, latest.log/crash report and reproduction steps. Remove personal command text/account secrets before sharing config/logs.


## New: Chat Tweaks

1. Right-click Chat Tweaks, set full-message and copy-line keys to different harmless keys (or mouse buttons); changes save immediately; left-click the module on.
2. Open chat, hover bottom/top messages, wrapped lines, messages partly outside the viewport, and scrolled history. Full copy should include the whole original message; line copy only the hovered wrapped line. Paste into a local editor to check spaces, Unicode and newlines. Do not paste private messages publicly.
3. Change Minecraft chat width/height/scale/line spacing and GUI scale/window size. Repeat hover checks near all rectangle boundaries. No copy over the input field or outside the chat history.
4. Turn Trim/Message On Copy on/off; check preview 0, 1, 50, 512 and Unicode truncation. If keys match, Copy Line wins. Holding a copy key must not repeatedly copy or leak matching owned presses into chat.
5. Copy keys must do nothing in world/settings/container screens and when unfocused. Existing command binds should still never execute in ChatScreen.
6. Enable Keep History; disconnect/reconnect/change server, then verify received chat remains. F3+D must still clear it. Disable Keep History and repeat disconnect to verify clearing returns.
7. Enable Extra Lines with 1000/5000; generate harmless local test messages, scroll beyond vanilla history, then lower to 100 and add another message. Both stored and rendered queues should respect the configured cap. Do not spam a public server for this test. Sent-command history is intentionally unchanged.
8. Change valid settings, leave without any Save action and reopen/restart: values and key device identities must persist. Invalid numeric fields should show an inline error without changing last-good live/disk settings.

## New: Info Tooltips

1. Enable Info Tooltips and its individual options. Check an ordinary vanilla item (no added lines), a SkyBlock item with only ID, dungeon items with quality 49 vs 50 and tier, timestamp item, dyed item, donated/non-donated museum item, and unknown/missing fields.
2. Check Created Date against your local timezone/locale; dye hex must be lowercase and zero-padded. Perfect quality is red/bold. Missing data must not create fake values.
3. Test pet, rune, single-enchant book, multi-enchant fallback, potion, max-quality dungeon and attribute-shard items. In particular, source-lore shard IDs should take priority over the displayed attribute name. Compare market IDs to NoFrills source semantics, not prices.
4. Museum is an explicit network opt-in. With it off, this mod should not request the Hypixel resource. With it on and in-world, allow an initial download; inspect known base/mapped museum items. Remote resource failure must not freeze the UI or show invented categories. A failed refresh retains a last-good session cache and waits ten minutes before another attempt.
5. Toggle off/restart: session cache is not disk-persisted. No API key is needed. Item tooltips must work offline for all other options.
6. Check tooltip ordering/layout with other tooltip mods; test inventories, creative/search and containers. Repeat the new hooks with your actual 26.2 Sodium/Iris stack; report exact versions.

The remaining Odin features (apart from Tick Timers and Player Display), original archive/Stray features, Price Tooltips, No Render and Experiment Solver remain pending. New mixins have only compile/static checks until these in-game tests pass.


## New Odin timers and player HUDs

1. Enable Tick Timers and Player Display separately. Open their embedded settings. Check independent HUD enable, X/Y, scale, player RGB colors, all format/overflow/visibility toggles save without leaving. Try invalid values, restart, resize, different GUI scale and F1. No new save/staging screen is allowed.
2. Use Preview buttons or `/solyanka preview timers` / `/solyanka preview player`. With the relevant master module enabled, local sample HUDs should appear for 15 seconds; they must not start a real timer, send a server chat/command or persist sample stats. HUD-disabled items should stay hidden. Adjust position/scale/color during preview.
3. In real F7/M7 verify all exact Storm/Goldor chat triggers; pad/Goldor cycles, once-only PY, Storm elapsed count and lightning countdown. Enable ticks and compare to actual nonzero server pings. No tick progress without the server signal. Floor/sidebar/boss detection must fail closed outside a recognized dungeon/boss arena. Test world transfer, reconnect, module disable/re-enable and repeated boss runs.
4. In F3/M3 verify Professor line, 106-tick countdown and subsequent 100-tick Fire Freeze prompt. No ability is automatically used.
5. Check real SkyBlock action-bar PUA/legacy health/mana/overflow/defense/vitality. Health follows vanilla ratio, speed base movement speed, source integer-defense EHP formula retained. Zero/combined/separate overflow and icon choices must work. Missing vitality should disappear. Unrecognized event/secret text should remain.
6. Verify selected action-bar components and vanilla armor/hearts/food/XP-level hide only in SkyBlock while Player Display is enabled. Disable it/leave SkyBlock: vanilla HUD must immediately return. XP bar is intentionally unchanged. Compare rich-style behavior when text is partially stripped.

## New NoFrills utilities

- **Animation Fix:** with and without SkyBlock-only guard, compare repeated sneaking/swimming animation resets from local-player POSE metadata. Other-player poses and non-POSE metadata must remain normal. Disable restores vanilla behavior.
- **Middle Click Fix:** in survival container GUI try the configured pick-item/middle input. It should use vanilla CLONE packet handling, not create creative items. Test player inventory, real chest and known SkyBlock menu; check other inventory mods.
- **Middle Click Override:** review current server rules first. In harmless eligible SkyBlock menus test a manual left click with empty carried stack; enable debug to see one local replacement. Confirm exact/paginated blacklists, Auction/Hex/phone/etc fragments, Rename Loadout, actual inventory items, shift/right/drag/throw/carried-item paths remain unchanged. Test Fix and Override separately/together and ensure one original click operation, never automatic repetition.
- **No Cursor Reset:** close/reopen GUIs at different cursor positions, wait expiry with no GUI, keep GUI open beyond expiry, set 0, resize/high-DPI/fullscreen, Spirit Leap, module disable and world transfer. Cursor should stay inside current game window and no position should leak across resets.
- **No Loading Screen:** test remote-only on/off with remote/LAN/singleplayer transfers. Only LevelLoadingScreen foreground/background should be suppressed. Vanilla loading readiness, tick/close behavior and input blocking must remain; resource-pack/error/disconnect screens must still be visible. This is visual-only and does not guarantee a ready world will render instead of a black frame.
- Repeat all new hooks with actual 26.2 Sodium/Iris and other HUD/inventory mods. Provide exact versions and logs. Static bytecode matches do not prove runtime transformation or mod-order compatibility.
