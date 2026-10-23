# In-game handoff — NoFrills GUI update 0.2.1-dev

**Not yet performed here.** Compilation and unit tests do not establish in-game visuals, mixin application or server messaging behavior.

## Upgrade

1. Back up `config/solyanka.json`; remove the older solyanka JAR.
2. Exact Minecraft 26.2, Java 25, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2, Fabric Language Kotlin 1.14.2+kotlin.2.4.21.
3. Install **owo-lib 0.13.1+26.2**. YACL is no longer required for solyanka; do not remove it if another mod needs it.
4. Optional Mod Menu 20.0.3; install the ordinary 0.2.1 JAR, not sources.

## GUI acceptance

- Run **/solyanka**: it must open the NoFrills-style category GUI directly after chat closes, without requiring /solyanka config.
- Verify /solyanka config, Right Shift and Mod Menu open the same GUI. Close to the world/title/Mod Menu correctly.
- Check General/Core columns, blue headers, active accent/text, dark surfaces, flat controls, green/red toggles and search. Search names/descriptions with mixed case/spaces; clear and confirm all three real modules return.
- Left-click cards to persist toggles; right-click command cards to open settings. Diagnostics has no fake settings screen.
- Scroll individual categories, horizontal overflow and long settings lists. Check different GUI scales, window resize, full-screen changes, very small windows and repeated open/close cycles for clipped/overlapping controls, stale listeners or native-cursor issues.
- Add/rename/toggle/edit/delete keybinds. In editor, capture keyboard and mouse, cancel/repeat capture, unbind, set modifiers, GUI permission and class/island CSV filters.
- **Done (stage)** must not write yet. **Save & back** on the list writes; Cancel / Escape from the root list abandons draft changes. Editor Cancel discards that editor's unstaged changes while retaining other list draft edits.
- Add/edit/delete shortcuts inline. Check duplicate/reserved aliases, cycles and empty enabled rows fail with a local error and preserve live config. Correct them and retry.
- Restart and confirm valid 0.2 config entries/IDs, unknown fields and active states survive unchanged.

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
