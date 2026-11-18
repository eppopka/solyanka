# solyanka 1.4.8 — source candidate

## Status — read before installing

This is a complete **source project**, not a verified binary release. No compiled mod JAR is included.
The Java 25 runtime is available, but `./gradlew build --no-daemon` stopped before compilation:
`java.net.UnknownHostException: services.gradle.org`, while fetching the configured Gradle 9.7.1 distribution.
The sandbox has neither cached Gradle/Minecraft dependencies nor a Kotlin compiler. Kotlin compilation,
JUnit tests, Mixin ABI verification, Windows integration, GUI visual QA and in-game behavior were **not** run.
Existing reports under `validation/` and `audit/` came from the uploaded project and are historical;
they do not establish the correctness of 1.4.8. This release's actual check results are in
`validation/1.4.8/summary.json` and its build attempt log.

## Build on a network-enabled machine

1. Install a JDK 25 distribution.
2. Extract the project, open a terminal in its root, and run:
   - Linux/macOS: `./gradlew clean build --no-daemon`
   - Windows: `gradlew.bat clean build --no-daemon`
3. Resolve any compiler/test failures before considering this candidate a release.
4. The expected runtime artifact after a successful build is `build/libs/solyanka-1.4.8.jar`.
   Do not install the sources JAR or the Gradle wrapper JAR as a mod.
5. Test with the Fabric/Minecraft versions already declared in `gradle.properties` (Minecraft 26.2).

## Implemented source changes

- Version changed to 1.4.8. GUI header reads the mod and Minecraft versions from Fabric metadata;
  the old CLIENT UTILITIES subtitle is removed.
- Recently changed is the last/rightmost category column (horizontal scrolling still applies).
- GUI Settings moved to HUD; Bestiary ESP, Tiki ESP and Invisibug ESP moved to General.
  Titanium ESP and Spread Guesser moved to Mining. Stable module IDs and config keys are retained.
- Missing/failed artwork collapses the shared media HUD from 208 to 150 pixels wide. No empty
  cover slot or music-note placeholder remains; artwork success restores the wider layout.
- Spotify is a separate HUD-category module using Windows desktop media sessions. Enable the module,
  open its settings, click Connect Spotify desktop, and play a track in the Spotify app.
  Track title/artist, progress and available cover art use the existing bounded media bridge.
  Only Spotify.exe / Spotify's Store package is selected, never arbitrary media apps.
  This supports Windows 10/11 desktop Spotify, **not** Linux/macOS, the web player, Spotify Web API,
  Spotify account authentication, queue control or Spotify !suggest. No Spotify secrets are collected.
  Spotify and Yandex share one HUD and its placement/appearance/lyrics settings; last connection wins.
- Russian feature names retain SkyBlock terms including Bestiary, Invisibug, Titanium ESP,
  Spread Guesser, Waypoints, Mineshaft, Corpse and Tracer rather than inventing literal equivalents.
- Hide Players: real-account UUID filter, excludes the local player/NPCs, distance slider,
  Hide all and Only in Dungeons controls. Uses the existing entity visibility hook.
- Auto RNG Announce: own-player `found a/an` messages, optional `just`, bracketed prefixes,
  local SkyHanni Tracker Drop messages; independent /ac, /gc and /pc targets. Default minimum is 30M.
  Numeric SkyHanni prices below the minimum are skipped; the inclusive minimum passes.
  Standard messages have no price: the explicit Announce unknown value switch controls these
  (default on). There is no invented market-price lookup. Item de-duplication lasts 30 seconds;
  pending sends are bounded, spaced 1.2 seconds apart and expire after 15 seconds.
  SkyHanni's locally displayed messages are observed using the project's existing chat accessor;
  old messages present when enabling the module are not replayed.
- Powder Chest: near-player non-chest -> chest block transitions in Crystal Hollows are marked
  through walls; Tracer defaults on. It does not scan pre-existing structure chests or load chunks.
  Only block-update packets are observed, not a confirmed mining action. Other newly placed chests
  could therefore match. Markers expire at two minutes, block removal or chunk/world unload.
