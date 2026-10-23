# Validation — 0.2.1-dev, NoFrills GUI port

- Exact target: Minecraft **26.2**, Fabric, Kotlin, JVM 25.
- Command: `./gradlew clean build --no-daemon` — **BUILD SUCCESSFUL**.
- **60 tests passed; 0 failures/errors/skipped.** Existing 50 command/core/persistence tests plus 10 GUI catalog/draft tests.
- Compiled NoFrills-derived screen, controls and settings panel classes present in the JAR.
- Root `/solyanka` handler requests the GUI; `/solyanka config` remains an alias.
- Runtime metadata requires owo, not YACL; current Kotlin source/Gradle dependency declarations have no YACL references.
- GPL LICENSE/NOTICE included in ordinary and source JARs.
- Resolved owo artifact SHA-512 matches the published primary artifact metadata.
- Existing input mixin target/injection descriptors still match Minecraft 26.2 statically.
- No external library or NoFrills runtime classes are bundled.
- JAR SHA-256: `9922272dacd0c1a3f6479a9ba14c547f64607a031b12eb6eff3e66ed6b0c7269`.
- Gradle emits deprecation warnings about future Gradle 10; Gradle 10 support is not claimed.

## Not validated

A Minecraft launch, actual GUI appearance or screenshot, dynamic layout/scale/scroll, inline capture/navigation/save/cancel interaction, native cursor cleanup during gameplay, actual Mixin transformation, signed chat/server commands and Sodium/Iris/mod-order compatibility. Source/compile/static ABI checks and pure-model unit tests do not prove these runtime behaviors.

See TESTING.md. Historical 0.1/0.2 evidence is under validation/history/. GUI source trace and external dependency evidence are under audit/gui/.
