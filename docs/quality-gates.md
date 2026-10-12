# Quality Gates

PR 및 병합 전에 확인하는 기준이다. 각 항목의 상세 규칙은 링크된 문서를 따른다.

## PR 전

- [ ] PR 제목이 커밋 메시지 형식이다 ([development-cycle.md](development-cycle.md#커밋-메시지))
- [ ] 하나의 브랜치·PR에 하나의 작업 목적만 있다 ([development-cycle.md](development-cycle.md#브랜치))
- [ ] Entity 변경과 마이그레이션 SQL이 같은 PR에 있고, 실행된 마이그레이션 파일을 수정하지 않았다 ([persistence.md](persistence.md#마이그레이션))
- [ ] Entity를 API 요청·응답으로 직접 노출하지 않았다 ([layer-boundaries.md](layer-boundaries.md#dto))
- [ ] 사진 URL, 메모, 공유 토큰 원문을 로그에 남기지 않았다
- [ ] `.env`와 비밀 키가 커밋에 없다

## 병합 전

- [ ] 리뷰어 1명 이상 승인을 받았다
- [ ] squash merge로 병합하고, 병합된 브랜치를 삭제한다
