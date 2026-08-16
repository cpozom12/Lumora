# CPZ Lumora Security Audit

Status: **HARDENING IN PROGRESS — install only after the current branch, CI and rebuilt APK pass the trust gate below**

Audit target:
- Fork: `cpozom12/Lumora`
- Upstream executable baseline: `c0aaf9bd798052ad51e33d703ecffd3ef022eef6`
- First audited build commit: `3bf17c2bd6557ed91376dfcc655551f180638fb7`
- First APK SHA-256: `85a3025bc8bf3fc8961055382def17b02060ef80c17dad7670717dfb784729ac`
- Hardened install identity: `com.cpozom.lumora` (`com.cpozom.lumora.debug` for debug builds)

## Provenance verified

The first APK was re-downloaded from GitHub Actions artifact `9253496187`; its artifact ZIP digest matched GitHub's published SHA-256 and the extracted APK was byte-identical to the file prepared for device installation.

APK Signature Scheme v2 was parsed and cryptographically verified:
- signer: `CN=Android Debug, O=Android, C=US`
- certificate SHA-256: `4d5f249fcbf3377d451f1aa17e76d97a6fc7b1141c2e83d60c6b5e126dcc4d76`
- signature algorithm: RSA PKCS#1 v1.5 + SHA-256 (`0x0103`)
- APK content digest matched the signed digest.

The first build differed from the upstream baseline only by CPZ documentation, therefore the original findings below apply to the upstream code at that baseline. The hardened branch intentionally removes or disables the risky paths before CPZ installation.

## No classic spyware indicators found

Static inspection has not found app code requesting SMS, contacts, microphone, camera, Accessibility Service, device-admin, notification-listener or usage-stats privileges. No source references to `Runtime.exec`, `ProcessBuilder` or `DexClassLoader` were found. No Google Mobile Ads, Facebook Ads, Firebase Analytics/Crashlytics, Sentry, AppsFlyer, Adjust, OneSignal or Mixpanel SDK package was identified in the audited upstream-equivalent APK.

This is evidence against conventional spyware behavior, **not proof that arbitrary upstream releases are safe**.

## Security findings and hardening status

### S-01 — Upstream self-update supply-chain path — MITIGATED

The upstream app could check `disclosurez/Lumora` releases, download an APK, request Android's install-packages capability and launch the installer. A future upstream account/release compromise could therefore bypass CPZ review and signing controls.

Hardened state:
- `REQUEST_INSTALL_PACKAGES` removed;
- update checker performs no network I/O and returns no update;
- APK installer facade fails closed;
- inherited release workflow removed pending a CPZ-owned signing pipeline.

### S-02 — Network-supplied JavaScript evaluated in Rhino — REMOVED

The inherited MStreamDay path could evaluate network-supplied obfuscated JavaScript through Rhino.

Hardened state:
- MStreamDay path fails closed;
- `AADecoder.java` removed;
- Rhino dependency removed.

### S-03 — TLS certificate/hostname verification bypasses — CORE MITIGATED

The inherited stack contained trust-all certificate and hostname behavior, including DoH paths.

Hardened state:
- DoH uses normal platform TLS and HTTPS-only resolvers;
- shared `NetworkClient.trustAll` is a compatibility alias to the validating client;
- shared request/header logging that could expose credentials was removed;
- inherited third-party scraper providers are runtime-disabled.

Remaining cleanup: physically delete or individually rewrite legacy provider-specific unsafe compatibility code before any scraper provider is ever re-enabled.

### S-04 — Remote JavaScript plugin trust — DISABLED IN TRUSTED BUILD

The upstream app could discover, download and execute JavaScript plugins from network stores.

Hardened state:
- QuickJS dependency/native payload removed;
- plugin execution API is a fail-closed compatibility facade;
- plugin store list is empty and cannot fetch/install scripts;
- no remote executable plugin path is part of the trusted build.

Future reintroduction, if ever desired, requires a separate signed/hash-pinned plugin trust design.

### S-05 — Excessive scraper attack surface — QUARANTINED

The inherited product includes many independently changing scraper/provider implementations, WebView challenge handling, cookies and site-specific parsing.

Hardened state:
- inherited third-party providers are not exposed by the trusted runtime;
- R8/minification is enabled for security-audit debug builds so unreachable legacy code is stripped from the produced APK.

Remaining cleanup: physically remove scraper source/dependencies once compatibility call sites are untangled.

