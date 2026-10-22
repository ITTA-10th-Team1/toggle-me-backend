# Layer Boundaries

Controller, Service, Repository, Domain, DTO의 책임과 참조 규칙이다.
원본: Notion `Code Convention`, `Class Naming Convention`.

## 계층별 책임

| 계층 | 책임 |
| --- | --- |
| Controller | HTTP 요청·응답, DTO 검증, 인증 사용자 전달, 상태 코드 |
| Service | 비즈니스 규칙, 권한 확인, Entity 조회·변경, 트랜잭션 |
| Repository | 데이터 접근만 |

Entity 작성과 트랜잭션 규칙은 [persistence.md](persistence.md).

## 검증 위치

| 위치 | 검증 내용 |
| --- | --- |
| DTO | 필수값, 길이, 형식 등 요청 자체 |
| Service | 권한, 중복, 존재 여부 등 DB 조회가 필요한 규칙 |
| Entity | 항상 지켜져야 하는 상태와 불변식 |

## DTO

- DTO는 Java `record`로 작성하고, 도메인의 `dto/` 패키지에 둔다
- Entity를 API 요청이나 응답으로 직접 노출하지 않는다
- Request DTO와 Response DTO를 분리한다
- 일반적인 응답은 기존 DTO를 재사용하고, 필드나 접근 권한이 실제로 다를 때만 전용 DTO를 추가한다
- 단일 Entity를 DTO로 변환할 때는 DTO의 `from()` 메서드를 사용한다
- 여러 Entity나 외부 데이터를 조합할 때만 별도 Mapper를 사용한다. Mapper는 데이터를 조립하는 역할만 하고 DB나 외부 API를 직접 조회하지 않는다

```java
public static ItemResponse from(Item item) {
    return new ItemResponse(
            item.getId(),
            item.getMemo(),
            item.getCreatedAt()
    );
}
```

## 공통 작성 규칙

- Controller와 Service의 생성자 주입에는 `@RequiredArgsConstructor`를 사용한다
- 메서드 이름은 수행하는 동작을 구체적으로 표현한다
- 들여쓰기 4칸, 줄 길이 120자, import 와일드카드(`*`) 미사용
- 포매터 설정은 `.editorconfig`로 공유한다

## 클래스 네이밍

- 클래스명은 PascalCase 단수형을 사용하고, 계층 역할은 접미사로 드러낸다
- 약어도 일반 단어처럼 첫 글자만 대문자로 쓴다 (`ShareUrlGenerator` ○ / `ShareURLGenerator` ✕)
- 의미 없는 접미사(`Info`, `Data`, `Manager`, `Util`)는 지양한다

| 종류 | 형식 | 예시 |
| --- | --- | --- |
| Controller | `{도메인}Controller`. 인증 여부나 접근 대상이 다른 API는 별도 Controller로 분리 (공유 조회는 `Shared` 접두사) | `ItemController`, `SharedRoomController` |
| Service | `{도메인}Service`. 조회·변경 Service는 분리하지 않고, 책임이 뚜렷하게 다른 기능만 별도 Service로 분리 | `ItemService`, `ShareLinkService` |
| Repository | `{Entity}Repository`. 커스텀 쿼리는 `{Entity}RepositoryCustom` + `{Entity}RepositoryImpl` | `ItemRepository`, `ItemRepositoryImpl` |
| Request DTO | `{도메인}{동작}Request`. 동작은 `Create`, `Update`, `Delete` 등 동사형 | `ItemCreateRequest`, `RoomVisibilityUpdateRequest` |
| Response DTO | 기본 `{도메인}Response`, 상세 `{도메인}DetailResponse`, 공개 조회는 `Shared` 접두사 | `ItemResponse`, `SharedRoomResponse` |
| Mapper | `{도메인}Mapper` | `RoomMapper` |
| Enum | PascalCase 단수형, 값은 UPPER_SNAKE_CASE | `FurnitureCategory.BOOKSHELF` |
| 상수 클래스 | `{용도}Constants`, 상수명은 UPPER_SNAKE_CASE | `ImageConstants` |

- 하나의 Controller는 하나의 도메인 자원만 다룬다
- 인터페이스와 `Impl` 구현체 분리는 구현체가 둘 이상 필요할 때만 사용한다
- 쿼리 메서드명은 조회 조건이 드러나게 작성한다 (`findAllByRoomIdAndCategory`)
- 목록 응답은 별도 DTO 없이 `List<ItemResponse>` 또는 페이지 객체로 반환한다
- 공개 조회 응답은 노출 필드를 최소화한다
- 하나의 클래스에서만 쓰는 상수는 해당 클래스 안에 `private static final`로 두고, 환경마다 달라지는 값은 설정 파일로 관리한다
