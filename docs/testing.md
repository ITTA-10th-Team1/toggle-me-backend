# Testing

테스트 종류와 네이밍 규칙이다. 원본: Notion `Class Naming Convention`.
실행 방법은 [README.md](../README.md#로컬-실행).

## 테스트 클래스 네이밍

- 단위·슬라이스 테스트는 `{대상 클래스}Test` 형식을 사용한다 (예: `ItemServiceTest`, `ItemRepositoryTest`, `ItemControllerTest`)
- 여러 계층을 함께 검증하는 통합 테스트는 `{기능}IntegrationTest` 형식을 사용한다 (예: `SharedRoomIntegrationTest`)
- 테스트 클래스는 대상 클래스와 같은 패키지 경로에 둔다
