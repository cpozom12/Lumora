# CPZ Media Hub

Independent Android application owned by CPZ. Version 1.0.0.

## Security model

- Android platform APIs only; no runtime third-party dependencies.
- No `uses-permission` declarations.
- No own network stack, URLs, WebView, native libraries, background services, receivers or providers.
- Static package allow-list for six official media applications.
- No credentials, DRM handling, scraping, plugins, torrent/P2P, updater or remote executable content.
- Android backup and cleartext traffic are disabled.
- Release builds are minified, resource-shrunk and non-debuggable.

## Android Auto

The manifest declares `CAR_LAUNCHER` so Android can evaluate the app for parked surfaces. It does not declare a false game or video category and does not bypass host restrictions. Android Auto decides whether the activity is eligible and controls parked/moving availability.

## Permanent signing

The permanent signing key must be generated and retained only on a trusted local machine. Do not upload the keystore or its password to source control, chat, CI artifacts or cloud storage unless an independently secured signing system is intentionally configured.

Run `build-permanent-release.ps1` on the trusted Windows development machine. If the keystore does not exist, the script invokes `keytool` locally. It then asks for the passwords through secure prompts, exposes them only as temporary environment variables to the Gradle process, builds the signed non-debuggable release and clears the signing environment afterwards.

The `.jks` file is excluded by `.gitignore`. Future updates MUST use the same keystore and alias. Losing that key means Android will not accept future APKs as updates to the installed app.

CI deliberately builds only an unsigned release for reproducible binary auditing. CI never receives the permanent signing key.

## Third-party names

Names such as Movistar TV App, Netflix, Disney+, YouTube, Prime Video and HBO Max are used only to identify the corresponding official installed applications. CPZ Media Hub does not bundle their code, content, credentials or branding assets and does not claim affiliation or endorsement.

Copyright (c) 2026 Christian Anthony Pozo Mejía. All rights reserved.
