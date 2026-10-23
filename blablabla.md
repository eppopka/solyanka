# solyanka

**Minecraft 26.2 • Fabric • Kotlin • Java 25**

Foundation milestone only: this is not the completed combined mod. All 38 requested named features and every feature/helper in impl.zip are tracked, but upstream feature implementations are not included yet.

## Toolchain

| Component | Pinned version |
|---|---|
| Minecraft | **26.2** (exact; not 1.21.x) |
| JDK / JVM target | **25** |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.161.0+26.2 |
| Fabric Language Kotlin | 1.14.2+kotlin.2.4.21 |
| Kotlin plugin | 2.4.21 |
| Fabric Loom | 1.18.4 (`net.fabricmc.fabric-loom`) |
| Gradle wrapper | 9.7.1, distribution SHA-256 pinned |

Use the game's official unobfuscated names. Do not add Yarn mappings or replace the Loom plugin with a legacy remapping configuration. Change versions only after testing the whole toolchain.

## Build

Install a **full JDK 25**, set JAVA_HOME, then:

```sh
./gradlew clean build
```

On Windows use `gradlew.bat clean build`. First build requires network access. The ordinary `build/libs/solyanka-0.1.0-dev.jar` is the mod; the `-sources.jar` is not installable.

For a development client:

```sh
./gradlew runClient
```

Development launch/gameplay has not been tested in this environment. Use your launcher to test on a real Minecraft **26.2 Fabric** client. Put the normal mod JAR, **Fabric API for 26.2** and **Fabric Language Kotlin** in its mods folder. Loader must be at least 0.20.1. Neither runtime mod is bundled into the solyanka JAR. No server-side installation is required.

## What works in this milestone

- Client-only entrypoint and module registry.
- Idempotent enable/disable, startup cleanup and runtime-failure isolation.
- Active-module resets on world identity changes/disconnects.
- JSON persistence with schema validation, preservation of unknown settings and atomic replacement.
- `/solyanka`, `/solyanka status`, `/solyanka toggle core.diagnostics`.
- The only executable module is a lifecycle diagnostic; it logs enable/disable and exercises reset/tick paths. It does not add a HUD or upstream utility feature.

No fake toggles advertise unimplemented features. All modules default off. Configuration is at `config/solyanka.json`; it is saved on toggles and client shutdown. Corrupt or unsupported config is preserved and writes are disabled for that session; see the log. Back up and repair/remove the file, then restart to recover. No GUI/YACL screen or Mod Menu integration is implemented yet.

## Layout

```text
build.gradle.kts                  Dependencies, Loom, Kotlin and test configuration
settings.gradle.kts               Plugin repositories and version resolution
gradle.properties                Version pins
src/main/kotlin/dev/solyanka/
  core/                          Game-independent lifecycle and registry
  config/                        JSON persistence
src/client/kotlin/dev/solyanka/
  client/                        Minecraft/Fabric integration and commands
src/main/resources/fabric.mod.json
src/test/kotlin/                 Lifecycle and config tests
audit/                           Source locks, feature index, attachment inventory, licenses
docs/                            Architecture, feature matrix, audit, roadmap, test handoff
```

Read `docs/ROADMAP.md` for the next implementation slices. Read `docs/AUDIT.md` before importing code. In particular, GPLv3 NoFrills integration affects distribution; the archive lacks its customized framework/mixins/assets; and Stray PiP currently uses Windows window capture.

## Licensing

No upstream implementation or artwork is compiled in this milestone. A license for the project's original code has not been selected on the owner's behalf; `fabric.mod.json` uses conservative all-rights-reserved metadata until that decision. `audit/licenses/` is source-audit evidence, not a claim that all referenced assets inherit those licenses. Gradle wrapper bootstrap files came from FabricMC's CC0 example project. See `NOTICE` and `docs/AUDIT.md`.
