# Validation and in-game handoff

## Automated

Run `./gradlew clean build` on JDK 25. The test suite covers lifecycle idempotence, failed-start cleanup, duplicate-ID rejection, disabled-module gating, failure quarantine, world reset behavior, shutdown cleanup, JSON round trips, unknown-field retention, corrupt/future-schema preservation, invalid state validation and temporary-file cleanup.

See `VALIDATION.md` for actual results from this delivery. Compilation and unit tests are not a substitute for a real client launch.

## User acceptance checklist (not yet performed)

1. Create a clean Minecraft **26.2** Fabric profile; use the pinned Loader/API/Kotlin runtime above. Back up any existing config first.
2. Install the foundation JAR. Confirm main menu opens and log contains `solyanka foundation initialized for Minecraft 26.2`.
3. Join a test world. Run `/solyanka` and `/solyanka status`; only `core.diagnostics` is available.
4. Run `/solyanka toggle core.diagnostics`; confirm enable log. Repeat to disable; no HUD is expected.
5. Enable, disconnect, join another world/dimension, and shut down. Check no errors and persisted state in `config/solyanka.json` after restart.
6. Try an unknown module ID; confirm it returns an error, not a fake successful enable.
7. On a backed-up test config, place invalid JSON, restart and confirm the error is logged without replacing the bad original. Restore valid config afterward.
8. Repeat launch/toggle/world transitions with your actual **26.2-compatible** Sodium version. This foundation does not yet hook rendering, and Sodium/Iris compatibility has not been tested here.

## Reporting a failure

Provide exact Minecraft/Loader/API/Kotlin and optional-mod versions, OS, Java version, latest.log/crash report and reproduction steps. Do not share session tokens or account secrets. Keep launcher authentication intact; no authentication modifications are required by this starter.
