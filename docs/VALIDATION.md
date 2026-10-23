# Validation — 0.2.0-dev (M1)

- Exact target: Minecraft **26.2**, Fabric, Kotlin, JVM 25.
- Command: `./gradlew clean build --no-daemon`.
- Result: **BUILD SUCCESSFUL**.
- **50 tests passed; 0 failures, 0 errors, 0 skipped.**
- Suites: 7 lifecycle, 6 foundation config, 20 command resolver/validation, 11 input/context policy and 6 profile/transaction/migration tests.
- JAR metadata, required YACL dependency, optional Mod Menu entrypoint and packaged LICENSE/NOTICE verified.
- Both compiled input injection descriptors/annotations match Minecraft 26.2 target descriptors statically.
- Resolved Mixin compatibility enum includes JAVA_25.
- No third-party mod implementation classes or YACL/Mod Menu artifacts are bundled.
- Ordinary JAR SHA-256: `eb5e8db2412e1f578a083a0df1c8f304f16a931a692c5f6628c80935f0c341d0`.
- Gradle emits deprecation warnings about future Gradle 10 compatibility; no Gradle 10 support is claimed.

## Not validated

A real Minecraft launch, actual Mixin transformation/application, YACL navigation/capture/save/cancel in-game, signed chat or server command behavior, nonstandard tab layouts, mod ordering and Sodium/Iris compatibility. Static descriptor checks are not a substitute for runtime mixin testing. See TESTING.md.

The companion source ZIP contains GPLv3 project source/build scripts, tests, source references, audit and validation evidence. External runtime mods are separate dependencies. M0 historical reports are retained under validation/history/m0.
