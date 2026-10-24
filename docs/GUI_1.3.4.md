# 1.3.4 — Held Item pipeline correction and visible feature list

## Feature list

The feature/category list stays rendered behind an embedded settings panel again. Only the portion physically covered by the panel is hidden. Settings remain draggable, undimmed, instant-saving and modal: clicks/scroll/key input do not leak to background cards/search and background tooltips remain suppressed. The 1.3.3 rounded scanline coalescing and 128-entry geometry cache are retained. This intentionally reverses the 1.3.3 hidden-list extraction optimization at the user's request.

## Held Item Shader source comparison

Rechecked the current public Stray HEAD through GitHub and fetched HeldItemShader, ItemFeatureRendererMixin, both core shaders and gradle.properties. HEAD is still d91fcd4515b4eda647ee8fd7fa99408f473e5718. Exact source files, URLs and hashes are recorded in audit/held-item-recheck-source-manifest.json.

Corrections/hardening:
- Use Stray's **ColorTargetState.DEFAULT** for the main item-fill pipeline. The previous port enabled alpha blending; Portal/Galaxy then blended their reduced alpha into the world, unlike Stray's opaque item fill. The effect's internal color/fill calculations remain configurable.
- Register pipeline metadata at client initialization, before normal resource shader precompilation, rather than on the first enabled client tick only.
- Prepare resources at **GameRenderer.renderLevel HEAD**, before first-person item draws are prepared. This no longer depends on the client-tick callback, including when ticks pause but frames continue.
- Explicit native pipeline compilation/validity check before publishing readiness. Compilation/setup/upload failures retain vanilla rendering and are logged; toggling the feature retries.
- Initialize both settings texels before DynamicTexture's constructor performs its first upload. Recreate a closed backing image; clear owned resources/cache on world/renderer lifecycle reset.
- Retain first-person hand filtering, compatible ENTITY/QUADS validation and a bounded atlas cache. Use Stray's item-atlas fallback if matching material metadata is unavailable. No GPU upload occurs inside the material replacement callback.
- Keep native glint/outline preparation untouched. No global render-pass uniform interception or server changes.

**These changes do not establish the exact cause of the user's “not working” symptom.** No current client log or visual example of that symptom was supplied. Do not claim an in-game fix has been confirmed.

## Runtime diagnosis

Run `/solyanka helditemstatus` after enabling Held Item Shader and holding an ordinary sword/block in first person. It reports enabled/style/hand flags, ready/status, eligible replacements, successful replacements, not-ready skips and incompatible-layer skips. The command reports locally only; no network request is made. Replacements mean the native material callback selected the effect, not proof of correct final GPU pixels. One first-replacement info line is logged per enable cycle, not per quad/frame.

- `enabled=false`: enable the module (opening settings alone is not enabling).
- `ready=false` with a compile/upload failure: send the status and relevant latest.log shader messages. Toggling off/on retries; do not remove other mods blindly.
- `eligible=0`: use first person and an ordinary baked item; check hand toggles. Arms, maps and special model renderers are still not covered.
- `replaced>0`, but no visible effect: send a screenshot/short clip, effect choice and latest.log, including shader pack/Sodium/Iris versions. This isolates native shader/compositing compatibility from a skipped callback.

## Scope and validation boundary

Still only Smoke/Stars/Portal/Galaxy for eligible first-person item quads. Stray's arm replacement, silhouette post-outline, player fill and CS2 model backend remain outside this patch. No placeholder switches added.

Clean tests/build, native target/handler checks and offline expanded/translated shader tests are rerun. The new pixel test renders the actual vertex/fragment shader sources with synthetic atlas/portal inputs for all four styles: visible modification, animated time, speed-zero freeze and alpha-cutout discard (20 render cases). The backend is Chromium SwiftShader, not Minecraft/native GPU execution.

**No live Minecraft, applied-Mixin, native GPU, Sodium/Iris or visual/FPS acceptance is claimed.** The feature-list restore costs more than not drawing it, but keeps the lower-cost panel geometry.

Manual checks: replace the previous JAR (one version only), open/drag/close several settings and verify the list stays visible without click-through; test a normal main/offhand item in every effect; change settings while a paused-world GUI is open; toggle off/on, change worlds and reload resources; collect helditemstatus/latest.log if the effect remains absent.
