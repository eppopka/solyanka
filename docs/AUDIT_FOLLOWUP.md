# Claude audit: work in progress

Baseline: `9f4cb3507a0a1a8bd48e6b0b31424d046d46804e` (Solyanka 1.4.6). This working tree is **not yet a complete implementation of the audit**. Test runs do not replace an in-game visual or interaction check.

## Issues

- P1: Feature-card text is replaced only when its displayed color changes. Theme edits still take effect while idle.
- P2: The HUD editor computes its entry list once per normal render frame and uses one live scoreboard snapshot for both drawing and measurement.
- P3: The eight skybox frames were downscaled to 1024² and the icon to 512²; downscaling is lossy and needs in-game review. A shared base plus padded replacement-crop atlas reconstructs each resized frame exactly (this is not a signed-delta atlas). Tests verify reconstructed pixels and filtering gutters; in-game visual review remains necessary. Modern NPOT textures are not inherently incompatible with mipmaps.
- P4: HUD styling and generic feature panels use typed settings updates instead of full Gson encode/decode cycles. Persisting configuration still serializes it.
- P5: Repository-wide formatting and CI enforcement remain open. A GitHub Actions workflow runs the build and tests. In an isolated experiment, ktfmt passed after normalizing leading-dot literals, but 61 tests failed after formatting. Many tests inspect source text and need careful, behavior-preserving updates before formatting and its CI enforcement can be accepted.
- P6: Presets and shared ClickGUI palette defaults have been introduced, but some hard-coded colors remain.
- P7: Common controls, category labels, and feature-card names have English/Russian translations with English fallback. Descriptions, some controls, and search by translated names remain untranslated. The unused key-capture screen and command draft were removed.
- P8: Version logging and user agents use Fabric metadata rather than hard-coded release strings.
- P9: The archive hotbar geometry has been renamed; the suggested package migration remains open.
- P10: Editor input tests use the actual control bounds.
- P11: Module transition failures are reported and actual states saved when possible; hook side effects are not transactional.
- P12: Existing Kotlin mixins are not assumed broken just because they use Kotlin. New mixins should be reviewed individually.
- The title gradient is cached. The PowerShell resource is in use; see `LOCAL_MUSIC.md`. The root ignore rules have been consolidated, and the historical foundation notes retained under `docs/`. Render-thread checks and wider font caching remain open.

## UI improvements

Shared button hover, card hover/press and sounds, panel shadow, static gradient backgrounds, pill switches, slide-in, sliders, scoreboard accent and zebra rows, vignette, search highlighting and category counts, themed-chat fade, performance metric colors, music progress styling, HUD editor grid/guides/snapping, and some cursor affordances are implemented. The title remains the only animated-gradient text, as requested. Panel fade, consistent hover/cursor coverage for all custom controls, and visual verification in Minecraft remain open.

## Proposed features

Theme presets, favorites and a hold-key quick-toggle screen, layout/theme share commands, and opt-in context-based HUD position profiles are implemented but still require live-game testing. A session-only recently-changed column records actual successful state changes. The sharing format deliberately excludes commands, personal preferences and credentials.

## Structural work still open

Module IDs were extracted with compatibility forwards; command registration and input routing were separated; client module declarations were extracted into a catalog; shared UI models moved to `model`; several geometry/animation helpers moved to `gfx`; and third-party decoders moved under `thirdparty`. A settings route map and HUD preview registry replace dispatch chains, but are not yet a unified typed feature/HUD registry. A shared HUD frame helper handles positioning and pose restoration for several HUDs, including inventory; not every HUD uses it. The remaining package migration, entrypoint decomposition, licensing provenance review, and full registry are open. LZ4, the BC7-table attribution, and two helper source files have unresolved provenance; see `licenses/CS2-THIRD-PARTY-NOTICES.md`. Do not describe this work as a finished audit patch.
