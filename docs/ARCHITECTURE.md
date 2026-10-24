# Architecture — 0.5.0-dev

26 registered modules; 30 client mixin classes. Main/client Kotlin source sets target Java25/official MC26.2. Config schema remains additive/unknown-field-preserving. Main GUI and all settings remain one Screen with owned modal OwoUIAdapter panels and immediate atomic settings commits.

- Pure logic: command/chat/tooltip/context/timer/player/cursor models plus ExperimentLogic, PriceLogic, DungeonLogic, DungeonMapLogic (keys/statistics), WaypointLogic/Codec/Store and EnvironmentLogic.
- Client runtime: FeatureRuntime context; Experiments menu snapshots; PriceTooltips bounded background public data + local market store; Suppression filters; DungeonPuzzles loaded room scan/solvers; DungeonMap map/key/score/HUD; DungeonWaypoints local pack/editor/collection; Environment weather/clock/aspect/fog adapters.
- Renderer: immutable line/fill/text snapshots via LevelRenderEvents COLLECT_SUBMITS, camera-relative pose scopes, pipeline-owned depth/blend state and explicit official AW. No whole Odin/NoFrills/Stray render frameworks.
- World identity resets context, experiment state, room/run/waypoint collection and geometry. Individual dungeon disable clears geometry; next five-tick snapshot reconstructs remaining enabled outputs. Public account-independent market cache may outlive a world/requesting toggle.
- Server packet consumers run after vanilla thread guards; pickup/removal observations happen before vanilla discards entities. Manual keybind/editor input uses press/repeat/release ownership. No automatic experiment clicks or waypoint movement.
- Current implementation is not full source parity. M4.md is authoritative for new behavior, omissions, file/network boundaries and validation.

## Prior architecture (0.4.0 historical)

# Architecture — 0.4.0-dev

The game-independent lifecycle, command resolver, input policy and transactional JSON persistence remain as in M1. The UI layer is now an actual adapted NoFrills/owo port rather than YACL.

## Layers

1. Pure core: Module/ModuleRegistry lifecycle and failure isolation.
2. Pure command domain: immutable profiles, validation, bounded resolver, input context/policy and stateless tab parser.
3. Pure GUI model: implemented FeatureCardInfo entries, normalized search and InstantSettings. Stable-ID row updates validate and persist immediately without mutating last-good live values on failure.
4. Persistence: typed candidate validation, atomic replacement, then in-memory commit. Preserve unrelated data and stable-ID row fields; corrupt/future-schema data disables writes rather than overwriting it.
5. Client adapter: runtime module state, world reset, raw input ownership, outgoing-command event, deferred GUI-open requests and vanilla command/chat dispatch.
6. GUI: NoFrillsClickGui + GuiKit + SettingsPanel using external owo. ConfigUi binds screens to solyanka. Main toggles and valid list/editor changes commit immediately. Inline capture is screen-owned and blocks navigation while waiting for a key/mouse input.
7. Mixins: existing five classes plus server-ping TAIL, local pose metadata wrapper, scoped container click wrappers, cursor capture/release, screen-transition TAIL and loading visual extraction hooks.
10. FeatureRuntime: main-thread shared scoreboard/tab/floor/boss context, pure server-ping timer/stat/cursor state and Fabric HUD element adapters. Preview data is local/temporary and never replaces live state.
11. Additive FeatureSettings under generalUtilities.features; strict known-field type/range validation and recursive unknown-field preservation.
8. Information domain: pure ChatLogic, ItemFacts/TooltipLogic and typed GeneralSettings with recursive unknown-field-preserving codec.
9. Museum provider: opt-in, bounded HTTP/background parse into a volatile immutable category map; official public resource, no per-player data.

All game/UI/config work runs on the client thread; only museum HTTP/parsing is background work. Server-ping handling injects after vanilla's thread guard. HUD scaling uses a push/try/finally/pop matrix scope. Active module dispatch faults are quarantined; fatal JVM Errors are not swallowed. Owned input repeats/releases stay consumed until release/reset. Gameplay bindings do not run in settings or search screens.

## Screen ownership / 0.3.1

NoFrillsClickGui is the only Minecraft Screen for categories and settings. SettingsPanel is an ordinary Kotlin object owning a createWithoutScreen OwoUIAdapter, not a Screen subclass. The host renders it over the existing UI, routes all key/character/mouse/drag/scroll events exclusively to it, hides background tooltips and clears background focus. Each closed/replaced/resized panel disposes its own native cursor adapter. Only exiting the entire GUI returns to the previous Minecraft screen.

ConfigUi builds panel contents from a shared InstantSettings session per opened feature. List/editor navigation changes the embedded panel, never invokes setScreenAndShow. Resize remounts widgets from the same model. Capture owns its input and suppresses resulting text entry. Escape cancels capture first, otherwise navigates back.

InstantSettings validates every edit and invokes the existing atomic persistence callback immediately. Runtime state updates only after successful disk writes. Invalid intermediate input and write failures remain in the local editing model with inline status; correction commits pending valid values. Back does not save/undo; closing a feature editor discards only invalid/failed pending changes. No asynchronous/debounced write queue, save button or error Screen is used. Legacy CommandGuiDraft remains an unused compatibility/test helper; it no longer backs production UI.

## Dependencies / status

- Required external UI dependency: owo-lib 0.13.1+26.2 (MIT).
- Optional Mod Menu 20.0.3 factory.
- No YACL imports/dependency remain in current runtime source or Gradle pins; historical M1 audit records are preserved.
- Twelve registered modules: existing five plus Odin Tick Timers/Player Display and NoFrills Animation/Middle Fix/Override/Cursor/Loading. See M3.md.
- Endec 0.1.12 is compile-only because owo injects its MapCarrier into CompoundTag; runtime is nested within external owo.
- See M2.md for network lifetime, privacy, failure behavior and source differences.
- Full Minecraft launch, visual layout, cursor lifecycle, GUI scaling and mod compatibility remain user acceptance work.

Future context, dungeon, solver, utility, renderer, HUD, cosmetic and hunting modules retain their roadmap scope. Share adapters/settings/context rather than importing whole independent upstream frameworks per feature. Gameplay async work must be session scoped and game-thread applied; account-independent public museum data intentionally survives world changes in a process. Future GPU code must own resources and restore state.
