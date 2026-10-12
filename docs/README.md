# docs

전체 개발 문서의 목차와 작업 유형별 문서 탐색 경로다.

## 문서 목록

| 문서 | 역할 |
| --- | --- |
| [product-scope.md](product-scope.md) | MVP 범위, 핵심 기능, 비즈니스 규칙 및 보류 기능 |
| [architecture.md](architecture.md) | 시스템 구성, 도메인 경계, 의존 방향, 외부 시스템 연동 |
| [layer-boundaries.md](layer-boundaries.md) | Controller, Service, Repository, Domain, DTO의 책임과 참조 규칙 |
| [api-conventions.md](api-conventions.md) | API URL, 요청·응답, 오류, 인증 규칙 |
| [persistence.md](persistence.md) | DB 네이밍, 엔티티, 연관관계, 트랜잭션, 마이그레이션 규칙 |
| [testing.md](testing.md) | 테스트 종류와 네이밍 |
| [development-cycle.md](development-cycle.md) | 브랜치, 커밋, PR, 리뷰, 병합 절차 |
| [quality-gates.md](quality-gates.md) | PR 및 병합 전 확인 기준 |
| [decisions/](decisions/README.md) | 주요 결정의 ADR |
| [workflows/](workflows/README.md) | 반복 수행하는 개발·운영 절차 |

## 작업 유형별 탐색 경로

| 작업 | 읽을 문서 |
| --- | --- |
| 새 기능 구현 | product-scope → architecture → layer-boundaries → api-conventions → persistence → testing |
| Entity·스키마 변경 | persistence → decisions |
| API 추가·변경 | api-conventions → layer-boundaries |
| 커밋·PR | development-cycle → quality-gates |

## 관리 원칙

- 공통 규칙은 해당 주제 문서 한 곳에서만 관리한다
- 같은 내용을 여러 문서에 중복 작성하지 않고 원본 문서로 링크한다
- 결정되지 않은 내용은 미리 작성하지 않고, 확정된 내용부터 반영한다
- 특정 도메인에만 적용되는 복잡한 규칙이 생긴 경우에만 도메인 문서를 추가한다
