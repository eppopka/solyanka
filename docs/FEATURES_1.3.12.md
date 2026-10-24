# solyanka 1.3.12 — Knife browser, Item Bindings and hand shader

## CS2 Knife browser
Open CS2 Knife settings from `/solyanka`. The embedded, draggable three-column browser keeps the module list behind it and saves valid changes immediately.

- Searchable knife list and searchable finish-photo tiles, with a larger composed knife preview.
- Seed, random seed and wear/float controls; global item rule, animations and third-person switches.
- View tab for horizontal/vertical/distance controls and reset.
- Ctrl+F focuses knife search. Escape/back closes the embedded panel.
- Assets / More retains the existing asset setup and advanced settings, including the explicit confirmed download. Open Knife Browser returns to the picker.

Photos are decoded from the CS2 pack, not generated substitutes. Existing installed/cached assets are reused. If assets are missing, use Assets / More to configure/download them. No Valve assets are bundled and no download starts automatically. Missing previews display a loading/unavailable state. Enable CS2 Knife to render replacements in gameplay.

## Item Bindings
1. Select the knife, finish, seed and wear you want.
2. Hold the specific SkyBlock item and open **Item Bindings**.
3. Choose **Bind selection to held item**.
4. Repeat with another item and another setup (for example Butterfly and Karambit).

An enabled binding takes priority over the global Swords/Any Item/Empty Hand rule. Unbound items use global settings. Each binding stores its own knife, finish, seed and wear. Edit opens that profile; subsequent selection changes save to that profile. Use Global in the header to return to global editing. Bindings can be individually disabled or deleted; the limit is 128.

Identity comes only from an exact item custom-data UUID (root uuid or legacy ExtraAttributes.uuid). Conflicting or missing UUIDs are rejected with a message; display names and item types are never used as a fake identity. Items without stable UUIDs can still use global settings. Nothing is written to the item or sent to the server. A newly selected bound item never displays the previous item's knife while its own model is loading.

## Held Item Shader on the hand
Enable Held Item Shader, then enable **Apply to Hand / Arm** in its settings (also available from the knife browser View tab). Existing left/right hand switches apply. The new option defaults off. It targets the native first-person skin-arm renderer, including knife arms which use that renderer, not third-person players. Special render paths may not use this hook. It reuses the validated item pipeline and monotonic animation clock without adding a duplicate Globals layout.

## Port scope and verification
The menu is adapted from Stray's knife-browser layout and preview workflow, not a pixel-identical copy of its glass/theme framework. Its original SoftwareRender implementation body is retained, with namespace changes. Full Grip and Blade editors/shaders remain outside this release. Item bindings are solyanka's UUID-specific adaptation, not a claim of complete upstream per-sword profile parity.

Preview work is asynchronous on the existing serial decoder worker; the menu permits two pending jobs and 40 GPU textures, releases them when closed, and rejects stale asset-generation results. Full preview requests are debounced. These limits do not describe the underlying asset decoder's existing cache.

Verification: clean Fabric 26.2 build; 640 JVM tests; 22 offline validation scripts; synthetic rasterizer/depth checks; source-hash/body parity; native hand-method descriptor and compiled hook checks. There are 57 registered modules and 45 mixin classes. These checks do not launch Minecraft or prove native Mixin application, real CS2 pack decode, GPU rendering, FPS, or Sodium/Iris compatibility. Please test actual skin images, item switching, binding persistence after restart, both hand sides, resize/drag and closing/reopening the browser in-game.

Source provenance: audit/stray-knife-menu-source/manifest.json, pinned Stray commit d91fcd4515b4eda647ee8fd7fa99408f473e5718 (CC0-1.0). Previous hotbar recovery/original painter work and excluded-module choices are retained. `.gitignore` is unchanged.
