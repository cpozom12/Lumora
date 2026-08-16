# CPZ Lumora Security Audit

Status: **HARDENING IN PROGRESS — do not install the upstream-equivalent debug APK**

Audit target:
- Fork: `cpozom12/Lumora`
- Upstream executable baseline: `c0aaf9bd798052ad51e33d703ecffd3ef022eef6`
- First audited build commit: `3bf17c2bd6557ed91376dfcc655551f180638fb7`
- First APK SHA-256: `85a3025bc8bf3fc8961055382def17b02060ef80c17dad7670717dfb784729ac`

## Provenance verified

The first APK was re-downloaded from GitHub Actions artifact `9253496187`; its artifact ZIP digest matched GitHub's published SHA-256 and the extracted APK was byte-identical to the file prepared for device installation.

APK Signature Scheme v2 was parsed and cryptographically verified:
- signer: `CN=Android Debug, O=Android, C=US`
- certificate SHA-256: `4d5f249fcbf3377d451f1aa17e76d97a6fc7b1141c2e83d60c6b5e126dcc4d76`
- signature algorithm: RSA PKCS#1 v1.5 + SHA-256 (`0x0103`)
- APK content digest matched the signed digest.

The first build differs from the upstream baseline only by CPZ documentation, therefore all executable findings below apply to the upstream code at that baseline.

## No classic spyware indicators found

Static inspection has not found app code requesting SMS, contacts, microphone, camera, Accessibility Service, device-admin, notification-listener or usage-stats privileges. No source references to `Runtime.exec`, `ProcessBuilder` or `DexClassLoader` were found. No Google Mobile Ads, Facebook Ads, Firebase Analytics/Crashlytics, Sentry, AppsFlyer, Adjust, OneSignal or Mixpanel SDK package was identified in the APK.

This is evidence against conventional spyware behavior, **not a proof that the original APK is safe**.

## Security blockers discovered

### S-01 — Upstream self-update supply-chain path — BLOCKER

The upstream app can check `disclosurez/Lumora` releases, download an APK, request the Android install-packages capability and launch the installer. A future upstream account/release compromise would bypass CPZ code review and signing controls.

Hardening status: **MITIGATED on `agent/security-hardening`**.
- `REQUEST_INSTALL_PACKAGES` removed.
- APK installer query removed.
- update checker made fail-closed/no-network.

### S-02 — Network-supplied JavaScript evaluated in Rhino — BLOCKER

`MStreamDayExtractor` downloaded obfuscated JavaScript and could fall back to `AADecoder.decodeWithRhino()`. The evaluator initialized a standard Rhino scope and evaluated the remote script in-process. That is an unacceptable code-execution boundary for an untrusted streaming host.

Hardening status: **REMOVED**.
- MStreamDay extractor now fails closed.
- `AADecoder.java` deleted.
- Rhino dependency deleted.

### S-03 — TLS certificate/hostname verification bypasses — BLOCKER

The inherited network stack contained trust-all X509 trust managers and hostname verifiers. DoH itself also used disabled certificate/hostname verification. Several inherited providers expose `buildUnsafe()` fallbacks.

Hardening status: **CORE PATH MITIGATED / LEGACY SCRAPERS QUARANTINED**.
- DoH now uses normal platform TLS and HTTPS-only endpoints.
- shared `NetworkClient.trustAll` is now a safe compatibility alias to the validating client.
- header-level network logging removed from the shared client.
- all inherited third-party scraper providers are runtime-disabled pending individual review.

Remaining task: delete or rewrite legacy provider-specific `buildUnsafe()` implementations before any scraper is re-enabled.

### S-04 — Remote JavaScript plugin trust — HIGH

The upstream app automatically exposed a plugin catalogue hosted by the upstream project and could install JavaScript fetched from the network. QuickJS host APIs permit outbound GET/POST requests. The sandbox does not visibly expose filesystem/shell/Android Context, which is positive, but unsigned remote executable code is not acceptable as an implicit default.

