## 작업명

Play 스토어 등록 전 PDF 리더기 점검 및 출시 준비 개선

## 작업 목적

PDF 리더기 앱을 Google Play 스토어에 등록하기 전, 빌드 설정 / 권한 / 파일 접근 / 오류 안내 / 개인정보 리스크 / 릴리즈 문서를 점검하고 필요한 개선을 반영했습니다.

## 주요 변경사항

### 1. 외부 PDF 열기 정책 보강

- 외부 ACTION_VIEW 인텐트 처리 추가
- 중복 인텐트 방지
- 신규 파일 열기는 1페이지부터 시작하도록 개선

### 2. 마지막 페이지 복원 정책 분리

- 최근 문서 진입 시에만 마지막 페이지 복원
- 신규 파일 선택 및 외부 인텐트 진입 시에는 1페이지부터 시작
- restoreLastPage 인자를 통해 열기 동작을 명확히 분리

### 3. 사용자 오류 메시지 개선

- 비밀번호 PDF 미지원 안내
- 손상/미지원 PDF 안내
- 마지막 페이지 복원 실패 안내

### 4. 릴리즈 보안 설정 정리

- keystore 관련 민감 파일 Git 추적 제외
- .gitignore 보강
- keystore.properties.example 추가

### 5. Play 스토어 등록 준비 문서 추가

- release-build-guide.md
- play-store-release-check.md

## 빌드 검증

- [x] clean 성공
- [x] assembleDebug 성공
- [x] assembleRelease 성공
- [x] bundleRelease 성공

## 생성된 빌드 산출물

- PDF_260603_01.apk
- app-release-unsigned.apk
- app-release.aab

단, APK/AAB 빌드 산출물은 Git 커밋 대상에서 제외했습니다.

## 정책 리스크 점검

- [x] AndroidManifest 기준 위험 권한 없음
- [x] 광고 SDK 미검출
- [x] Firebase / Analytics / Ads 의존성 미검출
- [x] keystore 민감 파일 Git 추적 차단
- [x] Play Console 등록 전 확인 문서 추가

## Play Console 등록 전 남은 작업

- [ ] 앱 이름 최종 확정
- [ ] 앱 아이콘 512x512 PNG 준비
- [ ] 휴대폰 스크린샷 준비
- [ ] 기능 그래픽 준비
- [ ] 개인정보처리방침 URL 준비
- [ ] Play Console 데이터 세이프티 입력
- [ ] 콘텐츠 등급 설문 작성
- [ ] 앱 액세스 권한 입력
- [ ] 비공개 테스트 트랙 구성
- [ ] 테스터 명단 준비

## 테스트 필요 항목

- [ ] 실제 Android 기기에서 APK 설치 테스트
- [ ] 일반 PDF 열기
- [ ] 큰 용량 PDF 열기
- [ ] 암호 PDF 안내 확인
- [ ] 손상 PDF 안내 확인
- [ ] 확대/축소 확인
- [ ] 좌우 스와이프 확인
- [ ] 최근 문서 마지막 페이지 복원 확인
- [ ] 신규 파일 1페이지 시작 확인
- [ ] 외부 파일 열기 확인

## 커밋 메시지

chore: prepare PDF reader for Play Store release
