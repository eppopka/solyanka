# solyanka 1.3.9 — Halo depth hotfix

- Corrected the inverted depth comparison in the shared world-overlay triangle and fill pipelines for Minecraft 26.2 reversed-Z rendering.
- With Through Walls off, Halo now uses GREATER_THAN_OR_EQUAL, without depth writes. Empty scene depth should no longer hide the halo; opaque player models and walls should occlude portions behind them.
- Through Walls on deliberately retains ALWAYS_PASS, including visibility through player models. Leave it off for ordinary occlusion.
- China Hat triangles and shared filled overlays receive the same depth correction. Line and label paths are unchanged.
- No configuration reset, new modules, downloads or CS2 menu changes in this hotfix.

## Verification

JVM regression guards cover both world-overlay pipelines and Halo's through-walls routing. An offline software-rendered depth fixture checks visibility against empty depth, occlusion behind a nearer surface, visibility in front of it, and the explicit through-walls bypass. The native 26.2 default depth comparison is checked too.

Minecraft was not launched here: actual Halo visuals, player-model ordering and Sodium/Iris compatibility still need in-game acceptance. Test in third person with Through Walls off: look at the halo against the sky, then across the player model, then behind an opaque wall. Only the occluded parts should disappear.
