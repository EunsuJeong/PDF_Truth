# APK 보안 검사 기록

## 검사일

2026-06-12

## 검사 대상

- 파일명: PDFTruth_v1.0.0_260612.apk
- SHA-256: 733BB61FB0844FACDCC4708C6DE937BF74BB5ED1994F1F51CB9306723CCC2B55
- 파일 크기: 6.14 MB (6,441,408 bytes)
- 위치: release/PDFTruth_v1.0.0_260612.apk

## 서명 검증

- 도구: apksigner (Android Build Tools 37.0.0)
- 결과: **통과**
- 세부 결과:
  - v1 (JAR signing): false
  - v2 (APK Signature Scheme v2): **true**
  - v3 (APK Signature Scheme v3): **true**
  - Number of signers: 1
  - Signer DN: CN=PDF Truth, OU=Release, O=PDF Truth, L=Seoul, ST=Seoul, C=KR
  - Key algorithm: RSA 2048-bit

## 권한 점검

- apksigner aapt dump permissions 결과:
  - com.pdftruth.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (앱 내부 동적 리시버 보호 권한, 자동 생성)
- 위험 권한(위치/카메라/마이크/연락처/문자/전화/MANAGE_EXTERNAL_STORAGE): **없음**
- 인터넷 권한(INTERNET): **없음**

## 외부 통신 점검

- AndroidManifest 점검: INTERNET 권한 선언 없음
- 소스코드 점검(OkHttp/Retrofit/HttpURLConnection): 발견 없음
- 결론: 외부 서버 통신 로직 미확인

## 악성코드 검사

| 검사 도구 | 검사일 | 결과 | 비고 |
|---|---|---|---|
| (비워두기) | (비워두기) | (비워두기) | 배포 전 검사 후 여기에 기록 필요 |

## 권장 검사 도구 안내

아래 도구를 직접 실행 후 결과를 위 표에 기록하세요.

1. **Android Studio Lint** (로컬): `./gradlew lint` → 앱 코드 품질/보안 경고 확인
2. **VirusTotal** (외부 업로드): https://www.virustotal.com — APK 파일 업로드 후 여러 보안 엔진 스캔. **업로드하면 파일이 VirusTotal 시스템에 공유될 수 있으므로 정책 확인 후 사용.**
3. **MobSF** (로컬 설치): https://github.com/MobSF/Mobile-Security-Framework-MobSF — 로컬 실행 가능한 Android 앱 보안 분석 도구.

## 주의사항

외부 검사 서비스에 APK를 업로드하는 경우 파일이 보안 업체 또는 제3자에게 공유될 수 있으므로, 각 서비스의 업로드 정책을 확인한 뒤 사용합니다.

## 최종 판단

- 배포 가능 여부: 악성코드 검사 완료 후 최종 판단. 서명/권한/통신 점검 기준 현재 이상 없음.
- 남은 보안 우려: 악성코드 검사 도구 실행 결과 추가 기록 필요.
