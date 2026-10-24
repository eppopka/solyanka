# Validation — 0.5.0-dev

The ordinary JAR and companion sources are built with `./gradlew clean build --no-daemon`. `validation/summary.json` contains the exact clean-build/test counts and JAR hash. JUnit XML and build log are retained; previous delivery evidence lives in validation/history/v0.4.0.

Static checks cover mixin class entries, official AW targets, exact selected Minecraft target descriptors, selected callsite counts and packet thread guards. Solver resource SHA256/structure, room transforms, score arithmetic, experiment state and bounded recipe/price/waypoint file behavior have pure tests.

These checks do **not** launch Minecraft or apply its mixins in a live loader. No GPU, Hypixel, Sodium/Iris or runtime compatibility acceptance is claimed. docs/TESTING.md is the required in-game follow-up. All new feature families and remaining source parity are explicitly separated in M4.md and FEATURE_MATRIX.md.
