# Architecture

## Implemented now

- Fabric Kotlin client entrypoint; exact Minecraft 26.2 dependency.
- Loader/API/Kotlin runtime and Java 25 toolchain; official unobfuscated game names.
- Game-independent lifecycle core in `src/main/kotlin/dev/solyanka/core`.
- Game-independent JSON persistence in `src/main/kotlin/dev/solyanka/config`.
- Client-only Minecraft/Fabric adapters in `src/client/kotlin/dev/solyanka/client`.
- Registry rejects duplicate IDs; enable/disable are idempotent; failed startup runs cleanup.
- Only enabled modules receive ticks and world-reset events. Runtime exceptions disable the failing module without skipping siblings; fatal JVM Errors are not swallowed.
- World identity changes/disconnects reset active module state; menu ticks do not run feature logic. Future per-run/dimension events need explicit adapters.
- Versioned JSON config with atomic replacement, unknown-setting retention, validation and read-only fallback for corrupt/future-schema files.
- `/solyanka`, `/solyanka status`, `/solyanka toggle core.diagnostics`.
- One diagnostic lifecycle module only. No fake placeholder feature toggles.

## Ownership / contracts

All module mutation and dispatch occurs on the client thread. Modules release resources and clear transient state on disable; active modules clear world-scoped state in onReset. Async work must carry a session generation token, avoid touching Minecraft off-thread, and post results back only if still current. A future renderer adapter owns GPU resource lifetime and restores all modified state. Never embed a separate upstream event bus per feature.

Feature catalog records are NOT executable Module instances. Only register implemented, tested modules. Keep persistent settings separate from transient gameplay state. Extend config through typed settings/adapters; do not store Minecraft entities or GPU objects in JSON.

## GUI choice

YACL is the preferred initial settings UI because a Fabric 26.2 release is published (`3.9.7+26.2-fabric` at audit time). See https://api.modrinth.com/v2/project/yacl/version?game_versions=%5B%2226.2%22%5D&loaders=%5B%22fabric%22%5D .

The first milestone intentionally has JSON + command controls, not a GUI. YACL integration and Mod Menu entry are the next infrastructure slice, not claimed implemented. Verify dependency artifacts/transitives before wiring them. A shared settings model should preserve AetherOS theme options independently of UI-library choice. No Vigilance or legacy GUI framework is copied.

## Proposed feature packages

- `context`: SkyBlock location, dungeon run/room/player state, item metadata.
- `dungeon`: map scanner, coordinate transforms, waypoints, doors, mimic and boss timers.
- `solver`: pure algorithms and state machines, separate from Minecraft data collection and overlay output.
- `utility`: chat, command shortcuts, keybinds, input/screen behavior and tooltips.
- `render`: common overlay extraction/drawing, environment controls, outlines, glow, post-processing and compatibility adapters.
- `hud`: layout, editor, panels, scoreboard/chat styling, counters and timers.
- `cosmetic`: held-item transforms, knife, player layers and local cosmetics.
- `hunting`: shared entity/block trackers for Bestiary/Invisibug/Tiki/Titanium.
- `mixin`: only hooks unavailable in Fabric API; narrow targets and version-specific compat packages.

No empty Mixin config is installed: add configs when real hooks exist, with required/defaultRequire policy, optional-mod guards and focused tests.
