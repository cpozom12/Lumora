param(
    [string]$KeystorePath = (Join-Path $PSScriptRoot "cpz-media-hub-release.jks"),
    [string]$KeyAlias = "cpz-media-hub"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Convert-SecureStringToPlainText([Security.SecureString]$SecureValue) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Find-GradleCommand {
    $localWrapper = Join-Path $PSScriptRoot "gradlew.bat"
    $workshopWrapper = Join-Path $PSScriptRoot "..\..\gradlew.bat"
    if (Test-Path $localWrapper) { return (Resolve-Path $localWrapper).Path }
    if (Test-Path $workshopWrapper) { return (Resolve-Path $workshopWrapper).Path }
    $gradle = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradle) { return $gradle.Source }
    throw "Gradle wrapper/Gradle not found. Open the project once in Android Studio or generate a Gradle wrapper locally."
}

function Find-Keytool {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $keytool = Get-Command keytool -ErrorAction SilentlyContinue
    if ($keytool) { return $keytool.Source }
    throw "keytool not found. Install/use JDK 17 and set JAVA_HOME."
}

$keystoreFullPath = [IO.Path]::GetFullPath($KeystorePath)
$keytoolCommand = Find-Keytool
$gradleCommand = Find-GradleCommand

if (-not (Test-Path $keystoreFullPath)) {
    Write-Host "Creating the permanent CPZ Media Hub signing key locally." -ForegroundColor Cyan
    Write-Host "Choose a strong password and keep the .jks in at least two secure offline backups." -ForegroundColor Yellow
    & $keytoolCommand `
        -genkeypair `
        -v `
        -keystore $keystoreFullPath `
        -alias $KeyAlias `
        -keyalg RSA `
        -keysize 4096 `
        -sigalg SHA256withRSA `
        -validity 36500 `
        -dname "CN=CPZ Media Hub, OU=Personal Apps, O=Christian Anthony Pozo Mejia, L=Lima, C=PE"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }
}

$storeSecure = Read-Host "Keystore password" -AsSecureString
$keySecure = Read-Host "Key password (normally the same as the keystore password)" -AsSecureString
$storePlain = Convert-SecureStringToPlainText $storeSecure
$keyPlain = Convert-SecureStringToPlainText $keySecure

try {
    $env:CPZ_MEDIA_HUB_KEYSTORE = $keystoreFullPath
    $env:CPZ_MEDIA_HUB_STORE_PASSWORD = $storePlain
    $env:CPZ_MEDIA_HUB_KEY_ALIAS = $KeyAlias
    $env:CPZ_MEDIA_HUB_KEY_PASSWORD = $keyPlain

    Write-Host "Building signed, minified, non-debuggable CPZ Media Hub 1.0.0..." -ForegroundColor Cyan
    & $gradleCommand -p $PSScriptRoot :app:clean :app:assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed with exit code $LASTEXITCODE" }

    $apk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $apk)) { throw "Signed APK was not produced at $apk" }

    $hash = (Get-FileHash -Algorithm SHA256 $apk).Hash.ToLowerInvariant()
    Write-Host "Signed release created:" -ForegroundColor Green
    Write-Host $apk
    Write-Host "SHA-256: $hash"
    Write-Host "Keep the keystore private. Future updates MUST use this same key." -ForegroundColor Yellow
}
finally {
    $env:CPZ_MEDIA_HUB_KEYSTORE = $null
    $env:CPZ_MEDIA_HUB_STORE_PASSWORD = $null
    $env:CPZ_MEDIA_HUB_KEY_ALIAS = $null
    $env:CPZ_MEDIA_HUB_KEY_PASSWORD = $null
    $storePlain = $null
    $keyPlain = $null
}
