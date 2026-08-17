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
EXPECTED_MEDIA_PACKAGES = (
    'pe.movistar.go',
    'com.netflix.mediaclient',
    'com.disney.disneyplus',
    'com.google.android.youtube',
    'com.amazon.avod.thirdpartyclient',
    'com.wbd.stream',
)

FORBIDDEN_SCRAPER_MARKERS = (
    b'aniworld.to', b'anime-world.in', b'dood.pm', b'dood.re', b'doodstream.com',
    b'mixdrop.top', b'mixdrop.ag', b'megacloud.blog', b'serienstream.to', b'cineby.app',
    b'streamingcommunityz.land', b'vixsrc.to', b'supervideo.tv', b'filemoon.sx',
    b'videasy.net', b'vidsrc.cc', b'vidsrc-embed.ru', b'vidsrc-embed.su', b'vidfast.pro',
    b'2embed.cc', b'vidlink.pro', b'videostr.net', b'4khdhub.fans', b'101kittens.com',
    b'hydrax.net', b'yourupload.com', b'waaw.to', b'wishonly.site', b'savefiles.com',
    b'luluvid.com', b'moviesapi.club', b'streamtape.com', b'vidmoly.me',
    b'disclosurez/lumora-plugins',
)

FORBIDDEN_HUB_PERMISSIONS = (
    'android.permission.INTERNET',
    'android.permission.ACCESS_NETWORK_STATE',
    'android.permission.REQUEST_INSTALL_PACKAGES',
    'android.permission.QUERY_ALL_PACKAGES',
    'android.permission.ACCESS_FINE_LOCATION',
    'android.permission.ACCESS_COARSE_LOCATION',
    'android.permission.CAMERA',
    'android.permission.RECORD_AUDIO',
    'android.permission.READ_CONTACTS',
    'android.permission.WRITE_CONTACTS',
    'android.permission.READ_SMS',
    'android.permission.SEND_SMS',
    'android.permission.READ_EXTERNAL_STORAGE',
    'android.permission.WRITE_EXTERNAL_STORAGE',
    'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.POST_NOTIFICATIONS',
    'android.permission.FOREGROUND_SERVICE',
    'android.permission.ACCESS_WIFI_STATE',
    'android.permission.CHANGE_WIFI_MULTICAST_STATE',
)

# These markers are stronger than the original hardening facade checks. The personal Hub V1 is a
# local allow-listed launcher, so legacy playback/plugin/torrent entry points must not survive R8
# into the shipped APK at all, even when their source facades remain fail-closed for compilation.
FORBIDDEN_LEGACY_APK_MARKERS = (
    b'Lcom/lumora/MainActivity;',
    b'resolveTorrentStream',
    b'wirePluginsPane',
    b'Lcom/lumora/torrent/TorrentEngine;',
    b'Lcom/lumora/plugin/js/JsPluginEngine;',
    b'github.com/disclosurez/Lumora',
    b'discord.gg/lumora',
    b'example.com/plugins/index.json',
)


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
    provider_registry = read('app/src/main/java/com/lumora/hub/ExternalMediaProvider.kt')
    provider_launcher = read('app/src/main/java/com/lumora/hub/ExternalProviderLauncher.kt')
    hub_activity = read('app/src/main/java/com/lumora/hub/HubActivity.kt')

    require(f'applicationId = "{EXPECTED_APPLICATION_ID}"' in gradle,
            f'applicationId must remain {EXPECTED_APPLICATION_ID}')

    for banned in ('org.mozilla:rhino', 'libtorrent', 'nanohttpd', 'java-websocket', 'quickjs'):
        require(banned.lower() not in deps.lower(),
                f'forbidden dependency marker present: {banned}')

    for permission in FORBIDDEN_HUB_PERMISSIONS:
        require(permission not in manifest,
                f'forbidden personal-Hub permission declared: {permission}')
    require('android:allowBackup="false"' in manifest,
            'Android backup must remain disabled')
    require('android:usesCleartextTraffic="false"' in manifest,
            'personal Hub must reject cleartext HTTP traffic')
    require('android:name=".hub.HubActivity"' in manifest,
            'HubActivity must remain the exported personal-Hub entry surface')
    require('android.intent.category.CAR_LAUNCHER' in manifest,
            'parked personal-Hub CAR_LAUNCHER entry must remain declared')
    for legacy_component in (
        'android:name=".MainActivity"',
        'android:name=".BaseApplication"',
        'androidx.core.content.FileProvider',
        'androidx.work.WorkManagerInitializer',
        'com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME',
        'androidx.car.app.CarAppService',
        '.torrent.TorrentForegroundService',
        '.reminder.ReminderBootReceiver',
        '.recording.RecordingRestoreReceiver',
    ):
        require(legacy_component not in manifest,
                f'legacy component must not be registered in personal Hub V1: {legacy_component}')

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

    for package_name in EXPECTED_MEDIA_PACKAGES:
        require(package_name in provider_registry,
                f'reviewed media package missing from registry: {package_name}')
        require(f'android:name="{package_name}"' in manifest,
                f'reviewed media package missing from manifest queries: {package_name}')
    require('http://' not in provider_registry and 'https://' not in provider_registry,
            'media provider registry must not become a remote catalogue')
    require('context.startCarApp(' not in provider_launcher,
            'external provider launcher must not force third-party car-app launches')
    require('catch (t: Throwable)' not in provider_launcher,
            'external provider launcher must not swallow arbitrary fatal errors')
    require('ExternalMediaProviderRegistry.providers' in hub_activity,
            'HubActivity must render only the reviewed static provider registry')


def check_apk(apk: Path) -> None:
    require(apk.is_file(), f'APK not found: {apk}')

    forbidden_dex_markers = (
        b'android.permission.INTERNET',
        b'android.permission.ACCESS_NETWORK_STATE',
        b'android.permission.REQUEST_INSTALL_PACKAGES',
        b'android.permission.QUERY_ALL_PACKAGES',
        b'api.github.com/repos/disclosurez/Lumora/releases',
        b'disclosurez/Lumora/releases',
        b'org.mozilla.javascript',
        b'org/mozilla/javascript',
        b'libtorrent4j',
        b'fi/iki/elonen/NanoHTTPD',
        b'org/java_websocket',
        b'DexClassLoader',
        b'/system/bin/su',
        b'Magisk',
    ) + FORBIDDEN_SCRAPER_MARKERS + FORBIDDEN_LEGACY_APK_MARKERS

    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        native = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
        require(not native,
                f'personal Hub APK must contain no native .so payloads: {native}')

        dex_names = [n for n in names if n.endswith('.dex')]
        require(dex_names, 'APK contains no DEX files')
        dex = b''.join(zf.read(name) for name in dex_names)
        lowered_dex = dex.lower()
        for marker in forbidden_dex_markers:
            require(marker.lower() not in lowered_dex,
                    f'forbidden runtime marker shipped in APK: {marker.decode("ascii", errors="ignore")}')

        for package_name in EXPECTED_MEDIA_PACKAGES:
            require(package_name.encode('ascii') in dex,
                    f'reviewed media provider missing from built APK: {package_name}')

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
