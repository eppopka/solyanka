# M1 dependency/source evidence

YACL/Mod Menu version metadata includes game/loader compatibility, primary artifact URLs and upstream hashes. Primary downloads were checked against SHA-512 metadata. Maven coordinates are pinned in gradle.properties/build.gradle.kts. No third-party mod JAR is included in the source bundle or solyanka JAR.

NoFrills behavior references use the original source-lock commit in ../source-lock.json. The YACL API was checked against the exact published 26.2 binary; its repository HEAD (97f89e5bd0eff3811324680b6dec9e0e1009b8c0) was supplementary documentation, not a pinned implementation dependency. Mod Menu API and Fabric sources were inspected at their resolved versions. No UI-library implementation code is copied into solyanka.

Compile/static API checks are not runtime compatibility validation.
