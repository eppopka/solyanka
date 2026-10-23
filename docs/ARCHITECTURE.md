# Architecture — 0.2.1-dev

The game-independent lifecycle, command resolver, input policy and transactional JSON persistence remain as in M1. The UI layer is now an actual adapted NoFrills/owo port rather than YACL.

## Layers

1. Pure core: Module/ModuleRegistry lifecycle and failure isolation.
2. Pure command domain: immutable profiles, validation, bounded resolver, input context/policy and stateless tab parser.
3. Pure GUI model: only implemented FeatureCardInfo entries, normalized search and CommandGuiDraft. Row updates are stable-ID based; original live profiles are not mutated by draft editing.
4. Persistence: typed candidate validation, atomic replacement, then in-memory commit. Preserve unrelated data and stable-ID row fields; corrupt/future-schema data disables writes rather than overwriting it.
5. Client adapter: runtime module state, world reset, raw input ownership, outgoing-command event, deferred GUI-open requests and vanilla command/chat dispatch.
6. GUI: NoFrillsClickGui + GuiKit + SettingsPanel using external owo. ConfigUi binds screens to solyanka. Main toggles commit immediately; nested list/editor drafts commit only through Save & back. Inline capture is screen-owned and blocks navigation while waiting for a key/mouse input.
7. Mixins: existing two Kotlin client-only input hooks. No new Minecraft mixin is needed to open this GUI.

Everything runs on the client thread. Active module dispatch faults are quarantined; fatal JVM Errors are not swallowed. Owned input repeats/releases stay consumed until release/reset. Gameplay bindings do not run in settings or search screens.

## Screen ownership

Internal navigation returns fresh screen factories carrying the same explicit draft, not stale row-index listeners. Back from an editor discards local row edits unless Done (stage) was used. Back from a root list abandons its uncommitted draft. Error screens return to a fresh retry view with draft values intact. Stable IDs keep row update/delete safe.

Each owo screen releases its native cursor adapter when removed. If a parent screen instance is reused by external navigation, its disposed adapter is cleared so it can initialize afresh. Draft data lives outside those widget objects.

## Dependencies / status

- Required external UI dependency: owo-lib 0.13.1+26.2 (MIT).
- Optional Mod Menu 20.0.3 factory.
- No YACL imports/dependency remain in current runtime source or Gradle pins; historical M1 audit records are preserved.
- Only diagnostics, command keybinds and command shortcuts are registered. All other requested features remain tracked, not fake modules.
- Full Minecraft launch, visual layout, cursor lifecycle, GUI scaling and mod compatibility remain user acceptance work.

Future context, dungeon, solver, utility, renderer, HUD, cosmetic and hunting modules retain their roadmap scope. Share adapters/settings/context rather than importing whole independent upstream frameworks per feature. Async work must be session scoped and game-thread applied; future GPU code must own resources and restore state.