### S-06 — Plaintext provider/media credentials and backup exposure — MITIGATED

Upstream persisted provider/media-server connection details and tokens in plaintext preferences and exported credential-bearing manual backups.

Hardened state:
- IPTV URLs/usernames/passwords/user-agent values are stored as AndroidKeyStore-backed AES-GCM envelopes;
- Jellyfin/Plex connection details and tokens use the same authenticated encryption path;
- legacy plaintext values are accepted only for one-time migration and immediately rewritten encrypted;
- Android application backup is disabled;
- plaintext manual backup/import is disabled pending a future authenticated, passphrase-protected design.

### S-07 — Cleartext LAN QR credential pairing — MITIGATED

The inherited quick-pair server accepted provider credentials over cleartext LAN HTTP.

Hardened state:
- credential-bearing LAN pairing server is disabled and opens no listener;
- generic QR generation remains available only for non-secret strings/URLs.

### S-08 — Permissive WebView scraper configuration — RUNTIME QUARANTINED

Inherited scraper flows contain permissive WebView/challenge behavior. Those providers remain disabled in the trusted runtime and are not considered an approved product surface.

### S-09 — Native torrent/P2P stack — MITIGATED

The inherited APK bundled libtorrent/native P2P and a local HTTP streaming server.

Hardened state:
- libtorrent4j/native P2P dependencies removed;
- NanoHTTPD removed;
- torrent foreground service removed from the manifest;
- TorrentEngine is a fail-closed compatibility stub.

Remaining cleanup: delete compatibility source once UI/call-site cleanup is complete.

### S-10 — CI/release supply chain — PARTIALLY MITIGATED

Hardened state:
- GitHub Actions used by CI are pinned to immutable commit SHAs;
- Gradle wrapper validation runs before build/test/lint;
- CI permissions are read-only;
- debug APK is built in CI;
- `tools/security_gate.py` enforces source invariants and scans the built APK for forbidden native/runtime payloads;
- Dependabot monitors Gradle and GitHub Actions dependencies;
- inherited automatic release/signing workflow is removed.

Remaining release work:
- generate a CPZ-owned release signing key outside Git;
- configure a minimal CPZ-controlled release workflow after the application id/product identity is frozen;
- record the final release signer fingerprint and exact APK SHA-256.

### S-11 — Global cleartext compatibility — OPEN / PRODUCT DECISION

The manifest still permits cleartext traffic because IPTV/M3U/Stalker ecosystems can include user-supplied `http://` endpoints. This does not itself transmit data, but it allows a configured provider to use an unencrypted transport and therefore exposes provider credentials/content to local-network interception.

Before a production release we must choose one explicit policy:
1. HTTPS-only trusted build; or
2. insecure HTTP allowed only behind an explicit per-provider/user opt-in with a visible warning.

Do not silently treat cleartext provider credentials as secure.

## Automated regression gate

`tools/security_gate.py` runs in CI and is intended to make security hardening a ratchet rather than a one-time audit. It currently fails if, among other things:
- the CPZ application id changes unexpectedly;
- install-packages capability returns;
- Rhino/QuickJS/libtorrent/NanoHTTPD/Java-WebSocket dependencies return;
- updater/plugin/torrent/LAN-pairing fail-closed facades are re-enabled;
- Android backup is enabled;
- provider/media secrets stop using `SecureValueStore`;
- unsafe hostname-verifier behavior returns to the shared network client;
- forbidden native/runtime markers appear in the produced APK.

## Trust gate before installing on a primary phone

A CPZ APK is approved for installation only when all of the following are true:
1. build comes from `agent/security-hardening` or a reviewed descendant;
2. `validate-wrapper`, `build`, unit tests, regression-aware lint and `security-gate` pass;
3. exact APK SHA-256 and signer fingerprint are recorded;
4. application id is the CPZ-owned id;
5. no self-update/install-package capability exists;
6. Rhino/remote-plugin/P2P native execution paths are absent from the produced APK;
7. trust-all TLS paths are not reachable;
8. scraper providers remain disabled or are individually reviewed;
9. credential-bearing LAN pairing and plaintext backups remain disabled;
10. merged APK manifest and DEX/native inventory are re-audited;
11. cleartext-provider policy is explicitly accepted/configured for the intended release.

Only after those gates pass should the APK be installed on the primary Android device.
