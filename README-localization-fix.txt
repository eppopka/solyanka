1.4.8.2 localization build fix

Tracker's shortened English translation differed from its catalog fallback.
Fixed the catalog to match the concise translation, retaining the original tests.
Removed four redundant Int conversions in GuiKit.kt:149.

Small ZIP: extract into the existing project root and replace the two files.
Complete ZIP: extract into a fresh folder; includes all previous updates.
Then run ./gradlew build (do not skip tests). Version remains 1.4.8.2.

Checked all 65 feature description/name fallbacks and Russian translations,
locale key parity, source whitespace, archive CRC and full ZIP source equality.
The original LocalizationTest is unchanged.
No full Gradle/JUnit rerun here: previous build was blocked downloading Gradle
because services.gradle.org DNS could not be resolved. No JAR included.
