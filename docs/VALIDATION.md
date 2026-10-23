# Validation result — foundation milestone

- Target: Minecraft **26.2**, Fabric, Kotlin, Java 25.
- Command: `./gradlew clean build --no-daemon`
- Result: **BUILD SUCCESSFUL**, 18 seconds, 9 executed tasks.
- Unit tests: **13 passed, 0 failed, 0 errors, 0 skipped** (7 lifecycle + 6 persistence).
- Packaged client entrypoint and core/config classes: present.
- Packaged metadata: `minecraft =26.2`, client-only environment, Kotlin adapter, Java >=25.
- No Odin/NoFrills/Stray/eppopkaaddons implementation classes in the JAR.
- JAR SHA-256: `1ee8bf80f493384c9f522cc73fe1612ac108de58fbaaeba088fa4a0867fba1bd`.
- Gradle emitted deprecation warnings concerning future Gradle 10 compatibility. No Gradle 10 compatibility is claimed.

## Not validated

Client startup, Minecraft account login, actual world/session callbacks, UI command behavior in-game, game performance, Sodium/Iris/shader integration and all upstream feature behavior. These require the user checklist in TESTING.md. There is no complete feature implementation in this milestone.

Build log and JUnit XML reports are retained in `validation/`. Toolchain metadata/source locks are in `audit/`.
