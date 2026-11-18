# Local music reader

`src/client/resources/solyanka-yandex-local.ps1` is intentionally bundled in the mod JAR. On Windows, the Yandex Music integration loads this script as a classpath resource to read local media information. Do not remove it as an unused artifact.


Yandex account sign-in uses OAuth authorization code with PKCE; no OAuth client secret is shipped in the client mod. The Yandex OAuth app must allow `https://oauth.yandex.com/verification_code` as a Redirect URI. Start sign-in in the music settings, open the supplied page, approve access, enter the displayed 7-digit code in the panel, and submit it. Any client secret previously present in a public source or mod build should be rotated in Yandex OAuth settings; deleting it from a new version does not remove it from old Git history or already downloaded JARs.
