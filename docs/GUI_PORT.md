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
- Shows only three registered modules; it does not copy unrelated NoFrills features or expose unimplemented toggles.
- `/solyanka` directly requests opening the GUI next client tick, avoiding chat-close races. `/solyanka config`, Right Shift and Mod Menu converge on the same screen factory.
- Keeps main left-click toggles immediate, but stages row edits until explicit Save & back. Editor Done stages one row; root Cancel abandons the draft. This deliberately differs from upstream auto-save settings to preserve validation and file safety.
- Keeps keyboard vs mouse distinction, focus/connection/filter checks, bounded aliases and corrupt-config protection.
- Key capture cancels with Escape and unbinds with Backspace; modifier selection is separate. It no longer opens a separate vanilla/YACL capture screen.
- Centers settings panels, adapts widths to the GUI and limits the main list to known categories. Exact upstream pixel identity is not promised.
- Releases screen-owned native cursor resources when replacing screens; returning screens rebuild adapters as needed.

## Preserved limitations

No in-game launch, screenshot/visual confirmation, signed chat test or Sodium/Iris test was performed. Unit tests and compile-time API checks do not prove runtime layout correctness. Tab parsing supports standard current Area/Dungeon/player-class labels only; alias completion nodes and other feature modules remain pending.

See TESTING.md for the acceptance sequence.
