# CPZ Media Hub Architecture

## Product definition

The CPZ fork is a personal media hub for Android/Android Auto parked use. The initial product is no longer IPTV-first.

Primary V1 provider targets:
- Movistar TV App
- Netflix
- Disney+
- YouTube
- Prime Video
- Max
- Plex / Jellyfin where useful

IPTV/M3U/Xtream support may remain available from the inherited codebase only if it survives security hardening and is useful later, but it is not a V1 requirement and must not drive product architecture.

Optional mobility utility shortcuts such as inDrive and DiDi are evaluated separately from the media provider layer.

## Personal-first delivery strategy

The first usable product is a CPZ-owned APK for personal sideloading and validation from the CPZ repository. Public distribution, Play Store eligibility and commercial packaging are later milestones.

### V1 personal scope — Media Hub

1. finish security hardening;
2. produce a reproducible CPZ APK;
3. install it manually on the owner's Android phone;
4. expose one CPZ Media Hub entry in Android Auto as a parked experience;
5. present provider tiles for Movistar TV App, Netflix, Disney+, YouTube, Prime Video, Max and other selected services;
6. detect whether each official provider app is installed;
7. hand off to the official provider app/deep link where Android and the provider permit it;
8. preserve CPZ-owned state needed to return cleanly to the hub;
9. record exact APK provenance, signer and hash.

The current `android:appCategory="game"` marker may remain temporarily in the personal/experimental build as an Android Auto parked-app compatibility mechanism. It is not a product claim that CPZ Media Hub is a game and must not be used as the basis for a future public/Play Store release.

V1 does not contain a motion-bypass feature. When Android Auto exits or blocks the parked activity, CPZ Media Hub should persist its own navigation/session state so the experience can resume when parked use becomes available again.

The app should not force-launch Spotify or another media application when leaving parked mode. It should release its media/session focus cleanly and allow Android Auto to fall back to the user's last active supported audio player according to platform behavior.

## Commercial provider strategy

Movistar TV App is the first real commercial provider target because the user already has an active account/application.

Netflix and Disney+ are mandatory V1/V1.1 targets. Prime Video, Max, YouTube and other services use the same provider abstraction.

Commercial provider rules:
- prefer official Android applications and documented deep links;
- never capture or reuse provider passwords, cookies, session tokens or DRM material;
- never impersonate an official application;
- never embed a provider website as a DRM/authentication workaround;
- never depend on undocumented private APIs for production behavior;
- never weaken TLS validation for a provider;
- provider integrations must be replaceable without changing the hub core.

## Provider integration tiers

### Tier A — Native/trusted playback

Used only where CPZ legitimately controls or is authorized to consume the playback source and license configuration, such as user-owned media or supported personal media servers.

### Tier B — Official-app handoff

The default for commercial streaming services. CPZ Media Hub presents the provider tile and launches the provider's official Android experience when the platform permits it.

Initial Tier B targets:
- Movistar TV App
- Netflix
- Disney+
- Prime Video
- Max
- YouTube

Capabilities are detected rather than assumed:
- app installed / not installed
- launch provider home
- supported public/deep link
- safe fallback when a head-unit launch is not supported

### Tier C — Authorized partner/native commercial integration

Reserved for providers that expose a documented SDK/API or approve a device/application integration that allows catalog, entitlement and/or playback inside CPZ Media Hub.

## Mobility utility shortcuts

inDrive, DiDi and similar driver apps are separate Android applications, not libraries to package inside CPZ Media Hub.

A future utility tile may:
- detect whether the official app is installed;
- launch the official app on the phone when Android permits it;
- preserve a consistent hub UX;
- degrade safely when Android Auto cannot present that app on the head unit.

Do not clone, repackage or inject into the official driver apps.

## Proposed code boundaries

- `ProviderRegistry`: known providers and capabilities
- `ProviderCapability`: externalLaunch, deepLink, nativePlayback, search, resume, offline, drm
- `ProviderAdapter`: provider-specific behavior where a supported integration exists
- `ExternalProviderLauncher`: strict package/link allowlists for commercial providers and utility handoffs
- `PlaybackCore`: Media3/ExoPlayer only for authorized native sources
- `HubStateStore`: persist selected provider, hub navigation and resumable CPZ-owned state
- `CarPresentation`: parked presentation layer; no provider credentials or DRM logic

## Security ratchets

- Do not reintroduce remote executable plugins to add services.
- Do not embed credentials or service tokens.
- Do not use undocumented private Netflix/Disney/Movistar APIs as production dependencies.
- Do not disable TLS verification to accommodate a provider.
- Do not proxy/decrypt DRM streams to make them playable by the generic player.
- Keep `tools/security_gate.py` green and extend it when new provider surfaces introduce new trust boundaries.

## Delivery order

1. Finish hardening and get CI/security gate green.
2. Rebuild and audit the hardened APK.
3. Build the provider-tile Media Hub shell.
4. Add Movistar TV App detection/official-app handoff.
5. Add Netflix and Disney+ detection/official-app handoff.
6. Add YouTube, Prime Video and Max using the same abstraction.
7. Validate phone behavior.
8. Validate Android Auto parked-mode presence and provider-handoff behavior on the target hardware.
9. Freeze/sign the first personal V1 APK.
10. Evaluate Plex/Jellyfin and any retained IPTV functionality only if it adds value.
11. Evaluate inDrive/DiDi utility shortcuts separately from the media provider layer.
12. Only after the personal product is stable, design the public/compliant profile.
