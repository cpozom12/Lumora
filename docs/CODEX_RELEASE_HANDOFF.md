# Codex Release Handoff — CPZ Lumora

## Mission
Produce the first CPZ-owned release candidate from the reviewed hardened branch without reintroducing any upstream executable trust paths.

## Starting point

- Repository: `cpozom12/Lumora`
- Working branch: `agent/security-hardening`
- Draft PR: `#2 — Security hardening baseline`
- CPZ application id: `com.cpozom.lumora`
- Kotlin namespace intentionally remains `com.lumora` for now to keep the hardening migration small/reviewable.
- Upstream updater/install path is disabled.
- Remote executable JavaScript plugins are disabled; QuickJS/Rhino are absent from trusted dependencies.
- Torrent/libtorrent/NanoHTTPD runtime is removed/disabled.
- Credential-bearing LAN QR pairing is disabled.
- IPTV/Jellyfin/Plex secrets persist through AndroidKeyStore-backed AES-GCM.
- Plaintext manual backup/import is disabled.
- Security ratchet: `tools/security_gate.py`.
- Canonical security status: `docs/SECURITY_AUDIT_CPZ.md`.

## Non-negotiable rules

1. Do not blindly merge/sync upstream executable code.
2. Do not re-enable `REQUEST_INSTALL_PACKAGES`, in-app APK download/install, remote executable plugins, Rhino/QuickJS, torrent/P2P, cleartext credential-pairing servers, trust-all TLS, or plaintext credential backups.
3. Never commit a keystore, signing password, `keystore.properties`, provider credential, API token, or other secret.
4. Android Auto video remains parked-only. Do not implement or test bypasses for host motion restrictions.
5. PR #2 remains draft until build/security/signing/device evidence is recorded.

## PC validation sequence

From the exact reviewed head of `agent/security-hardening`:

```bash
python tools/security_gate.py
./gradlew clean :app:test :app:lintDebug :app:assembleDebug --no-daemon
python tools/security_gate.py --apk app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` if needed.

Do not continue to release signing if any command fails.

## Cleartext-provider decision

The manifest still permits cleartext traffic for compatibility with user-supplied IPTV/M3U/Stalker `http://` endpoints.

Before production release, implement/record one explicit policy:

A. HTTPS-only trusted release; or

B. HTTP allowed only through an explicit user opt-in/per-provider warning that clearly states credentials/content can be intercepted on the network.

Do not silently treat HTTP credentials as secure.

## Product identity before signing

Before the first public signed release:

1. Freeze user-visible product/launcher name.
2. Freeze branding/icons.
3. Confirm `applicationId = "com.cpozom.lumora"` is the intended permanent Android package id.
4. Decide whether Kotlin namespace/package renaming is worth doing now or should remain a later refactor. Do not couple a cosmetic namespace refactor to the security release unless necessary.
5. Preserve the upstream MIT copyright/license notice in derivative distributions.

## CPZ signing key

Generate a new Android signing key locally, outside Git. Example using JDK `keytool`:

```bash
keytool -genkeypair -v \
  -keystore cpz-lumora-release.jks \
  -alias cpz-lumora \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Store the keystore and recovery information in a secure location. Losing the release key means losing the ability to publish updates under the same Android application id.

Create local `keystore.properties` only on the build machine:

```properties
storeFile=/absolute/path/to/cpz-lumora-release.jks
storePassword=...
keyAlias=cpz-lumora
keyPassword=...
```

Confirm `.gitignore` excludes it before any commit/push.

## Release build

```bash
./gradlew clean :app:test :app:assembleRelease --no-daemon
```

Then locate the release APK and verify its signature with the Android SDK build tools:

```bash
apksigner verify --verbose --print-certs <release.apk>
```

Record:
- exact source commit SHA;
- APK SHA-256;
- signer certificate SHA-256;
- build date/toolchain versions.

Also run the security gate against the release APK:

```bash
python tools/security_gate.py --apk <release.apk>
```

## Release APK audit

Before device installation confirm:

- no `REQUEST_INSTALL_PACKAGES`;
- no upstream release/self-update endpoint;
- no Rhino/QuickJS runtime;
- no libtorrent/NanoHTTPD/Java-WebSocket runtime;
- no unexpected native libraries;
- no credential-bearing LAN listener;
- no plaintext provider/media-server persistence;
- no automatic credential backup path;
- CPZ application id is present;
- expected signing certificate is present.

## Samsung Galaxy A35 smoke test

Only after static/release gates pass:

1. Install the CPZ-signed candidate.
2. Confirm it installs as the CPZ application identity and does not overwrite an unrelated upstream package.
3. Add only legal/test IPTV/Jellyfin/Plex sources.
4. Test provider add/edit/remove.
5. Test restart/persistence and confirm credentials still work after encryption migration.
6. Test live playback, VOD, series, pause/resume, audio/subtitles, favorites and reconnect.
7. Test offline/download features that remain intentionally supported.
8. Confirm no unexpected install-app prompts, plugin execution, P2P behavior or LAN credential server appears.
9. Capture failures/logs without exposing provider passwords/tokens.

## Toyota Agya / Android Auto validation

Vehicle must remain stationary.

Validate:
- app visibility in Android Auto launcher;
- session startup/disclaimer;
- navigation with large targets;
- catalog loading;
- stationary video playback;
- audio focus;
- reconnect after cable/wireless interruption;
- phone lock/unlock behavior;
- return from Waze/other Android Auto apps;
- no attempt to override or bypass Android Auto motion restrictions.

Record results in `docs/TEST_MATRIX_CPZ.md`.

## Final evidence update

Update `docs/SECURITY_AUDIT_CPZ.md` with:

- release candidate commit SHA;
- CI result;
- APK SHA-256;
- signer certificate SHA-256;
- final cleartext-provider policy;
- A35 result;
- Toyota Agya Android Auto stationary result;
- any accepted residual risks.

Only then should PR #2 move from draft toward final review/merge.

## Definition of done

The release candidate is complete only when:

- `validate-wrapper`, `build`, `test`, `lint`, and `security-gate` are green;
- release signing is CPZ-controlled;
- the release APK passes the static security gate;
- artifact hash and signer fingerprint are recorded;
- A35 smoke test passes;
- Toyota Agya stationary Android Auto test passes;
- cleartext-provider behavior is explicit;
- no secret material enters Git;
- final human approval is given before merge/release.
