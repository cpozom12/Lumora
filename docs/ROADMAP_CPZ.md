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

## M3 — In-car stationary UX

Goal: optimize the product for short parked sessions and large-touch-target interaction.

Planned work:
- Home/favorites/recent items optimized for quick access.
- Reduce navigation depth.
- Improve resume/continue-watching workflow.
- Device/head-unit compatibility matrix.
- Keep platform parked-only restrictions intact.

## M4 — Media expansion

Goal: broaden legitimate media sources while keeping the playback core maintainable.

Planned work:
- Harden IPTV/Jellyfin/Plex source handling already present upstream.
- Improve offline media workflow.
- Evaluate supported integrations/deep links for commercial streaming services.
- Do not bypass DRM, authentication, subscription controls, or Android Auto motion restrictions.

## Engineering rules

- `main` stays stable.
- Every functional/security change goes through an isolated branch and PR.
- Security hardening is a ratchet: `tools/security_gate.py` must stay green.
- Baseline/build failures are fixed before feature work.
- Upstream changes are reviewed before integration; never blindly sync executable code.
- No secrets, provider credentials, keystores, signing passwords, or API tokens in Git.
- Android Auto parked-only restrictions are never bypassed.
