-- 책장 가구 기준 데이터: 가구 1종(디자인 1종). 거울·냉장고(V17)와 같은 방 GLB의 노드를 쓴다.
-- 좌표·노드 이름은 프론트 방 GLB 값(2026-11-28 전달): 노드 desk, 위치 아래 값, 회전 없음(0,0,0,1), 배율 1.
-- code는 서버 책장 규칙(태그 지침·책 추천·하루 기록 수)이 쓰는 bookshelf다.
-- 이미 방이 있는 회원은 책장이 숨김 기본값으로 조회되고, 방 편집에서 켜면 놓인다(RoomService).
-- 이미 같은 데이터가 있으면 건너뛴다(운영에 손으로 넣은 행이 있어도 실패하지 않게).

INSERT INTO furniture_type (code, name, display_order,
                            position_x, position_y, position_z,
                            rotation_x, rotation_y, rotation_z, rotation_w,
                            scale_x, scale_y, scale_z,
                            render_order, status, created_at, updated_at)
VALUES ('bookshelf', '책장', 3,
        -1.0408016920089722, 0.385222852230072, -0.021670036017894745,
        0, 0, 0, 1,
        1, 1, 1,
        3, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
AS seed
ON DUPLICATE KEY UPDATE position_x   = seed.position_x,
                        position_y   = seed.position_y,
                        position_z   = seed.position_z,
                        rotation_x   = seed.rotation_x,
                        rotation_y   = seed.rotation_y,
                        rotation_z   = seed.rotation_z,
                        rotation_w   = seed.rotation_w,
                        scale_x      = seed.scale_x,
                        scale_y      = seed.scale_y,
                        scale_z      = seed.scale_z,
                        render_order = seed.render_order,
                        updated_at   = UTC_TIMESTAMP(6);

INSERT INTO furniture_variant (furniture_type_id, name, asset_object_key, object_node_name,
                               width_units, height_units, status, created_at, updated_at)
SELECT type.id, '기본 책장', 'assets/models/toggleme_room.glb', 'desk', 1, 1, 'ACTIVE',
       UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM furniture_type type
WHERE type.code = 'bookshelf'
  AND NOT EXISTS (SELECT 1 FROM furniture_variant variant WHERE variant.furniture_type_id = type.id);
