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

The manifest declares `CAR_LAUNCHER` so Android can evaluate the app for parked surfaces. It does not declare a false game or video category and does not bypass host restrictions. Android Auto decides whether the activity is eligible and automatically controls parked/moving availability.

## Permanent signing

The permanent signing key must be generated and retained only on a trusted local machine. Do not upload the keystore or `signing.properties` to source control, chat, CI artifacts or cloud storage unless an independently secured signing system is intentionally configured.

When `signing.properties` exists locally, the release build uses that stable key. Without it, Gradle can build an unsigned release artifact for audit but that artifact is not the permanent installable release.

## Third-party names

Names such as Movistar TV App, Netflix, Disney+, YouTube, Prime Video and HBO Max are used only to identify the corresponding official installed applications. CPZ Media Hub does not bundle their code, content, credentials or branding assets and does not claim affiliation or endorsement.

Copyright (c) 2026 Christian Anthony Pozo Mejía. All rights reserved.
