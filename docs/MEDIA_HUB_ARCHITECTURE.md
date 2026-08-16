# CPZ Media Hub Architecture

## Product definition

The CPZ fork is a media hub, not an IPTV-only client. IPTV is one provider family among several.

Baseline product goals:
- IPTV / live TV
- Jellyfin and Plex-style personal media servers already inherited from upstream
- Netflix as a first-class commercial streaming target
- Disney+ as a first-class commercial streaming target
- Expandable support for Prime Video, Max, YouTube, Apple TV+, Paramount+, Crunchyroll and other legitimate services where a supported integration path exists
- Phone, TV/large-screen and parked-car UX, while keeping platform motion/driver-distraction restrictions intact

## Personal-first delivery strategy

The first usable product is a CPZ-owned APK for personal sideloading and validation from the CPZ repository. Public distribution, Play Store eligibility and commercial packaging are later milestones.

### Personal experimental build

Initial priority:
- produce a reproducible, signed CPZ APK stored/released from the CPZ repository;
- install it manually on the owner's Android phone;
- expose one CPZ Media Hub entry in Android Auto;
- inside that hub, present provider tiles such as Netflix, Disney+, IPTV, Plex, Jellyfin, YouTube, Prime Video and Max;
- validate the experience on the target phone/head unit before optimizing for public distribution.

The current `android:appCategory="game"` marker may remain temporarily in the personal/experimental build as an Android Auto parked-app compatibility mechanism. It is not a product claim that CPZ Media Hub is a game and must not be used as the basis for a future public/Play Store release.

The experimental build must still preserve Android Auto's parked-only lifecycle. No code may defeat, neutralize or work around the platform behavior that exits/blocks parked apps when vehicle motion is detected.

### Driving transition policy

The personal build should preserve playback continuity without preserving moving-video output.

User preference:
- `On motion: continue audio` (recommended/default)
- `On motion: pause playback`

State behavior:
- parked: full authorized video/audio playback;
- motion detected: hide/stop the video surface and either continue audio or pause according to the user preference;
- preserve content id, provider, playback position, queue and session state;
- parked again: restore the video surface and offer fast resume from the preserved position;
- navigation/voice/audio controls may remain available only through platform-supported surfaces.

This preference controls CPZ playback behavior only. It is not a switch for bypassing Android Auto's parked-app lifecycle, vehicle-motion enforcement or driver-distraction restrictions.

### Future public build

A later public version must use a platform-supported category and distribution path appropriate to its actual functionality. If Android Auto gains generally available video/media-hub support, the public build can migrate to it. Until then, public compliance work is intentionally separate from the personal experimental APK.

Keep the personal and public concerns separable so a future compliance migration does not require rewriting the provider/core architecture.

## Provider integration tiers

Every provider must declare one integration tier. The UI can present providers consistently, but playback/authentication behavior must stay provider-specific.

### Tier A — Native playback

The CPZ app owns the catalog/provider session and can hand an authorized media URI/license configuration to the trusted playback core.

Examples:
- M3U/M3U8 and compatible IPTV sources
- Xtream/Stalker-style sources where retained and hardened
- user-owned/local media
- personal media servers where the protocol/API is legitimately available

Requirements:
- credentials stored with AndroidKeyStore-backed protection
- no provider secret in logs, backups, QR payloads or Git
- cleartext HTTP disabled by default and only allowed by explicit per-provider opt-in if product policy permits it
- DRM only through documented platform/provider mechanisms

### Tier B — Official-app handoff

The CPZ app acts as a media launcher/hub, but authentication, DRM and playback remain inside the provider's official Android application.

Initial mandatory targets:
- Netflix
- Disney+

Next candidates:
- Prime Video
- Max
- YouTube
- Apple TV+
- Paramount+
- Crunchyroll

Capabilities should be detected instead of assumed:
- app installed / not installed
- launch provider home
- open a supported public/deep link when the provider documents or reliably supports one
- fall back to a safe install/open instruction on the phone

Do not scrape credentials, copy provider cookies/tokens, impersonate the official app, intercept DRM licenses, or embed a provider website in a WebView as a substitute for an authorized integration.

### Tier C — Authorized partner/native commercial integration

Reserved for providers that expose a documented SDK/API or approve a device/application integration that allows catalog, entitlement and/or playback inside the CPZ app.

Netflix and Disney+ must remain eligible to move from Tier B to Tier C if an official/contracted path becomes available.

Entry criteria:
- official provider documentation or partner agreement
- documented authentication and entitlement flow
- documented DRM/license-server requirements
- no reverse-engineered private API dependency
- legal/commercial review before release

## Why Widevine support alone is not enough

Android Media3/MediaDrm can play Widevine-protected DASH/HLS when the application has the correct DRM configuration and license-server access. Commercial services additionally control authentication, device authorization, entitlement, manifests and license issuance. Therefore platform Widevine support does not itself create a supported Netflix/Disney+ integration.

## Android Auto / car target

Treat car support as a presentation target, not as a reason to couple provider logic to Android Auto.

The provider layer must work independently of the display surface:
- phone
- TV / large screen
- Android Automotive OS where supported
- Android Auto only in categories and modes supported by the platform

No code may bypass platform motion/driver-distraction restrictions.

## Proposed code boundaries

Keep provider and presentation concerns separate:

- `ProviderRegistry`: known providers, capabilities and integration tier
- `ProviderCapability`: browse, search, resume, nativePlayback, externalLaunch, deepLink, offline, drm
- `ProviderAdapter`: provider-specific authentication/catalog behavior for Tier A/C
- `ExternalProviderLauncher`: Tier B launch/deep-link handling with strict package/link allowlists
- `PlaybackCore`: Media3/ExoPlayer only for authorized native sources
- `DrivingTransitionController`: preserves session state and applies the configured audio-only/pause behavior when motion is detected without bypassing platform restrictions
- `CarPresentation`: parked-safe presentation layer; no provider credentials or DRM logic

The UI should consume capabilities rather than contain provider-specific branching wherever possible.

## Security ratchets

- Do not reintroduce remote executable plugins to add streaming services.
- Do not embed credentials or service tokens.
- Do not use undocumented private Netflix/Disney APIs as production dependencies.
- Do not disable TLS verification to accommodate a provider.
- Do not proxy/decrypt DRM streams to make them playable by the generic player.
- Keep `tools/security_gate.py` green and extend it when new provider surfaces introduce new trust boundaries.

## Delivery order

1. Stabilize and validate the hardened inherited app.
2. Produce the personal experimental CPZ APK and validate Android Auto parked-mode presence/lifecycle on the target hardware.
3. Harden and configure IPTV playback, provider credentials and cleartext policy.
4. Implement the driving transition controller: full playback when parked, configurable audio-only/pause behavior on motion, fast video resume when parked again.
5. Introduce provider capability abstractions without changing existing playback behavior.
6. Add Netflix and Disney+ provider tiles/official-app handoff as baseline commercial-streaming integrations where the platform allows it.
7. Add additional commercial providers through the same Tier B abstraction.
8. Evaluate each provider for a legitimate Tier C path; promote only when an official integration is available.
9. Optimize phone/TV/parked-car surfaces without duplicating provider logic.
10. Only after the personal product is stable, design the separate public/compliant distribution profile.
