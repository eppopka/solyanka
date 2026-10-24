# 1.3.1 — GUI polish and safer settings lifecycle

- Rounded dark-navy cards/panels, softer sky-blue accents, clearer category headings, consistent buttons and muted descriptive text.
- Wider/taller settings viewport, keeping the world background undimmed. Settings stay in the same Minecraft Screen and continue saving valid edits immediately.
- **Drag the settings title bar with LMB** to move the panel. Back and body controls do not start dragging. Release stops it. Position is clamped to the viewport and retained while switching settings within this GUI session; it is not written into feature configuration.
- Slider/color dragging remains separate from window movement. Resize cancels an active drag and clamps the panel.
- Opening/closing settings through the host is deferred to the next frame boundary, not performed inside the old widget's mouse callback. A new tree must mount successfully before the previous panel is disposed.
- Native HSV palettes are created only after clicking Edit color, not eagerly when opening Held Item or any other settings section.
- Settings construction/input/extraction failures are logged to `logs/latest.log` and return to a usable GUI rather than escaping those UI boundaries. This does **not** catch later GPU submission failures, Java Errors or failures elsewhere in Minecraft.

## Held Item crash status

The user's actual crash report has not been supplied. No exact root cause has been confirmed or reproduced. These are defensive GUI lifecycle changes, **not a verified fix for that crash**. If Held Item still crashes, provide the newest crash report and `logs/latest.log`; the shader/runtime path may require a separate fix.

## Validation boundary

The drag state machine, navigation queue and GUI source contracts have automated tests. Clean compilation and prior native/shader checks are rerun. No Minecraft GUI was launched here; visual polish, actual pointer behavior and the reported crash need in-game testing. Existing unimplemented features and source-parity limitations from M13.md remain unchanged.
