# CPZ Roadmap

## M0 — Fork stabilization

Goal: prove the upstream application is reproducible in the CPZ fork before changing product behavior.

Current state:
- CI baseline has passed lint, tests, wrapper validation and debug APK build.
- Security-hardening branch now adds an APK-level regression gate.
- Installation on the primary phone remains blocked until the latest hardened artifact passes the full trust gate.

Exit criteria:
- CI green: wrapper validation, lint, tests, debug APK build and security gate.
- Hardened debug APK provenance/hash recorded.
- Debug APK installed successfully.
- Phone playback sanity check completed.
- Android Auto stationary-mode compatibility verified on target hardware.

## M1 — Independent product identity

Goal: make the fork independently installable and releasable without relying on upstream release infrastructure.

Current state:
- CPZ-owned application id assigned: `com.cpozom.lumora`.
- Kotlin namespace remains `com.lumora` temporarily to keep the security migration reviewable.
- Upstream in-app updater/install path is disabled.
- Upstream automatic release workflow is removed.
- MIT license attribution must be preserved.

Remaining work:
- Freeze final product/launcher name and branding assets.
- Decide whether/when to rename the Kotlin namespace.
- Create CPZ signing key outside the repository.
- Configure a minimal CPZ GitHub Releases/signing pipeline after the signing identity is ready.
- Record release signer fingerprint and artifact hashes.

## M2 — Trusted media core

Goal: ship only the media-client surfaces we intentionally trust.

Current state:
- Provider/media-server secrets use AndroidKeyStore-backed AES-GCM storage.
- Android backup is disabled.
- Plaintext manual backup/import is disabled.
- Remote executable JS plugins are disabled and QuickJS removed.
- Credential-bearing LAN QR pairing is disabled.
- Torrent/libtorrent/NanoHTTPD runtime is removed/disabled.
- Third-party scraper providers remain quarantined.
- Dependabot monitors Gradle and GitHub Actions dependencies.

Remaining work:
- Decide explicit cleartext-provider policy (`https://` only vs explicit opt-in for `http://`).
- Physically remove quarantined legacy scraper/torrent/plugin compatibility source after functional validation.
- Add further security scanning where useful without weakening reproducibility.

## M3 — Media-hub provider architecture

Goal: make the product a provider-neutral media hub rather than an IPTV-centric application.

Architecture source of truth:
- `docs/MEDIA_HUB_ARCHITECTURE.md`

Required provider families:
- IPTV / live TV
- Jellyfin and Plex-style personal media
- Netflix as a first-class commercial streaming target
- Disney+ as a first-class commercial streaming target
- extensible support for Prime Video, Max, YouTube, Apple TV+, Paramount+, Crunchyroll and other legitimate services where a supported integration path exists

Integration policy:
- Native playback only where the app has a legitimate protocol/API and authorized media/DRM flow.
- Netflix and Disney+ initially target official-app handoff/deep-link integration; they may move to native/partner integration only through a documented/authorized provider path.
- Never scrape credentials, reuse private provider tokens, bypass DRM, or make undocumented private APIs a production dependency.

Planned work:
- Introduce provider capability abstractions without changing inherited playback behavior.
- Implement a provider registry and external-provider launcher.
- Add Netflix and Disney+ provider cards/launch paths as baseline commercial integrations.
- Add additional commercial providers through the same abstraction.
- Evaluate official partner/SDK paths separately for each commercial provider.

## M4 — In-car stationary UX

Goal: optimize the product for short parked sessions and large-touch-target interaction.

Planned work:
- Home/favorites/recent items optimized for quick access.
- Reduce navigation depth.
- Improve resume/continue-watching workflow.
- Device/head-unit compatibility matrix.
- Keep platform parked-only restrictions intact.
- Keep provider logic independent from the car presentation layer.

## M5 — Media expansion

Goal: broaden legitimate media sources while keeping the playback core maintainable.

Planned work:
- Harden IPTV/Jellyfin/Plex source handling already present upstream.
- Improve offline media workflow where provider rights permit it.
- Expand the commercial provider registry beyond Netflix and Disney+.
- Evaluate documented/authorized native integrations provider by provider.
- Do not bypass DRM, authentication, subscription controls, or Android Auto motion restrictions.

## Engineering rules

- `main` stays stable.
- Every functional/security change goes through an isolated branch and PR.
- Security hardening is a ratchet: `tools/security_gate.py` must stay green.
- Baseline/build failures are fixed before feature work.
- Upstream changes are reviewed before integration; never blindly sync executable code.
- No secrets, provider credentials, keystores, signing passwords, or API tokens in Git.
- Android Auto parked-only restrictions are never bypassed.
- Commercial providers must use documented/authorized integration paths; external launch is preferred over brittle WebView/private-API emulation.
