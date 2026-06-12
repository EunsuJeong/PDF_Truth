# APK Release Checklist (직접 배포용)

점검일: 2026-06-12
앱: PDF Truth
버전: v1.0.0
배포 파일: release/PDFTruth_v1.0.0_260612.apk

## 1. 배포 출처/파일 관리

- [x] 공식 배포 파일명에 버전/날짜 포함
- [x] 프로젝트 내부 폴더에 APK 저장
- [ ] 공식 배포 페이지 URL 확정
- [ ] 배포자명(법인/개인) 명시
- [x] SHA-256 해시 제공
- [x] 최신 버전과 구버전 구분 규칙 정의
- [x] 변조/재배포 주의 문구 작성

## 2. 저작권/상표권

- [x] 앱 이름 사용 문구 점검(타사 상표 직접 사용 회피)
- [x] 금지 표현 회피 가이드 반영
- [x] 앱 리소스 내 외부 이미지/폰트/아이콘 직접 포함 없음 확인
- [ ] 상표 사전 검색(KIPRIS/WIPO 등) 최종 확인
- [ ] 스크린샷/샘플 PDF 저작권 검수

## 3. 오픈소스 라이선스

- [x] `app:dependencies --configuration releaseRuntimeClasspath` 실행
- [x] `THIRD_PARTY_NOTICES.md` 작성
- [x] 테스트 전용 의존성과 런타임 의존성 구분
- [ ] 각 라이선스 전문(원문) 번들 제공 방식 확정

## 4. 개인정보/데이터

- [x] 개인정보 직접 수집 없음 문구 명시
- [x] PDF 서버 전송 없음 문구 명시
- [x] 광고/분석 SDK 없음 문구 명시
- [x] 로컬 저장 항목(최근 문서/마지막 페이지) 명시
- [x] 앱 삭제 시 데이터 삭제 안내 반영

## 5. Android 권한/보안

- [x] `MANAGE_EXTERNAL_STORAGE` 없음
- [x] 위치/카메라/마이크/연락처/문자/전화 권한 없음
- [x] 인터넷 권한 없음
- [x] exported Activity 확인(Launcher Activity 1개)
- [x] URI 권한 예외 처리 코드 존재 확인
- [x] 최근 문서 삭제는 목록 삭제이며 실제 PDF 삭제 아님 안내

## 6. 빌드/서명/해시

- [x] `./gradlew clean` 성공
- [x] `./gradlew assembleRelease` 성공
- [x] 서명 APK 생성
- [x] `apksigner verify --verbose --print-certs` 통과(v2/v3)
- [x] SHA-256 해시 생성

## 7. 사용자 안내/면책

- [x] 설치 방법 문구 작성
- [x] 외부 출처 설치 허용/재비활성화 권장 안내
- [x] 무료 제공 고지
- [x] 기능 한계(암호/손상 PDF) 안내
- [x] 백업 권장 안내
- [x] 면책 범위 문구 작성(고의/중과실 면제 문구 회피)

## 8. 상표/악성코드 검사 기록

- [x] 상표 선행조사 기록 파일 생성 (`trademark-search-record.md`)
- [x] GitHub 검색: 동일 명칭 타사 저장소 없음(개발자 본인 저장소만 확인)
- [x] Google Play 검색: 동일 명칭 타사 앱 없음
- [ ] KIPRIS 직접 확인(브라우저 수동 필요)
- [ ] WIPO Brand DB 직접 확인(브라우저 수동 필요)
- [x] 악성코드 검사 기록 파일 생성 (`apk-security-scan-record.md`)
- [ ] 악성코드 검사 도구(VirusTotal/MobSF 등) 실행 및 결과 기록

## 9. 배포 전 수동 입력 미완료 항목

- [x] 문의 이메일 입력 (euntrue@kakao.com)
- [ ] 공식 배포 URL 입력 (현재: 미입력)
- [x] 배포자명 입력 (Euntrue)

## 10. 문서 URL 확정 필요

- [ ] 개인정보처리방침 URL 확정
- [ ] 이용약관 URL 확정
- [ ] 오픈소스 라이선스 고지 URL 확정

> 주의: 위 항목은 실제 값이 확정되기 전까지 배포하지 마세요.

---

참고: 본 체크리스트는 법률 자문이 아니라 법적 리스크를 줄이기 위한 사전 점검용입니다.
