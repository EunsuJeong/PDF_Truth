# Third Party Notices

이 문서는 PDF Truth APK 직접 배포를 위한 오픈소스 사용 고지입니다.
실제 포함 라이브러리는 `./gradlew app:dependencies --configuration releaseRuntimeClasspath` 결과를 기준으로 작성했습니다.

## 배포 대상 기준

- 앱 패키지: com.pdftruth
- 기준 빌드: releaseRuntimeClasspath
- 점검일: 2026-06-12

## 주요 런타임 라이브러리 고지

| 라이브러리 | 버전 | 라이선스 | 사용 목적 | 고지 필요 여부 |
|---|---|---|---|---|
| Kotlin Standard Library (`org.jetbrains.kotlin:kotlin-stdlib`) | 1.9.24 | Apache-2.0 | Kotlin 런타임 | 예 |
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.13.1 | Apache-2.0 | Android 기본 확장 API | 예 |
| AndroidX Lifecycle Runtime KTX (`androidx.lifecycle:lifecycle-runtime-ktx`) | 2.8.3 | Apache-2.0 | 라이프사이클 처리 | 예 |
| AndroidX Lifecycle ViewModel KTX (`androidx.lifecycle:lifecycle-viewmodel-ktx`) | 2.8.3 | Apache-2.0 | ViewModel 상태 관리 | 예 |
| AndroidX Activity Compose (`androidx.activity:activity-compose`) | 1.9.1 | Apache-2.0 | Compose 기반 Activity 통합 | 예 |
| AndroidX DataStore Preferences (`androidx.datastore:datastore-preferences`) | 1.1.1 | Apache-2.0 | 최근 문서/마지막 페이지 로컬 저장 | 예 |
| Jetpack Compose UI (`androidx.compose.ui:ui`) | 1.6.8 | Apache-2.0 | UI 렌더링 | 예 |
| Jetpack Compose UI Graphics (`androidx.compose.ui:ui-graphics`) | 1.6.8 | Apache-2.0 | 비트맵/UI 그래픽 | 예 |
| Jetpack Compose UI Tooling Preview (`androidx.compose.ui:ui-tooling-preview`) | 1.6.8 | Apache-2.0 | 프리뷰 지원 어노테이션 | 예 |
| Jetpack Compose Material3 (`androidx.compose.material3:material3`) | 1.2.1 | Apache-2.0 | Material3 컴포넌트 | 예 |
| Kotlin Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android`) | 1.7.3 | Apache-2.0 | 비동기 처리 | 예 |
| Okio (`com.squareup.okio:okio`) | 3.4.0 | Apache-2.0 | DataStore 내부 I/O 의존 | 예 |

## 테스트 전용 의존성 (APK 비포함)

| 라이브러리 | 버전 | 라이선스 | 비고 |
|---|---|---|---|
| JUnit | 4.13.2 | EPL-1.0 | 단위 테스트용, 배포 APK 비포함 |
| Hamcrest Core | 1.3 | BSD-3-Clause | 테스트 매처, 배포 APK 비포함 |
| AndroidX Test / Espresso | 1.2.1 / 3.6.1 | Apache-2.0 | 계측 테스트용, 배포 APK 비포함 |

## 리소스(이미지/폰트/아이콘) 점검 결과

- `app/src/main/res`에는 `values/strings.xml`만 존재하며, 외부 폰트/이미지/아이콘 파일이 직접 포함되어 있지 않습니다.
- 앱 아이콘은 시스템 기본 아이콘(`@android:drawable/sym_def_app_icon`)을 사용합니다.

## 라이선스 준수 메모

- Apache-2.0 라이선스는 저작권 및 라이선스 고지 보존이 필요합니다.
- 본 파일을 APK 배포 페이지와 함께 제공하여 고지 누락 위험을 줄입니다.
- 향후 의존성 변경 시 본 문서를 함께 갱신해야 합니다.

## 확인이 필요한 항목

- 본 문서는 법률 자문이 아닙니다.
- 상표/저작권 최종 판단(앱 이름, 소개 문구)은 별도 법률 검토 또는 상표 검색으로 보완해야 합니다.
