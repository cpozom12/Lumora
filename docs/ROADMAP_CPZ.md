# CPZ Roadmap

## M0 — Fork stabilization

Goal: prove the upstream application is reproducible in the CPZ fork before changing product identity.

Exit criteria:
- CI green: lint, tests and debug APK build.
- Debug APK installed successfully.
- Phone playback sanity check completed.
- Android Auto stationary-mode compatibility verified on target hardware.

## M1 — Independent product identity

Goal: make the fork independently installable and releasable without breaking the validated baseline.

Planned work:
- Freeze product name.
- Change namespace/application id in one controlled migration.
- Replace branding/assets and inherited updater references.
- Create CPZ signing key outside the repository.
- Configure CPZ GitHub Releases and release signing secrets.
- Preserve MIT attribution/license requirements.

## M2 — In-car stationary UX

Goal: optimize the product for short parked sessions and large-touch-target interaction.

Planned work:
- Home/favorites/recent items optimized for quick access.
- Reduce navigation depth.
- Improve resume/continue-watching workflow.
- Device/head-unit compatibility matrix.
- Keep platform parked-only restrictions intact.

## M3 — Media expansion

Goal: broaden legitimate media sources while keeping the playback core maintainable.

Planned work:
- Harden IPTV/Jellyfin/Plex source handling already present upstream.
- Improve offline media workflow.
- Evaluate supported integrations/deep links for commercial streaming services.
- Do not bypass DRM, authentication, subscription controls, or Android Auto motion restrictions.

## Engineering rules

- `main` stays stable.
- Every functional change goes through an isolated branch and PR.
- Baseline/build failures are fixed before feature work.
- Upstream changes are reviewed before integration.
- No secrets, provider credentials, keystores, or signing passwords in Git.
