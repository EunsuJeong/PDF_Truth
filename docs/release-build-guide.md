# Release Build Guide

## 1) Prepare Keystore
1. Place your keystore file under `keystore/` (example: `keystore/release-keystore.jks`).
2. Copy `keystore.properties.example` to `keystore.properties`.
3. Fill in all values in `keystore.properties`.

## 2) Build Commands
From project root:

```powershell
.\gradlew clean
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew bundleRelease
```

## 3) Output Paths
- Debug APK: `app/build/outputs/apk/debug/`
- Release APK: `app/build/outputs/apk/release/`
- Release AAB: `app/build/outputs/bundle/release/`

Artifacts are copied with naming pattern:
- APK: `PDF_YYMMDD_NN.apk`
- AAB: `PDF_YYMMDD_NN.aab`

## 4) Signature Verification (Optional)

```powershell
$androidBuildTools = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"
$latest = Get-ChildItem $androidBuildTools -Directory | Sort-Object Name -Descending | Select-Object -First 1
$apksigner = Join-Path $latest.FullName "apksigner.bat"
& $apksigner verify --print-certs "app/build/outputs/apk/release/PDF_YYMMDD_NN.apk"
```

## 5) Safety Checklist
- `keystore.properties`, `*.jks`, `*.keystore`, `keystore/` are ignored by git.
- `local.properties` is ignored by git.
- Only final release artifacts are uploaded to Google Play Console.
