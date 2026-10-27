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

## 작업 시작·완료 형식

작업을 시작하기 전에 아래 항목을 먼저 적는다.

| 항목 | 내용 |
| --- | --- |
| Goal | 작업이 끝났을 때의 결과 |
| Constraints | 지켜야 할 제약 |
| Done | 완료 조건 |

작업을 마치면 아래 항목을 남긴다.

- 실행한 명령과 결과
- 실행하지 못한 검사와 이유
- 남은 위험

## 개인 작업 폴더

개인 메모, 작업 계획, 실험 코드는 레포 루트의 `.local/`에 둔다. `.local/`은 커밋하지 않는다.

```plain text
.local/
├── notes/       # 개인 메모
├── plans/       # 작업 계획·진행 기록
└── prototypes/  # 실험 코드
```