- Mineshaft Helper: sidebar/TAB variant identifiers (including LITT_L), optional Party type announce,
  a stable TAB corpse summary prefixed [solyanka], and loaded-corpse helmet-ID classification.
  Lapis/Tungsten/Umber/Vanguard each have separate color and Tracer controls. Tracers disappear
  within the configured radius (default five blocks); optional coordinates stay in local chat.
  Party announcements are off by default. TAB totals include LOOTED and NOT LOOTED corpse lines.
  Detection/ESP cannot reveal entities that the server has not sent to the client.
- Explicit export of loaded corpses creates a new area/variant-scoped Waypoints pack without
  overwriting an existing pack. Select it in Waypoints, then edit it normally. Existing Waypoints
  Visible/Hidden pack buttons provide individual custom-route visibility; numbered points and
  route lines use the existing Waypoints editor. Exported corpse markers are not an authored route.

## Upstream reference scope

The supplied HidePlayers.kt, CorpseType.kt, CorpseFinder.kt and CorpseSharing.kt were reviewed.
Hide Players adapts Odin's filtering behavior; the existing BSD-3-Clause license is retained.
Corpse IDs and TAB formats are independently implemented from the supplied SkyHanni references.
SkyHanni upstream is LGPL-2.1; no SkyHanni source files are bundled.

The upstream MineshaftDetection.kt and MineshaftWaypointManager.kt on the beta branch were also
inspected via public GitHub. The inspected waypoint manager creates entrance/ladder and discovered
corpse waypoints; it does not contain a predefined per-variant traversal route pack to port.
No claim is made that a complete search of every SkyHanni asset found no routes. No unverified
route coordinates, entrance estimates or traversal routes are bundled in this project.
Powder Chest is a new block-transition implementation, not a claimed SkyHanni source port.

References:
- https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/mining/glacitemineshaft/MineshaftDetection.kt
- https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/mining/glacitemineshaft/MineshaftWaypointManager.kt

## Checks actually completed here

- 31 JVM regex-fixture assertions using patterns extracted directly from the Kotlin source.
  Re-run: `java validation/Check148Regex.java` (JDK 25).
- Source consistency: 63 unique GUI cards, requested category layout, translation key parity,
  English description/name fallbacks, module ID references, version and subtitle checks.
- `git diff --check`.
- Patch application against the originally uploaded source, and comparison of patched files
  with the packaged project (recorded by packaging validation).
- New JUnit regression cases were written for parsing, price thresholds, de-duplication,
  TAB counts, settings validation/migration, Spotify HUD visibility and category ordering.
  These JUnit cases have **not** run here.

## Manual acceptance checklist (not yet performed)

- Build and run the full JUnit suite on a machine with internet; address failures.
- Launch Minecraft 26.2/Fabric, inspect startup logs and verify existing modules still work.
- Verify category order and version header at small and large window sizes and both GUI languages.
- In both providers, exercise no art, invalid art, delayed art and successfully loaded art;
  check compact/full HUD bounds, text, lyrics, dragging, scaling and screen edges.
- On Windows, test Spotify.exe and Store Spotify; switch Yandex/Spotify sources, disable each
  module, disconnect, and close the game; ensure the correct session/process is released.
- On Hypixel, check Hide Players distance/dungeon toggles, yourself and NPCs.
- Test own/other-player RNG messages, player-chat echoes, SkyHanni local drops, 29.9M/30M/30.1M,
  unknown-value off/on, duplicate callbacks, multi-chat targets and pending sends across world changes.
- Spawn Powder Chests in Crystal Hollows; verify Tracer/color, block removal, TTL and world reset.
- Visit multiple Mineshaft variants; verify TAB totals, late helmet metadata, each corpse color,
  Tracer threshold, coordinate de-duplication and optional Party announcements.
- Export corpse points; select/edit/hide/show the new Waypoints pack in the matching variant.

## Patch and repository

The .patch is relative to the uploaded solyanka.zip project root, not a verified Git commit:
the ZIP contains no Git branch history. From an untouched extracted copy run
`git apply --check /path/to/solyanka-1.4.8.patch`, then `git apply /path/to/solyanka-1.4.8.patch`.
Do not apply the patch a second time to the already-updated project ZIP.
No GitHub fork, commit, push or pull request was created: authenticated GitHub access is unavailable.
