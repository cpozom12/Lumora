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
2. Introduce provider capability abstractions without changing existing playback behavior.
3. Add Netflix and Disney+ Tier B provider cards/launchers as baseline commercial-streaming integrations.
4. Add additional commercial providers through the same Tier B abstraction.
5. Evaluate each provider for a legitimate Tier C path; promote only when an official integration is available.
6. Optimize phone/TV/parked-car surfaces without duplicating provider logic.
