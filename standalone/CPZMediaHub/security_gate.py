#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
APP = ROOT / "app"
EXPECTED_PACKAGES = (
    "pe.movistar.go",
    "com.netflix.mediaclient",
    "com.disney.disneyplus",
    "com.google.android.youtube",
    "com.amazon.avod.thirdpartyclient",
    "com.wbd.stream",
)
FORBIDDEN_TEXT = (
    "lumora", "disclosurez", "torrent", "quickjs", "scraper", "webview",
    "okhttp", "retrofit", "workmanager", "media3", "javascriptinterface",
    "dexclassloader", "runtime.exec", "processbuilder", "http://", "https://",
)
FORBIDDEN_APK = tuple(x.encode("utf-8") for x in FORBIDDEN_TEXT) + (
    b"android.permission.", b"Landroid/webkit/WebView;", b"Ljava/net/Socket;",
    b"Ljava/net/URL;", b"Ljava/lang/Runtime;", b"Ljava/lang/ProcessBuilder;",
    b"Ldalvik/system/DexClassLoader;", b"Landroid/content/ContentResolver;",
    b"Landroid/app/admin/DevicePolicyManager;", b"Landroid/accessibilityservice/AccessibilityService;",
)


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(f"CPZ PERMANENT GATE FAILED: {message}")


def source_gate() -> None:
    gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
    manifest = (APP / "src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    java = "\n".join(p.read_text(encoding="utf-8") for p in (APP / "src/main/java").rglob("*.java"))
    all_project_text = "\n".join(
        p.read_text(encoding="utf-8", errors="ignore")
        for p in ROOT.rglob("*")
        if p.is_file() and p.suffix.lower() in {".java", ".xml", ".kts", ".md", ".txt", ""}
        and p.name not in {"security_gate.py"}
    ).lower()

    require('applicationId = "com.cpozom.mediahub"' in gradle, "applicationId changed")
    require('versionName = "1.0.0"' in gradle, "versionName must be 1.0.0")
    require("dependencies {\n    // Intentionally empty" in gradle, "runtime dependency block is not intentionally empty")
    require("<uses-permission" not in manifest, "manifest declares a permission")
    require('android:allowBackup="false"' in manifest, "backup must be disabled")
    require('android:usesCleartextTraffic="false"' in manifest, "cleartext must be disabled")
    require('android:appCategory="game"' not in manifest, "must not masquerade as a game")
    require('android:appCategory="video"' not in manifest, "must not masquerade as a video player")
    require('android.intent.category.CAR_LAUNCHER' in manifest, "parked CAR_LAUNCHER declaration missing")
    require("getLaunchIntentForPackage" in java and "startActivity" in java, "expected local handoff logic missing")
    for package_name in EXPECTED_PACKAGES:
        require(package_name in manifest and package_name in java, f"allow-listed package missing: {package_name}")
    for marker in FORBIDDEN_TEXT:
        require(marker not in all_project_text, f"forbidden standalone source marker present: {marker}")


def apk_gate(apk: Path) -> None:
    require(apk.is_file(), f"APK not found: {apk}")
    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        require(len(names) == len(set(names)), "duplicate ZIP entries")
        require(not any(".." in Path(n).parts for n in names), "path traversal ZIP entry")
        require(not any(n.startswith("lib/") and n.endswith(".so") for n in names), "native library present")
        require(not any(n.endswith((".dex.jar", ".apk", ".jar", ".js")) for n in names), "nested executable payload present")
        dex_names = [n for n in names if n.endswith(".dex")]
        require(dex_names == ["classes.dex"], f"expected one DEX, got {dex_names}")
        payload = b"".join(zf.read(n) for n in names if n.endswith(".dex") or n == "AndroidManifest.xml")
        lowered = payload.lower()
        for marker in FORBIDDEN_APK:
            require(marker.lower() not in lowered, f"forbidden APK marker: {marker.decode(errors='ignore')}")
        for package_name in EXPECTED_PACKAGES:
            require(package_name.encode() in payload, f"package target missing from APK: {package_name}")
        bad_crc = zf.testzip()
        require(bad_crc is None, f"CRC failure: {bad_crc}")
    print(f"CPZ permanent APK gate passed: {apk}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path)
    args = parser.parse_args()
    source_gate()
    print("CPZ permanent source gate passed")
    if args.apk:
        apk_gate(args.apk)
    return 0


if __name__ == "__main__":
    sys.exit(main())
