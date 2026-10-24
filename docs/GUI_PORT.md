# NoFrills GUI port — 0.2.1-dev

## Source and licensing

NoFrills commit `74e9317c1a7b8484a575b2f177479f5e3f2bfb9a`:
https://github.com/WhatYouThing/NoFrills/tree/74e9317c1a7b8484a575b2f177479f5e3f2bfb9a

Original author attribution: WhatYouThing and NoFrills contributors. GPLv3 repository license is retained in LICENSE/NOTICE and audit/licenses/NoFrills.txt. This is a Kotlin port/adaptation, not merely a color theme on YACL.

| Upstream source | Adapted implementation |
|---|---|
| hud/clickgui/ClickGui.java | NoFrillsClickGui: column layout, search, scroll routing, usage hints |
| hud/clickgui/Category.java | blue header and per-category scroll assembly |
| hud/clickgui/Module.java | FeatureCard: left toggle, right settings, active text/surface/accent |
| hud/clickgui/Settings.java | SettingsPanel, flat rows, headers and scroll surfaces |
| hud/InnerSettingsScreen.java | factory-based parent navigation in ConfigUi/SettingsPanel |
| hud/clickgui/components/FlatTextbox.java | FlatTextbox border/background/scissor and no vanilla border |
| hud/clickgui/components/PlainLabel.java | non-consuming labels so cards receive clicks |
| hud/clickgui/components/ToggleButton.java | flat Enabled/Disabled control and green/red states |
| hud/clickgui/components/KeybindButton.java | inline Press key capture in SettingsPanel |
| misc/Rendering.java:drawBorder | GuiKit.border |
| features/general/CommandKeybinds.java, CommandShortcuts.java | adapted command list/editor structures using solyanka profiles |

All upstream paths are under `src/main/java/nofrills/`. The palette retains `0xff5ca0bf` blue, `0xff101010` control backgrounds, white outlines for secondary controls, dark module surfaces and active left accent bars.

## solyanka changes

- Uses published owo-lib **0.13.1+26.2**, not a NoFrills 26.1 binary or the old YACL layer.
- Replaces NoFrills Feature/Setting/Orbit references with solyanka state, immutable rows and transactional persistence.
- Initially showed three modules; 0.3.0 added Chat Tweaks and Info Tooltips. Version 0.4.0 now has twelve real registered modules, including two Odin and five additional NoFrills modules. Unimplemented toggles remain hidden.
- `/solyanka` directly requests opening the GUI next client tick, avoiding chat-close races. `/solyanka config`, Right Shift and Mod Menu converge on the same screen factory.
- As of 0.3.1, saves valid controls/list/editor changes immediately, with inline validation/write failures. No save or staging buttons remain. Last-good values are preserved while intermediate invalid input stays local.
- Keeps keyboard vs mouse distinction, focus/connection/filter checks, bounded aliases and corrupt-config protection.
- Key capture cancels with Escape and unbinds with Backspace; modifier selection is separate. It no longer opens a separate vanilla/YACL capture screen.
- Centers settings panels, adapts widths to the GUI and limits the main list to known categories. Exact upstream pixel identity is not promised.
- Releases screen-owned native cursor resources on exit and embedded panel adapters on replacement/close/resize. Settings use createWithoutScreen and never install a second Minecraft Screen.

## Preserved limitations

No in-game launch, screenshot/visual confirmation, signed chat test or Sodium/Iris test was performed here. The user subsequently reported the 0.2.1 build working; that is not a detailed compatibility certification. Unit tests and compile-time API checks do not prove runtime layout correctness. Tab parsing supports standard current Area/Dungeon/player-class labels only; alias completion nodes and other feature modules remain pending.

See TESTING.md for the acceptance sequence.

## 0.5.1 controls and panel scoping

ColorPickerScreen/SettingColor and EnumButton from the same pinned NoFrills
revision inform the new controls. `GuiKit.colorRow` embeds owo's
ColorPickerComponent directly in the current SettingsPanel, below a swatch and
Edit color button. No Minecraft Screen is created. Palette changes persist via
the existing validated instant-save path. `choiceRow` cycles named options while
preserving existing serialized integer meanings.

SettingsCatalog explicitly assigns fields to each feature; sibling environment
and dungeon features no longer expose identical whole-config sections. Legacy
puzzle depthTest migrates to separate iceDepthTest/blazeDepthTest/beamDepthTest
when the new keys are absent. Existing unknown config fields remain preserved.
The old shared field is retained for compatibility but not shown in the GUI.

Both top-level and panel roots are transparent. owo BaseOwoScreen's
extractBackground is a no-op in the pinned dependency; no additional fullscreen
dim surface is installed. Panel/card surfaces themselves remain dark.

Player Display preview controls and success footer text are removed; timer
preview controls, explicit diagnostic commands, validation and write errors
remain. Runtime appearance/input still requires in-game verification.

## 0.5.2 compact controls and HUD layout editor

`GuiKit.hudRow` places each HUD toggle and edit button in one line when it fits,
with a tight stacked fallback. `sliderRow` uses owo SlimSliderComponent with
explicit min/max/step; initialization precedes subscription so opening a panel
does not write config. `SettingsNumbers` defines ranges for every exposed numeric
field. Text inputs now represent actual text only. `DirectionalButton` consumes
RMB directly before vanilla rejects that button; LMB/keyboard advance. Cycling
supports an external current-index getter so canvas selection stays synchronized.

NoFrills HudEditorScreen interaction concepts are adapted to a dedicated
`HudEditorScreen` opened after chat closes by `/solyanka gui`. It shows six timers,
seven stat HUDs and position-only Map Info. All samples are generated locally,
not inferred from a dungeon run. No master module is enabled by opening/editing.
Map Info keeps its existing fixed scale and module-based visibility; other HUDs
support direct drag, per-HUD visibility, scroll-scale, and arrow-key nudging.

Pure `HudLayout` provides registry/update/bounds/drag policies. The editor reads
last-good live settings and resets each update to that base, so failed disk
writes do not render an uncommitted draft or leak it into another edit. The
old timed preview state and commands are removed, not repurposed.

Bounds reflect representative text, not guaranteed future dynamic text length.
Overlapping elements can be selected using the bottom forward/back cycle picker;
selected elements draw last. Disabled per-HUD elements remain selectable as gray
samples. The canvas is transparent. Done/Escape only navigates; writes occur
through the existing validated atomic config path during interaction.

Manual verification still required for mouse dragging, sliders, right-click,
resizing, keyboard focus and scaling at actual Minecraft GUI scales.
