# Architecture — 0.2.0-dev

## Layers

1. **Pure core:** Module/ModuleRegistry lifecycle and failure isolation; no Minecraft imports.
2. **Pure utility domain:** immutable CommandProfile/CommandBinding/Shortcut, validators, resolver, InputContext policy and TabContextParser. No Fabric/GLFW references; fixtures run under JUnit.
3. **Persistence:** ConfigStore + ProfileCodec. Validate a candidate, atomically replace disk, then update in-memory root. Retain unrelated config and stable-ID row extensions. A corrupt/future-schema load disables writes for that session.
4. **Client adapter:** client entrypoint owns runtime profile, lifecycle, tab data extraction, command event handler and raw input ownership. All mutation/dispatch is on the client thread.
5. **UI:** required external YACL. Screens edit a local draft; commits validate/persist before activating the runtime profile. Structural actions explicitly commit all edits before rebuilding. Key capture returns pending values to the same YACL option instances. Mod Menu is optional.
6. **Mixins:** two client-only Kotlin hooks at KeyboardHandler.keyPress and MouseHandler.onButton HEAD, cancellable. They forward into the guarded adapter, not separate upstream event buses. Target ABI evidence is retained; runtime application remains untested.

## Lifecycle and ownership

Only enabled modules receive tick/reset callbacks. Failed module dispatch is quarantined; fatal JVM Errors are not swallowed. Modules must clear transient state/release resources in onDisable and active world state in onReset. The key adapter owns consumed press tokens so repeats/releases cannot accidentally trigger vanilla after a command bind consumed its press. World changes clear those tokens.

All modules default off. Only diagnostics, utility.command_keybinds and utility.command_shortcuts are registered. Backlog descriptors are not executable fake modules.

Alias expansion uses a snapshot of currently enabled entries, excludes roots already registered by client mods, validates bounded output, and guards synchronous retransmission. It does not use raw packets or bypass vanilla signed message dispatch. Input keybinds must pass focused/connected/screen/text/modifier/filter policy before dispatch. Command configuration never sends a message on its own.

The tab adapter is intentionally small and stateless. The future shared SkyBlock/dungeon context layer must own authoritative run/room/item/player state and broaden parsing using real fixtures. Do not duplicate this context per feature.

## Future modules (all retained)

- context: SkyBlock location, dungeon run/room/player state, item metadata and session generation tokens.
- dungeon: map scan, coordinate transforms, both waypoint sources/codecs, doors, mimic, map info and boss timers.
- solver: pure algorithms separated from game extraction and overlay output.
- utility: chat, screen/mouse fixes, tooltips and pricing cache/providers.
- render: extraction/drawing, environment, outlines/glow, post-processing and optional renderer adapters.
- hud: editor/layout, progress, inventory, scoreboard/chat, counters and timers.
- cosmetic: held item/knife, layers, badges/wings/text replacements.
- hunting: common entity/block trackers for Bestiary/Invisibug/Tiki/Titanium.

Future async work must be cancellable/world-generation scoped and post results to the client thread. Future GPU adapters must own resource lifetime and restore state. Sodium/Iris hooks need optional-mod guards and actual version-specific tests; compatibility is not assumed.
