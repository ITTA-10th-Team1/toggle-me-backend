# Development Cycle

브랜치, 커밋, PR, 리뷰, 병합 절차다.
원본: 팀 Notion `BE` > Git Branch Naming Convention, Git Commit Message Convention.

## 브랜치

```plain text
main
├── develop
│   ├── feat/*
│   ├── fix/*
│   ├── refactor/*
│   └── chore/*
└── hotfix/*
```

- `main`: 배포 가능한 상용 버전
- `develop`: 개발 통합 브랜치, 모든 기능 PR의 기본 도착지

### 이름 형식

```plain text
<prefix>/<description>
<prefix>/<issue-number>-<description>   # 이슈를 사용하는 경우
```

| Prefix | 사용 기준 | 예시 |
| --- | --- | --- |
| `feat/` | 새로운 기능 개발 | `feat/share-link` |
| `fix/` | 개발 중 발견된 버그 수정 | `fix/item-owner-check` |
| `refactor/` | 기능 변화 없는 코드 구조 개선 | `refactor/room-query` |
| `chore/` | 설정·문서·테스트 등 보조 작업 | `chore/s3-setting` |
| `hotfix/` | 배포 후 긴급 수정 | `hotfix/presigned-url-expire` |

### 작성 기준

- 영문 소문자와 숫자 사용
- 단어 구분은 하이픈(-) 사용
- 브랜치명은 작업 목적이 드러나는 명사형 사용
- 하나의 브랜치에는 하나의 작업 목적만 포함
- 기능 브랜치는 `develop`에서 생성하고 `develop`으로 PR
- `hotfix` 브랜치는 `main`에서 생성하고 수정 후 `main`과 `develop`에 반영

```plain text
feat/item-upload
feat/12-share-link
fix/15-item-owner-check
refactor/room-query
chore/initial-setting
```

## 커밋 메시지

```plain text
<type>(<scope>): <description>
```

```plain text
feat(room): 공유 링크 발급 기능 추가
fix(item): 아이템 조회 오류 수정
refactor(member): 회원 프로필 수정 로직 분리
build: QueryDSL 의존성 추가
```

- squash merge를 사용하므로 `develop`에 남는 커밋은 PR 제목이다. **PR 제목도 같은 형식으로 작성한다**
- 이슈 번호는 커밋 메시지가 아닌 PR 본문에 작성한다 (예: `Closes #12`)

### Type

| Type | 사용 기준 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 구조 개선 |
| `test` | 테스트 코드 추가 및 수정 |
| `docs` | 문서 추가 및 수정 |
| `build` | 빌드 도구, 의존성, 프로젝트 설정 변경 |
| `ci` | CI/CD 설정 및 스크립트 변경 |
| `perf` | 성능 개선 |
| `chore` | 위 유형에 포함되지 않는 유지보수 작업 |
| `revert` | 이전 커밋 되돌림 |

- 커밋의 주된 목적을 기준으로 Type을 고른다
- 하나의 커밋에 여러 목적이 섞이면 커밋을 나눈다
- 판단이 어려울 때 `chore`를 먼저 쓰지 않고 변경 목적을 다시 확인한다
- `design`, `comment`, `rename`, `remove`, `settings` Type은 쓰지 않고 변경 목적에 맞는 Type을 고른다

| 브랜치 Prefix | 주로 사용하는 커밋 Type |
| --- | --- |
| `feat/` | `feat` |
| `fix/`, `hotfix/` | `fix` |
| `refactor/` | `refactor`, `perf` |
| `chore/` | `test`, `docs`, `build`, `ci`, `chore` |

### Scope

| Scope | 적용 범위 |
| --- | --- |
| `auth` | 로그인·인증 (방식 확정 후 구체화) |
| `member` | 회원 정보 및 프로필 |
| `room` | 내 방 조회, 방 공개 설정, 공유 링크 발급·폐기·조회 |
| `item` | 아이템 등록·조회·수정·삭제, 아이템 공개 설정 |
| `image` | S3 이미지 업로드 및 관리 |
| `global` | 공통 응답, 예외 처리, 보안 공통 설정 |

- 영문 소문자, 단수형
- 클래스명이나 메서드명이 아닌 도메인 경계 사용
- 변경 대상이 명확한 경우에만 선택적으로 사용하고, 중심 도메인이 없는 공통 변경은 생략
- 여러 도메인에 걸친 변경은 가장 핵심적인 도메인을 선택

### Description

- 한글, 명사형 종결
- 무엇이 변경되었는지 결과 중심으로 작성
- 50자 이내 권장, 끝에 마침표 미사용

| 지양 | 사유 | 개선 |
| --- | --- | --- |
| `feat(auth): 로그인` | 변경 내용의 구체성 부족 | `feat(auth): 소셜 로그인 기능 추가` |
| `fix(item): 오류를 수정했습니다.` | 서술형 종결 및 마침표 사용 | `fix(item): 아이템 조회 오류 수정` |
| `refactor: 코드 정리` | 변경 범위와 결과가 불명확 | `refactor(member): 회원 프로필 수정 로직 분리` |

### 커밋 단위

하나의 커밋은 하나의 논리적 변경 목적만 포함한다.

- 기능 추가, 버그 수정, 리팩터링 등 서로 다른 목적은 나눈다
- 각 커밋은 가능한 범위에서 빌드·테스트 가능한 상태를 유지한다
- 같은 기능의 구현 코드와 관련 테스트는 하나의 커밋에 넣을 수 있다
- 기능 변경과 무관한 구조 개선은 별도 커밋으로 나눈다
- 의존성 추가나 공통 설정처럼 후속 작업의 기반이 되는 변경은 먼저 커밋한다
- 공유 브랜치에는 `WIP`, `수정`, `최종` 같은 임시 커밋을 남기지 않는다

아래 질문 중 하나라도 '아니오'면 커밋을 나눈다.

- 변경 목적이 동일한가?
- 하나의 커밋 메시지로 모든 변경을 구체적으로 설명할 수 있는가?
- 해당 변경만 되돌려도 다른 변경이 손상되지 않는가?
- 기능 구현과 직접 관련된 테스트 또는 필수 설정인가?

## PR·병합

- PR은 리뷰어 1명 승인 후 병합한다
- 병합 방식은 squash merge
- 병합된 브랜치는 삭제한다
- `main`, `develop`에는 직접 push하지 않는다 (GitHub 브랜치 보호 규칙)
- PR 전 확인 항목은 [quality-gates.md](quality-gates.md)
