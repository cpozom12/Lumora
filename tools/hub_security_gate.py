#!/usr/bin/env python3
"""Security ratchet for the installable CPZ Media Hub V1 APK.

The hub module is intentionally a zero-permission, zero-network launcher for a fixed allow-list
of official apps. The legacy Lumora app module is not the artifact this gate approves.
"""

from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HUB = ROOT / "hub"
EXPECTED_APPLICATION_ID = "com.cpozom.mediahub"
EXPECTED_PACKAGES = (
    "pe.movistar.go",
    "com.netflix.mediaclient",
    "com.disney.disneyplus",
    "com.google.android.youtube",
    "com.amazon.avod.thirdpartyclient",
    "com.wbd.stream",
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"HUB SECURITY GATE FAILED: {message}")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check_source() -> None:
    gradle = read(HUB / "build.gradle.kts")
    manifest = read(HUB / "src/main/AndroidManifest.xml")
    activity = read(HUB / "src/main/java/com/cpozom/mediahub/HubActivity.kt")
    registry = read(HUB / "src/main/java/com/cpozom/mediahub/ProviderTarget.kt")

    require(f'applicationId = "{EXPECTED_APPLICATION_ID}"' in gradle,
            f"applicationId must remain {EXPECTED_APPLICATION_ID}")
    require("implementation(" not in gradle and "api(" not in gradle and "runtimeOnly(" not in gradle,
            "runtime dependencies are forbidden in the personal Hub module")

    require("<uses-permission" not in manifest,
            "personal Hub source manifest must declare zero permissions")
    require('android:allowBackup="false"' in manifest,
            "Android backup must remain disabled")
    require('android:usesCleartextTraffic="false"' in manifest,
            "cleartext traffic must remain disabled")
    require('android:appCategory="game"' in manifest,
            "parked personal build category changed unexpectedly")
    require("android.intent.category.CAR_LAUNCHER" in manifest,
            "CAR_LAUNCHER entry point must remain present")
    require(".HubActivity" in manifest and ".MainActivity" not in manifest,
            "only the isolated HubActivity may be the app entry point")

    for package_name in EXPECTED_PACKAGES:
        require(f'android:name="{package_name}"' in manifest,
                f"package missing from manifest queries: {package_name}")
        require(package_name in registry,
                f"package missing from reviewed registry: {package_name}")

    forbidden_source_markers = (
        "http://", "https://", "WebView", "OkHttp", "Retrofit", "HttpURLConnection",
        "Socket(", "DatagramSocket", "WebSocket", "DexClassLoader", "Runtime.getRuntime",
        "ProcessBuilder", "QUERY_ALL_PACKAGES", "REQUEST_INSTALL_PACKAGES", "startCarApp(",
        "libtorrent", "TorrentEngine", "JsPlugin", "scraper",
    )
    source = activity + "\n" + registry
    for marker in forbidden_source_markers:
        require(marker not in source, f"forbidden Hub source marker: {marker}")

    require("getLaunchIntentForPackage" in activity and "startActivity(intent)" in activity,
            "Hub must remain a local official-app launcher")

    print("Hub source security invariants passed")


def check_apk(apk: Path) -> None:
    require(apk.is_file(), f"APK not found: {apk}")

    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        native = [name for name in names if name.startswith("lib/") and name.endswith(".so")]
        require(not native, f"native payloads are forbidden in Hub APK: {native}")

        manifest = zf.read("AndroidManifest.xml")
        # Binary Android XML can use either UTF-8 or UTF-16LE string pools. A zero-permission
        # manifest must not contain any android.permission.* name after manifest merging.
        permission_ascii = b"android.permission."
        permission_utf16 = "android.permission.".encode("utf-16le")
        require(permission_ascii not in manifest and permission_utf16 not in manifest,
                "merged APK manifest contains an Android permission")

        dex_names = [name for name in names if name.endswith(".dex")]
        require(dex_names, "APK contains no DEX")
        dex = b"".join(zf.read(name) for name in dex_names)
        lowered = dex.lower()

        forbidden_runtime = (
            b"com/lumora", b"com.lumora", b"androidx/work", b"androidx.media3",
            b"okhttp3", b"retrofit2", b"libtorrent", b"torrentengine", b"jsplugin",
            b"webview", b"dexclassloader", b"http://", b"https://", b"lumora-plugins",
            b"mixdrop", b"doodstream", b"megacloud", b"vidsrc", b"streamtape",
        )
        for marker in forbidden_runtime:
            require(marker not in lowered,
                    f"forbidden legacy/network runtime marker shipped: {marker.decode(errors='ignore')}")

        whole_apk = manifest + dex
        for package_name in EXPECTED_PACKAGES:
            require(package_name.encode("ascii") in whole_apk,
                    f"reviewed provider missing from built APK: {package_name}")

    print(f"Hub APK security scan passed: {apk}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path)
    args = parser.parse_args()
    check_source()
    if args.apk:
        check_apk(args.apk)
    return 0


if __name__ == "__main__":
    sys.exit(main())
