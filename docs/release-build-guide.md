# Release Build Guide

## 1. Keystore 생성 방법
아래 예시는 루트 폴더 기준 `keystore/upload-keystore.jks` 파일을 생성합니다.

```powershell
New-Item -ItemType Directory -Force keystore
keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

## 2. keystore.properties 작성법
루트 폴더에 `keystore.properties` 파일을 만들고 아래 형식으로 작성합니다.

```properties
storeFile=keystore/upload-keystore.jks
storePassword=실제_스토어_비밀번호
keyAlias=upload
keyPassword=실제_키_비밀번호
```

참고:
- `keystore.properties`는 커밋하지 않습니다.
- keystore 파일도 커밋하지 않습니다.
- 예제는 `keystore.properties.example` 파일을 참고합니다.

## 3. Release Build 방법
Release APK 생성:

```powershell
.\gradlew.bat assembleRelease
```

Release AAB 생성:

```powershell
.\gradlew.bat bundleRelease
```

## 4. Play 업로드 파일 위치
Release APK 위치:
- `app/build/outputs/apk/release/`

Release AAB 위치:
- `app/build/outputs/bundle/release/`

파일명 규칙:
- APK: `PDF_YYMMDD_NUMBER.apk`
- AAB: `PDF_YYMMDD_NUMBER.aab`

## 5. 주의사항
- `local.properties`는 커밋하지 않습니다.
- keystore 비밀번호를 코드에 하드코딩하지 않습니다.
- Google Play 업로드 시 일반적으로 AAB 파일을 사용합니다.
- Release 빌드는 `keystore.properties`와 실제 keystore 파일이 준비되어야 성공합니다.
- 기존 자동 넘버링 규칙을 유지하므로 이전 산출물을 덮어쓰지 않습니다.
