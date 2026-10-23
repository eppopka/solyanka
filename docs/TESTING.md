# In-game handoff — 0.2.0-dev

**All items below remain user acceptance work.** Unit tests/builds are not an in-game launch test. Commands and chat may reach the server; use harmless test messages first.

## Setup / upgrade

- Back up config; remove the 0.1 solyanka JAR. Do not install both builds.
- Exact Minecraft 26.2, Java 25, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2, Fabric Language Kotlin 1.14.2+kotlin.2.4.21.
- **Install YACL 3.9.7+26.2-fabric (new required dependency).** Mod Menu 20.0.3 is optional.
- Install the ordinary 0.2 JAR, not the sources JAR.

## Startup and settings

1. Launch a minimal mod profile; check log for `solyanka 0.2 initialized for Minecraft 26.2` and no mixin errors.
2. With Mod Menu installed, open its solyanka config button from the title menu. Repeat without Mod Menu to check optional-dependency loading.
3. Join a test world; press Right Shift and run `/solyanka config`. Confirm the chat screen does not immediately close the settings screen. Rebind the settings key in Controls.
4. Add disabled bind and shortcut rows. Check Capture keyboard, mouse capture, Escape cancel, Backspace unbind, edit/name and delete.
5. Confirm ordinary Cancel discards unsaved field changes. Save & add/remove commits all pending edits by design. Try invalid/cyclic/duplicate aliases; confirm error screen and unchanged runtime/file.
6. Restart and verify saved settings/unknown config fields. Check a backed-up 0.1 config migrates additively.

## Commands / input

1. Enable both utility modules. Create an enabled shortcut `hello` -> `hello`; run `/hello test` in a harmless test context and check one plain chat message `hello test`.
2. Change it to a harmless local command, such as `/solyanka status`. Confirm immediate live application without changing lobby.
3. Bind a non-conflicting keyboard key to `/solyanka status`; enable the row. Tap and hold: exactly one action per press, no repeat spam or vanilla bound action leaking through.
4. Test mouse buttons, release after opening a screen, disable/re-enable and focus loss/recovery. Check ordinary movement/mouse controls still work.
5. Check ANY/NONE/SHIFT/CTRL/ALT, extra modifiers and Caps/Num Lock. First matching enabled bind wins; unbound/disabled rows do not fire.
6. Verify no bind runs while typing in chat/settings/anvil. Test ordinary container opt-in/global opt-in and a focused container search field, including third-party container mods.
7. On Hypixel, compare tab `Area:`/`Dungeon:` and actual player class labels against configured filters. Empty filters allow any context; absent/dead/mismatched context must block filtered binds. Do not assume nonstandard tab formats are recognized.
8. Test alias arguments, alias chains and cycle rejection. Existing client commands and `/solyanka` must retain control. Check an unrelated server command is untouched. Alias completion nodes are not implemented yet.
9. Disconnect/rejoin/change worlds and check no stale filter/owned-input state.

## Compatibility

Repeat startup, captures, settings and commands with actual 26.2-compatible Sodium/Iris/shader builds. No renderer feature is ported in this milestone. Check interactions with other client-command/input mods; mod-order behavior and signed chat dispatch remain unverified until these tests.

For issues, provide exact mod/Java/OS versions, latest.log/crash report, config with personal command text removed, and reproducible steps. Never include account/session secrets.