Hardening status: **MITIGATED BY DEFAULT**.
- no plugin store is configured on a clean install;
- stores are explicit opt-in and HTTPS-only;
- newly installed scripts remain disabled until separately enabled.

Future task: add signed/hash-pinned plugin manifests if plugins remain in the product.

### S-05 — Excessive scraper attack surface — HIGH

The upstream product includes dozens of independently changing third-party streaming/scraper providers, WebView challenge handling, extractor code, cookies and site-specific parsing. This is unrelated to the trusted IPTV/Jellyfin/Plex/Android Auto core CPZ wants to build.

Hardening status: **QUARANTINED**.
- `Provider.providers` is empty in the hardened branch.

Future task: physically remove the scraper package and its now-unneeded dependencies after compatibility cleanup.

### S-06 — Plaintext provider/media credentials and backup exposure — HIGH

IPTV usernames/passwords and Jellyfin/Plex credentials/tokens are stored in app-private preferences. Upstream Android Auto Backup was enabled. Manual JSON backups also include provider credential fields.

Hardening status: **PARTIAL**.
- Android application backup is disabled in the hardened manifest.

Remaining tasks:
- migrate secrets to Android Keystore-backed AES-GCM storage;
- remove secrets from normal manual backups or add password-protected authenticated encryption.

### S-07 — Cleartext LAN QR pairing — MEDIUM/HIGH

QR pairing starts a local HTTP server and can receive provider credentials over cleartext LAN traffic. It has a random 128-bit token and a five-minute lifetime, which are good controls, but those controls do not provide transport confidentiality.

Hardening status: **OPEN**.

Do not use credential-bearing QR pairing on untrusted Wi-Fi until the feature is redesigned or disabled.

### S-08 — Permissive WebView scraper configuration — MEDIUM/HIGH

The inherited Cloudflare resolver enables JavaScript, third-party cookies and mixed content. One extractor exposes a narrow JavaScript interface. These are substantial web attack surfaces even though no generic Android bridge was found.

Hardening status: **RUNTIME QUARANTINED with the scraper providers**.

### S-09 — Native torrent engine / unnecessary native attack surface — MEDIUM

The APK includes QuickJS JNI plus a large `libtorrent4j` native library and P2P engine. Torrent playback is not required for the intended trusted media-client core.

Hardening status: **OPEN**.

Future task: remove torrent/NanoHTTPD/libtorrent unless there is a deliberate product requirement.

### S-10 — CI/release supply-chain hardening — MEDIUM

Actions currently use floating major-version action tags and the release workflow uses third-party signing/release actions. That is common practice but not sufficient for a high-trust release pipeline.

Hardening status: **OPEN**.

Future task: pin Actions to immutable commit SHAs, enable dependency/security scanning, and create a CPZ-controlled release signing key.

## Native APK inventory

The audited upstream-equivalent APK contains 19 DEX files and these native libraries:
- `libquickjs-android-wrapper.so`
- `libtorrent4j.so`

No additional hidden native payload was found in the APK archive. The native components correspond to dependencies declared by the Gradle build, but native-code provenance and CVE review remain part of the release gate.

## Trust gate before installing on a primary phone

A CPZ APK is not approved for installation until all of the following are true:
1. build comes from `agent/security-hardening` or a reviewed descendant;
2. `build`, unit tests and regression-aware lint pass;
3. exact APK hash and signer fingerprint are recorded;
4. no self-update/install-package capability exists;
5. Rhino/network-code execution path is absent;
6. trust-all TLS paths are not reachable;
7. scraper providers are disabled or individually reviewed;
8. fresh-install plugin list is empty and scripts are disabled by default;
9. merged APK manifest is re-audited;
10. APK/Dex/native static scan is repeated on the new artifact.

Only after those gates pass should the APK be installed on the primary Android device.
