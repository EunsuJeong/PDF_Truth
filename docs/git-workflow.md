# Git/PR 워크플로

## 기본 원칙
- main 브랜치 직접 push 금지
- branch 전략: phase/*, feature/*, fix/*, docs/*
- 작업 단위별 commit 후 push
- Pull Request 생성 후 리뷰 진행
- CI 통과 확인 후 merge
- 모든 작업 보고는 한국어로 작성

## 작업 절차
1. main 최신화
2. 작업 브랜치 생성
3. 변경 작업 및 로컬 빌드 검증
4. git add / git commit
5. 원격 push
6. PR 생성
7. CI 통과 및 리뷰 완료 후 merge

## 금지 사항
- CI 실패 상태에서 merge 금지
- 기능 요구사항 범위를 벗어난 무단 구현 금지
- 과도한 기능 선반영 금지
