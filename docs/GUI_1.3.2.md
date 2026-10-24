# 1.3.2 — null hover-style crash fix

The supplied report is from solyanka **1.3.0**, Minecraft 26.2, owo 0.13.1+26.2. Its immediate failure is:

```
NullPointerException: hoveredStyle is null
GuiGraphicsExtractor.componentHoverEffect
LabelComponent.drawTooltip
OwoUIAdapter.drawTooltip
NoFrillsClickGui.extractRenderState
```

Inspection of the installed owo bytecode confirms `LabelComponent.drawTooltip` passes the nullable result of `styleAt` directly to Minecraft's non-null hover-effect API. Empty label areas, padding and wrapped-line gaps can legitimately have no text style. This is not a Held Item shader stack trace.

## Targeted fix

Our shared `PlainLabel` now returns `Style.EMPTY` when the inherited style lookup returns null. Non-null styles are retained unchanged. The library's normal tooltip renderer still runs: configured help tooltips and rich hover events are not disabled. This fix is scoped to solyanka's label subclass, not a global Minecraft/owo Mixin.

Includes the 1.3.1 rounded GUI, draggable settings title bars, transparent world background, deferred panel navigation and instant saving. No feature configuration migration is needed. Replace the previous solyanka JAR; do not keep duplicate versions.

## Validation

Regression checks verify the shared label path, fallback source contract and compiled null branch. The native dependency callsite is checked against the actual owo JAR. Clean build/tests and existing native/shader checks are rerun. **The user's live crash has not been reproduced here, and this build has not been tested in-game.** It directly addresses the null path identified in the report; in-game confirmation remains necessary.

The raw user crash report is not included in the source package because it contains unrelated system/session details. No shader changes or third-party mod disabling are required by this patch. Other feature-completion limitations in M13.md remain unchanged.
