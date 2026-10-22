# AGENTS.md

AI 개발 도구가 작업 전에 확인할 공통 지침과 문서 탐색 순서다.

## 문서 읽기 순서

1. [README.md](README.md)에서 프로젝트 실행 방법과 주요 문서 링크를 확인한다
2. [docs/README.md](docs/README.md)에서 작업별 필수 문서를 확인한다
3. 현재 작업과 관련된 문서만 골라서 확인한다
4. 설계 배경이 필요하면 [docs/decisions/](docs/decisions/README.md)의 ADR을 확인한다

## 공통 지침

- 문서에 정해진 규칙을 따른다. 문서에 없는 규칙은 임의로 정하지 않고 먼저 확인한다
- 기능 범위는 [docs/product-scope.md](docs/product-scope.md)의 MVP 열을 기준으로 구현한다
- 커밋·브랜치·PR은 [docs/development-cycle.md](docs/development-cycle.md)를 따른다
- PR 전에는 [docs/quality-gates.md](docs/quality-gates.md)를 확인한다
