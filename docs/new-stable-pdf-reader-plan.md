# 새 안정형 PDF 리더기 개발 계획

---

## 1. 새 PDF 리더기 목표
- 광고/로그인/구독/개인정보 수집/외부 서버 연동 없는 완전 무료 오프라인 PDF 리더기
- Android 기기 내부 PDF 파일 열람에 최적화
- 불필요한 기능 배제, 핵심 리더 기능에 집중

## 2. 기존 문제점
1. 기능 과다/빠른 확장
2. 단일/다중 페이지 혼합 구조
3. 제스처 충돌
4. 페이지/렌더링 상태 불일치
5. PDF 비율 강제 및 깨짐
6. APK 파일명/Wrapper/CI 관리 불안정
7. 핵심 기능보다 부가 기능 우선
8. APK 설치 테스트 부족

## 3. 새 개발 원칙
1. 한 번에 한 기능만 구현
2. 각 단계마다 APK 빌드 및 설치 테스트
3. PDF 한 장 보기 완성 전 다른 기능 금지
4. 페이지 넘김 완성 전 Zoom 금지
5. Zoom 완성 전 검색/썸네일/북마크 금지
6. 제스처는 단일 Controller에서만 처리
7. ViewModel이 페이지 상태 단일 기준
8. PdfRenderer는 Engine 내부에서만 사용
9. PDF 비율 강제 금지
10. LazyColumn 다중 페이지 리더 금지
11. PR 기반 개발
12. master 직접 commit/push 금지

## 4. MVP 1차 포함 기능
- PDF 파일 선택
- SAF 권한 유지
- PDF 열기
- 현재 페이지 1장 표시
- 이전/다음 페이지 버튼
- 현재/전체 페이지 표시
- 첫/마지막 페이지 경계 처리
- 마지막 읽은 페이지 저장
- 최근 문서 목록
- 권한 만료/파일 삭제/손상 PDF 오류 안내

## 5. MVP 1차 제외 기능
- 검색, 썸네일, 북마크, Pinch Zoom, Double Tap Zoom, Pan, 메모, 형광펜, PDFium, MuPDF, OCR, 클라우드, 로그인, 광고, PDF 편집, 다중 페이지 LazyColumn

## 6. 패키지 구조
com.pdftruth
- ui
- ui.screen
- ui.navigation
- ui.theme
- viewmodel
- domain
- domain.model
- domain.repository
- data
- data.local
- data.repository
- database
- storage
- viewer
- viewer.engine
- util

## 7. ReaderUiState 설계
- documentUri: PDF 파일 URI
- fileName: 파일명
- currentPageIndex: 현재 페이지(0-base)
- totalPageCount: 전체 페이지 수
- currentPageBitmap: 현재 페이지 Bitmap
- isLoading: 로딩 상태
- errorMessage: 오류 메시지

## 8. PdfRendererEngine 설계
- openDocument(uri)
- getPageCount()
- renderPage(pageIndex, targetWidth, targetHeight)
- close()

## 9. ViewerViewModel 설계
- openPdf(uri)
- renderPage(pageIndex)
- goToPreviousPage()
- goToNextPage()
- goToPage(pageIndex)
- saveCurrentPage()
- closePdf()

## 10. PDF 표시 기준
- 현재 페이지 1장만 표시
- ContentScale.Fit
- Bitmap 비율 유지
- 화면 중앙 정렬
- 어두운 배경, 흰색 문서
- 고정 A4 비율/FillBounds/LazyColumn/제스처 금지

## 11. 페이지 이동 기준
- 하단 버튼(이전/현재/다음)
- currentPageIndex: 0-base, UI는 +1
- 첫/마지막 페이지 버튼 비활성화
- 범위 검사 필수, 예외 시 errorMessage

## 12. 마지막 페이지 저장 기준
- URI 기준 저장
- 페이지 이동 시 저장
- 같은 PDF 재열람 시 복원
- 최근 문서: URI, 파일명, 마지막 페이지, 열람 시간

## 13. APK 파일명 규칙
- PDF_YYMMDD_NUMBER.apk
- YYMMDD: 빌드 날짜, NUMBER: 일별 빌드 번호
- 기존 APK 덮어쓰기 금지, 자동 번호 증가
- 하드코딩 금지

## 14. 개발 단계
- Phase 0: Gradle Wrapper, CI, APK 파일명, PR, 아이콘, Splash
- Phase 1: PDF 선택, URI 권한, PdfRenderer open, pageCount, 첫 페이지 렌더링
- Phase 2: 단일 페이지 리더, 이전/다음 버튼, 현재/전체 페이지, 경계 처리
- Phase 3: 마지막 읽은 페이지 저장, 최근 문서, 권한 만료/파일 삭제 안내
- Phase 4: 좌우 터치 페이지 넘김
- Phase 5: Pinch Zoom(Phase 1~4 안정화 후)

## 15. 테스트 체크리스트
- APK 파일명 규칙 준수
- PR 기반 개발/테스트
- PDF 파일 선택/권한/열기/페이지 이동/저장/복원/오류 안내 정상 동작
- 각 단계별 실제 기기 설치 테스트
- 불필요 기능/제스처/비율 강제/다중 페이지/검색/썸네일/북마크/광고/로그인/클라우드/외부 연동/개인정보 수집 없음
