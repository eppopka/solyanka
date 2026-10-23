# solyanka — NoFrills GUI update (0.2.1-dev)

**Minecraft 26.2 exactly • Fabric • Kotlin • Java 25 • GPL-3.0-only**

This update replaces YACL with an adapted port of the NoFrills click GUI. Implemented modules remain command keybinds, command shortcuts and diagnostics. All other requested gameplay/rendering features remain in the backlog. **The new GUI has not been launched or visually checked in Minecraft here.**

## Install / upgrade

1. Back up `config/solyanka.json`; remove the previous solyanka JAR. Do not install multiple solyanka builds.
2. Use Minecraft **26.2**, Java **25**, Fabric Loader **0.20.1+**.
3. Keep **Fabric API 0.161.0+26.2** and **Fabric Language Kotlin 1.14.2+kotlin.2.4.21**.
4. Install **owo-lib 0.13.1+26.2** (Fabric). It is the new required external UI dependency.
5. Install `solyanka-0.2.1-dev.jar`. **Mod Menu 20.0.3** is optional.

**YACL is no longer required by solyanka.** Keep it if another installed mod needs it. Existing 0.1/0.2 config remains compatible; no keybind/shortcut reset is required.

## Open the GUI

- **`/solyanka` opens the GUI directly**, on the next client tick after chat submission.
- `/solyanka config` remains an equivalent alias.
- **Right Shift** opens it from the world; rebind this key in Minecraft Controls.
- Optional Mod Menu configuration button opens the same GUI, including from the title menu.

The main GUI ports NoFrills' horizontal category columns, blue headers, dark/flat surfaces, active accent bars, text colors, search and category scrolling. **Left click a feature to toggle it; right click for its settings.** Only implemented features appear; there are no fake working toggles for the remaining backlog.

## Command settings

- Main feature-card toggles save immediately. All modules and new entries default off.
- Right click Command Keybinds or Command Shortcuts to edit entries.
- List changes are staged. Add/Delete do **not** save immediately in this update.
- In the keybind editor, **Done (stage)** returns edited values to the draft list; **Save & back** on the list validates and persists the profile.
- Cancel / back (or Escape outside capture) discards the current editor's unstaged changes, or the entire list draft when leaving the root list without saving.
- Capture is inline like NoFrills: click the keybind button, then press a main key or mouse button. Escape cancels capture; Backspace unbinds. Configure Ctrl/Shift/Alt separately.
- `/`-prefixed targets are commands; other targets are ordinary chat. They can reach the server. Configure and test with harmless targets first.
- Keyboard and mouse identities are separate; only initial presses send. First matching row wins. Container execution is opt-in; chat, anvils, settings, text fields, unfocused/disconnected clients are blocked.
- Class/island filters use current standard tab-list labels, not full dungeon context. Unknown filtered context blocks the bind; empty filters are unrestricted.
- Shortcut names are exact/case-sensitive tokens. Arguments append, changes apply live, cycles and excessive depth/length fail locally. Unrelated server commands are untouched.
- `/solyanka` and other registered client command roots retain precedence. Alias completion/help nodes are still pending.

Other commands: `/solyanka status`, `/solyanka toggle utility.command_keybinds`, `/solyanka toggle utility.command_shortcuts`, `/solyanka toggle core.diagnostics`.

## Build

Install a full JDK 25, set JAVA_HOME, then:

```sh
./gradlew clean build
```

Windows: `gradlew.bat clean build`. Dev client: `./gradlew runClient` in a graphical environment. Install the ordinary `build/libs/solyanka-0.2.1-dev.jar`, not the sources JAR.

Pinned build tools: Gradle 9.7.1 (checksum pinned), Fabric Loom 1.18.4, Kotlin 2.4.21. This Minecraft target uses official unobfuscated names; no Yarn/remapping configuration is added. Runtime mods are external, not bundled.

## Source and documentation

- `src/main/kotlin/dev/solyanka`: core lifecycle, typed command profiles, persistence and pure GUI catalog/drafts.
- `src/client/kotlin/dev/solyanka/client/ui/nofrills`: adapted NoFrills click GUI, flat controls and settings panels.
- `src/client/kotlin/dev/solyanka/client/ui/ConfigUi.kt`: solyanka profile/settings adapter.
- `src/test/kotlin`: lifecycle/config/command/input/context tests plus GUI draft/search tests.
- `docs/GUI_PORT.md`: upstream file trace and intentional differences.
- `docs/TESTING.md`, `docs/VALIDATION.md`: actual validation boundary and your in-game checklist.
- `docs/FEATURE_MATRIX.md`, `docs/ROADMAP.md`: all requested feature scope remains tracked.
- `audit/`: source locks, attachment inventory, license text and dependency evidence.

Project code and NoFrills adaptations are GPL-3.0-only; full project source/build scripts and notices accompany this delivery. See LICENSE/NOTICE. External owo (MIT), Mod Menu (MIT) and Fabric dependencies are separate. NoFrills itself is not required; its complete feature/config/event framework is not bundled.
