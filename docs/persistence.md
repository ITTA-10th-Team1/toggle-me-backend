# Persistence

DB 네이밍, 엔티티, 연관관계, 트랜잭션, 마이그레이션 규칙이다.
원본: Notion `Code Convention`, `Class Naming Convention`. ERD는 Notion `ERD 설계`.

## DB 네이밍

- 테이블명과 컬럼명은 snake_case 단수형을 사용한다

## 엔티티

- 도메인 명사를 접미사 없이 사용한다 (`Item` ○ / `ItemEntity` ✕)
- 공통 부모 클래스는 `Base` 접두사를 사용한다 (`BaseTimeEntity`)
- `@Getter`와 `@NoArgsConstructor(access = PROTECTED)`만 기본 적용한다
- `@Data`, 클래스 단위 `@Setter`, `@Builder`를 사용하지 않는다
- 필드를 외부에서 직접 변경하는 public Setter를 만들지 않는다
- 생성은 의미가 드러나는 정적 팩토리 메서드를 사용한다
- 값 변경은 `updateMemo`, `changeVisibility`처럼 행동을 나타내는 메서드를 사용한다
- 내부 ID에는 `Long`을 사용한다
- Enum은 영문 대문자로 작성하고 `EnumType.STRING`으로 저장한다

```java
public static Item create(Room room, FurnitureCategory category, String imageKey, String memo) {
    return new Item(room, category, imageKey, memo);
}

public void updateMemo(String memo) {
    this.memo = memo;
}
```

### 공통 시간 필드

- `BaseTimeEntity`에는 `createdAt`, `updatedAt`만 선언한다
- Spring Data JPA Auditing을 사용한다
- ID는 공통 부모에 넣지 않고 Entity별로 관리한다
- 날짜·시간은 `Instant`를 사용하며 UTC 기준으로 저장한다
- JPA 타임존은 `hibernate.jdbc.time_zone: UTC`로 고정한다

## 연관관계

- 실제로 필요한 방향만 연관관계로 매핑한다
- 기본적으로 자식에서 부모로 향하는 단방향 관계를 사용한다
- `@ManyToOne`에는 `fetch = FetchType.LAZY`를 적용한다
- 필요성이 확인되기 전까지 부모 Entity에 `@OneToMany`를 추가하지 않는다
- 목록은 Repository 쿼리로 조회한다

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "room_id", nullable = false)
private Room room;
```

## 트랜잭션

- Service 클래스에는 `@Transactional(readOnly = true)`를 기본 적용한다
- 생성·수정·삭제 메서드에만 `@Transactional`을 별도로 적용한다

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    @Transactional
    public Long createItem(...) {
        // ...
    }
}
```

## 마이그레이션

- DB 변경은 Flyway 마이그레이션으로 관리한다
- `ddl-auto`는 `validate`를 사용한다
- 실행된 마이그레이션 파일은 수정하지 않는다
- Entity 변경과 마이그레이션 SQL을 같은 PR에 포함한다

```plain text
V1__init.sql
V2__add_item_memo.sql
```

## 제약 조건

- MVP에서 `room.owner_id`는 unique다 ([ADR 0001](decisions/0001-room-count-per-member.md))
