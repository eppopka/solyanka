# solyanka — command/settings milestone (0.2.0-dev)

**Minecraft 26.2 exactly • Fabric • Kotlin • Java 25 • GPL-3.0-only**

Implemented/build-tested: YACL configuration UI, command keybinds, command shortcuts and the foundation lifecycle/config infrastructure. All other requested features remain in the backlog. **No in-game launch or multiplayer validation has been performed here.**

## Install / upgrade

1. Back up `config/solyanka.json`; remove the old solyanka JAR. Do not load both versions.
2. Use Minecraft **26.2**, Java **25**, Fabric Loader **0.20.1+**.
3. Install **Fabric API 0.161.0+26.2**, **Fabric Language Kotlin 1.14.2+kotlin.2.4.21** and **YACL 3.9.7+26.2-fabric**. These external runtime mods are not bundled.
4. Add `solyanka-0.2.0-dev.jar`. Optionally install **Mod Menu 20.0.3** for its configuration button.
5. Open settings using **Right Shift** (rebindable in Minecraft Controls), `/solyanka config` in a world, or the Mod Menu entry.

YACL is a **new required dependency** in 0.2. A 0.1 config migrates additively; existing unknown fields and diagnostic state are retained. Invalid or future-schema config is not overwritten; writes remain disabled for that session until the file is repaired and the client restarted.

## Use

- `/solyanka status`: implemented module state and entry counts.
- `/solyanka toggle utility.command_keybinds`
- `/solyanka toggle utility.command_shortcuts`
- `/solyanka toggle core.diagnostics`
- `/solyanka config`: open the YACL configuration screen on the next client tick.

Everything defaults **off**. In settings, enable the relevant module under General, add a row, fill it out and enable the row. A `/` prefix selects a command; otherwise the text is sent as ordinary chat. **Commands/chat can reach the server; test using harmless messages first.**

### Example

Create shortcut `dh` targeting `/warp dungeon_hub`, then a keyboard binding for `/dh`. Capture its main key and choose the modifier separately. Enable both rows and modules before expecting expansion. No command is sent just by adding a row or starting the client.

### Settings behavior

- Save commits ordinary edits. Cancel discards edits since the last commit.
- **Save & add / Save & remove explicitly commit all pending edits**, then rebuild the list. Cancel does not undo a previous explicit list-action commit.
- Key capture: press the main keyboard key or click a mouse button away from Cancel. Escape cancels; Backspace unbinds. Set Ctrl/Shift/Alt in the separate modifier control.
- Ordered keybinds: the first matching row wins. Only initial press sends; held-key repeats do not resend.
- Container execution is opt-in. Chat, anvils, settings screens and focused EditBox inputs are excluded. Input from an unfocused window or disconnected client is ignored.
- Class/island filters use current tab-list labels, not a full dungeon state model. Unknown filtered context blocks the binding; empty filters are unrestricted.
- Aliases match an exact, case-sensitive command token, append arguments and apply immediately without lobby changes. Cycles, expansions beyond 16 aliases and generated messages over 256 characters are rejected. Unrelated commands bypass the interceptor.
- `/solyanka` and existing registered client commands take precedence. Aliases can intentionally override a matching server command. Alias tab-completion integration is not yet implemented.

## Build

Install a full JDK 25, set JAVA_HOME, then run:

```sh
./gradlew clean build
```

Windows: `gradlew.bat clean build`. Development client: `./gradlew runClient` (requires a real graphical environment). Install the ordinary `build/libs/solyanka-0.2.0-dev.jar`, not the `-sources.jar`.

Pinned tools: Gradle 9.7.1 (checksum verified), Loom 1.18.4 (`net.fabricmc.fabric-loom`), Kotlin 2.4.21. Minecraft 26.2 uses official unobfuscated names; no Yarn/remapping configuration is added.

## Project layout

- `src/main/kotlin/dev/solyanka/core`: lifecycle/registry.
- `src/main/kotlin/dev/solyanka/config`: transactional JSON persistence.
- `src/main/kotlin/dev/solyanka/utility`: typed profiles, validation, resolver, tab parser and input policy.
- `src/client/kotlin/dev/solyanka/client`: Fabric integration, command routing, YACL screens, key capture and Kotlin input mixins.
- `src/test/kotlin`: pure lifecycle, persistence, resolver, policy, parser and migration tests.
- `docs`: audit, implementation notes, roadmap and in-game checklist.
- `audit`: pinned upstream references, all 38 requested feature entries and all 34 attached Kotlin files (23 feature objects).
- `validation`: build log, unit-test reports and static mixin ABI evidence.
- `examples/solyanka.json`: disabled-by-default example; do not overwrite an existing config without backing it up.

## Licensing and status

Original/adapted project source is GPL-3.0-only; see LICENSE and NOTICE. This delivery includes complete project source/build scripts. NoFrills behavior references and changes are documented in `docs/M1.md`; the original owo/Orbit runtime is not bundled. External libraries remain separate dependencies. Other upstream code, graphics and assets are not yet ported; their separate license/provenance gates remain in force.

See `docs/VALIDATION.md` for actual build/test results, `docs/TESTING.md` for your in-game checklist and `docs/ROADMAP.md` for remaining work. This is not the complete combined mod.
