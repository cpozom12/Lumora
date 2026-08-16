# CPZ Media Hub Architecture

## Product definition

The CPZ fork is a personal media hub, not an IPTV-only client. IPTV is the first provider family to make production-ready because it is already present upstream and gives us the fastest path to a useful, testable Android Auto parked experience.

Longer-term provider targets:
- IPTV / live TV
- Jellyfin and Plex-style personal media servers already inherited from upstream
- Netflix and Disney+ as first-class commercial streaming targets
- Prime Video, Max, YouTube, Apple TV+, Paramount+, Crunchyroll and other legitimate services where a supported integration path exists
- optional mobility utility shortcuts such as inDrive and DiDi where Android/Android Auto actually permits a useful handoff

## Personal-first delivery strategy

The first usable product is a CPZ-owned APK for personal sideloading and validation from the CPZ repository. Public distribution, Play Store eligibility and commercial packaging are later milestones.

### V1 personal scope — keep it simple

The V1 goal is intentionally narrow:

1. finish security hardening;
2. produce a reproducible CPZ APK;
3. install it manually on the owner's Android phone;
4. expose the app in Android Auto as a parked experience;
5. configure one trustworthy IPTV source;
6. prove stable live playback, EPG/favorites/resume where supported, credential persistence and reconnect behavior while parked;
7. record exact APK provenance, signer and hash.

The current `android:appCategory="game"` marker may remain temporarily in the personal/experimental build as an Android Auto parked-app compatibility mechanism. It is not a product claim that CPZ Media Hub is a game and must not be used as the basis for a future public/Play Store release.

V1 does not contain a motion-bypass feature. When Android Auto exits/blocks the parked activity, playback state should be persisted so the same content can be resumed when the parked experience becomes available again.

Automatic switching to Spotify or another audio app is not a V1 requirement. It can be evaluated later as an interoperability feature, subject to Android background-launch and Android Auto behavior.

### IPTV trust policy

The embedded IPTV player is only for streams/accounts the user is authorized to access.

Provider acceptance criteria:
- M3U/M3U8, Xtream or another protocol intentionally supported by the trusted player;
- HTTPS preferred;
- cleartext HTTP, if retained at all, must require an explicit per-provider opt-in and visible warning;
- credentials stored only through AndroidKeyStore-backed protection;
- no provider credentials in Git, logs, backups, QR payloads or analytics;
- no requirement to install a second unknown APK;
- no remote executable plugin requirement;
- no disabled TLS certificate or hostname validation;
- provider must be replaceable without changing the playback core.

For initial functional validation we may use a public/free playlist source with no secret credentials. A paid provider should only be configured after its legitimacy, protocol and security posture are reviewed.

### V2 — Media Hub providers

After V1 IPTV is stable, introduce a provider-oriented home screen. The desired parked UI is one CPZ Media Hub entry containing tiles such as:

- IPTV
- Netflix
- Disney+
- Plex
- Jellyfin
- YouTube
- Prime Video
- Max

Commercial streaming services remain official-app handoffs unless a legitimate native integration path exists. Do not scrape credentials, copy provider cookies/tokens, impersonate official apps, intercept DRM licenses, or embed provider websites as a substitute for an authorized integration.

### Mobility utility shortcuts

inDrive, DiDi and similar driver apps are separate Android applications, not libraries to package "inside" CPZ Media Hub.

A future utility tile may:
- detect whether the official app is installed;
- launch the official app on the phone when Android permits it;
- preserve a consistent hub UX;
- degrade safely when Android Auto cannot present that app on the head unit.

Do not clone, repackage or inject into the official driver apps. Do not assume a phone app can render on Android Auto unless it independently supports an allowed Android Auto surface.

### Future public build

A later public version must use a platform-supported category and distribution path appropriate to its actual functionality. Personal experimentation and public compliance remain separate so a future migration does not require rewriting the provider/core architecture.

## Provider integration tiers

Every media provider must declare one integration tier.

### Tier A — Native playback

The CPZ app owns the provider session and can hand an authorized media URI/license configuration to the trusted playback core.

Examples:
- M3U/M3U8 and compatible IPTV sources
- Xtream/Stalker-style sources where retained and hardened
- user-owned/local media
- personal media servers where the protocol/API is legitimately available

### Tier B — Official-app handoff

The CPZ app acts as a launcher/hub while authentication, DRM and playback stay inside the provider's official Android application.

Initial mandatory targets after V1:
- Netflix
- Disney+

Then evaluate:
- Prime Video
- Max
- YouTube
- Apple TV+
- Paramount+
- Crunchyroll

### Tier C — Authorized partner/native commercial integration

Reserved for providers that expose a documented SDK/API or approve a device/application integration that allows catalog, entitlement and/or playback inside CPZ Media Hub.

## Proposed code boundaries

- `ProviderRegistry`: known providers, capabilities and integration tier
- `ProviderCapability`: browse, search, resume, nativePlayback, externalLaunch, deepLink, offline, drm
- `ProviderAdapter`: provider-specific authentication/catalog behavior for Tier A/C
- `ExternalProviderLauncher`: strict package/link allowlists for Tier B and utility handoffs
- `PlaybackCore`: Media3/ExoPlayer only for authorized native sources
- `PlaybackStateStore`: persist provider/content/position for resume after the parked activity is exited
- `CarPresentation`: parked presentation layer; no provider credentials or DRM logic

## Security ratchets

- Do not reintroduce remote executable plugins to add services.
- Do not embed credentials or service tokens.
- Do not use undocumented private Netflix/Disney APIs as production dependencies.
- Do not disable TLS verification to accommodate a provider.
- Do not proxy/decrypt DRM streams to make them playable by the generic player.
- Keep `tools/security_gate.py` green and extend it when new provider surfaces introduce new trust boundaries.

## Delivery order

1. Finish hardening and get CI/security gate green.
2. Rebuild and audit the hardened APK.
3. Configure a safe test IPTV source and validate playback on phone.
4. Validate CPZ Media Hub / IPTV on Android Auto while parked.
5. Validate persisted playback state and resume after leaving/re-entering the parked app.
6. Freeze/sign the first personal V1 APK.
7. Build the provider-tile Media Hub abstraction.
8. Add Netflix and Disney+ official-app handoffs where supported.
9. Add additional commercial provider tiles.
10. Evaluate inDrive/DiDi utility shortcuts separately from the media provider layer.
11. Only after the personal product is stable, design the public/compliant profile.
