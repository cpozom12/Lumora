#!/usr/bin/env python3
"""Fail CI if CPZ security invariants regress.

Dependency-free by design so it can run in GitHub Actions and locally. It checks
source/build configuration first and, when --apk is supplied, scans the built
artifact for runtime payloads that must never ship in the trusted build.
"""

from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_APPLICATION_ID = 'com.cpozom.lumora'


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding='utf-8')


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f'SECURITY GATE FAILED: {message}')


def gradle_dependency_lines(gradle: str) -> str:
    prefixes = ('implementation(', 'api(', 'compileOnly(', 'runtimeOnly(', 'ksp(')
    return '\n'.join(
        line.strip() for line in gradle.splitlines()
        if line.strip().startswith(prefixes)
    )


def check_source() -> None:
    gradle = read('app/build.gradle.kts')
    deps = gradle_dependency_lines(gradle)
    manifest = read('app/src/main/AndroidManifest.xml')
    updater = read('app/src/main/java/com/lumora/data/update/AppUpdateChecker.kt')
    installer = read('app/src/main/java/com/lumora/data/update/AppUpdateInstaller.kt')
    plugin_engine = read('app/src/main/java/com/lumora/plugin/js/JsPluginEngine.kt')
    plugin_store = read('app/src/main/java/com/lumora/plugin/js/PluginStoreManager.kt')
    torrent = read('app/src/main/java/com/lumora/torrent/TorrentEngine.kt')
    pairing = read('app/src/main/java/com/lumora/pairing/QrPairingManager.kt')
    backup = read('app/src/main/java/com/lumora/data/backup/BackupManager.kt')
    iptv_store = read('app/src/main/java/com/lumora/data/IptvProviderStore.kt')
    media_store = read('app/src/main/java/com/lumora/data/MediaServerStore.kt')
    network = read('app/src/main/java/com/lumora/scraper/utils/NetworkClient.kt')

    require(f'applicationId = "{EXPECTED_APPLICATION_ID}"' in gradle,
            f'applicationId must remain {EXPECTED_APPLICATION_ID}')

    for banned in (
        'org.mozilla:rhino',
        'libtorrent',
        'nanohttpd',
        'java-websocket',
        'quickjs',
    ):
        require(banned.lower() not in deps.lower(),
                f'forbidden dependency marker present: {banned}')

    require('android.permission.REQUEST_INSTALL_PACKAGES' not in manifest,
            'REQUEST_INSTALL_PACKAGES must not be declared')
    require('android:allowBackup="false"' in manifest,
            'Android backup must remain disabled')
    require('.torrent.TorrentForegroundService' not in manifest,
            'torrent foreground service must not be registered')

    require('suspend fun checkForUpdate(): UpdateInfo? = null' in updater,
            'in-app updater must remain fail-closed')
    require('fun installApk' in installer and '= false' in installer,
            'APK installer facade must remain disabled')

    require('Executable plugins are disabled in the CPZ hardened build' in plugin_engine,
            'executable plugin engine must remain disabled')
    require('fun storeUrls(): List<PluginStore> = emptyList()' in plugin_store,
            'remote plugin stores must remain disabled')

    require('Torrent/P2P playback is disabled in the CPZ hardened build' in torrent,
            'torrent/P2P engine must remain disabled')
    require('LAN credential pairing is disabled in the hardened build' in pairing,
            'credential-bearing LAN pairing must remain disabled')
    require('suspend fun exportTo' in backup and ': Boolean = false' in backup,
            'plaintext manual backup/export must remain disabled')

    require('SecureValueStore' in iptv_store,
            'IPTV provider secrets must use SecureValueStore')
    require('SecureValueStore' in media_store,
            'media-server secrets must use SecureValueStore')

    require('hostnameVerifier' not in network,
            'custom hostnameVerifier must not be reintroduced')
    require('trustAll: OkHttpClient get() = default' in network,
            'legacy trustAll alias must remain validating')


def check_apk(apk: Path) -> None:
    require(apk.is_file(), f'APK not found: {apk}')

    forbidden_native = ('torrent', 'quickjs')
    forbidden_dex_markers = (
        b'android.permission.REQUEST_INSTALL_PACKAGES',
        b'api.github.com/repos/disclosurez/Lumora/releases',
        b'disclosurez/Lumora/releases',
        b'org.mozilla.javascript',
        b'org/mozilla/javascript',
        b'libtorrent4j',
        b'fi/iki/elonen/NanoHTTPD',
        b'org/java_websocket',
    )

    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        native = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
        for name in native:
            lowered = name.lower()
            for marker in forbidden_native:
                require(marker not in lowered,
                        f'forbidden native payload shipped in APK: {name}')

        dex_names = [n for n in names if n.endswith('.dex')]
        require(dex_names, 'APK contains no DEX files')
        dex = b''.join(zf.read(name) for name in dex_names)
        for marker in forbidden_dex_markers:
            require(marker not in dex,
                    f'forbidden runtime marker shipped in APK: {marker.decode("ascii", errors="ignore")}')

    print(f'APK security scan passed: {apk}')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, help='Optional built APK to inspect')
    args = parser.parse_args()

    check_source()
    print('Source security invariants passed')
    if args.apk:
        check_apk(args.apk)
    return 0


if __name__ == '__main__':
    sys.exit(main())
