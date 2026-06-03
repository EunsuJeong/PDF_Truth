# Google Play Release Check

## Scope
This document summarizes release readiness for PDF Truth before Google Play upload.

## 1) Core Behavior Check
- PDF open from picker: supported.
- Swipe page navigation: supported.
- Pinch zoom: supported.
- Recent documents list: supported.
- External `ACTION_VIEW` PDF open: supported.
- New picker/external open starts at page 1.
- Opening from recent documents restores the last page.

## 2) Security and Privacy
- Dangerous permissions: none declared.
- Ad/analytics/crash SDKs: none found.
- User data collection: none implemented in app logic.

## 3) Build and Signing Readiness
- Release signing: configured through `keystore.properties`.
- Generated output naming: configured (`PDF_YYMMDD_NN`).
- Required files:
  - `keystore.properties.example` (committed)
  - `keystore.properties` (local only, gitignored)

## 4) User-Facing Error Messages
Current app messages include:
- File moved/deleted warning.
- Expired permission warning.
- Password-protected PDF unsupported warning.
- Corrupted/unsupported PDF warning.
- Last-page restore fallback warning.

## 5) Policy Risk Notes
- No over-privileged permissions found.
- No background tracking/data upload logic found.
- Main policy risk remains store listing accuracy (description/screenshots/privacy form consistency).

## 6) Pre-Upload Final Checklist
- [ ] Build `assembleDebug` success
- [ ] Build `assembleRelease` success
- [ ] Build `bundleRelease` success
- [ ] Release APK signature verified
- [ ] Store listing text finalized
- [ ] Data Safety form reviewed
- [ ] Real-device smoke test complete

## 7) Suggested Play Listing Draft (Korean)
- App name: PDF Truth
- Short description: 빠르고 가벼운 PDF 뷰어, 최근 문서와 마지막 읽은 페이지를 기억합니다.
- Full description:
  - PDF Truth는 문서를 빠르게 열고, 스와이프와 핀치 줌으로 편하게 읽을 수 있는 PDF 뷰어입니다.
  - 새 파일은 첫 페이지부터, 최근 문서는 마지막 읽은 페이지부터 이어서 확인할 수 있습니다.
  - 앱 내부에 광고나 추적 SDK를 포함하지 않았으며, 문서 읽기에 집중한 단순한 경험을 제공합니다.
