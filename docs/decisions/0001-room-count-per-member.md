# 0001. 사용자당 방 개수

- 상태: 확정
- 결정일: 2026-10-11

## 맥락

MVP는 사용자당 방 1개로 시작하고, 최종 버전에서는 여러 방을 허용한다.
MVP 기간에는 동시 요청이 들어와도 방이 중복으로 생기지 않아야 한다.

## 결정

- MVP: 사용자당 방 1개. `room.owner_id`에 unique 제약(`uk_room_owner`)을 걸어 DB에서 보장한다
- 최종: 여러 방을 허용할 때 새 마이그레이션으로 unique 제약을 해제한다

## 결과

MySQL은 외래키 컬럼에 인덱스가 필요하므로, 해제 마이그레이션은 일반 인덱스를 먼저 만든 뒤 unique를 지운다.

```sql
CREATE INDEX idx_room_owner ON room (owner_id);
ALTER TABLE room DROP INDEX uk_room_owner;
```
