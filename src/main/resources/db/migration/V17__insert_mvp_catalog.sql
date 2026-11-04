-- MVP 카탈로그 기준 데이터: 거울·냉장고 2종(디자인 1종씩)과 기본 벽·바닥 스타일.
-- 좌표·노드 이름·코드는 프론트 backend-room-layout.json(layoutVersion demo-06-fridge-floor-edge) 값을 그대로 쓴다.
-- 이미 같은 데이터가 있으면 건너뛴다(운영에 손으로 넣은 행이 있어도 실패하지 않게).
-- 프론트 확인(2026-11-03):
--   asset_object_key: 앱에 포함된 방 GLB 경로다. S3 키가 아니며 프론트는 이 값으로 받지 않는다. 서버 배포용 키는 S3에 올린 뒤 정한다
--   가구 디자인은 '공유 방 GLB + 노드(object_node_name)'다. 가구마다 방을 따로 불러오지 않는다
--   벽·바닥은 GLB 내장 재질을 쓴다. room_style 행은 방 생성에 필요해서 두며 asset_object_key는 쓰이지 않는다
--   width_units·height_units: 프론트가 렌더링에 쓰지 않는 임시값 1. 크기는 GLB를 따른다
--   render_order: 거울 1, 냉장고 2. 앞뒤 표시는 GLB 깊이로 처리하고 이 값은 쓰지 않는다

INSERT INTO furniture_type (code, name, display_order,
                            position_x, position_y, position_z,
                            rotation_x, rotation_y, rotation_z, rotation_w,
                            scale_x, scale_y, scale_z,
                            render_order, status, created_at, updated_at)
VALUES ('mirror', '거울', 1,
        -1.3948961734771728, 0.38799723982810974, 1.667795181274414,
        0.5865176320075989, 0.39496466517448425, -0.5865176320075989, 0.39496463537216187,
        0.25, 1, 0.550000011920929,
        1, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('fridge', '냉장고', 2,
        1.1741273403167725, 1.186818853020668, -1.438000932456924,
        0, -0.7071065902709961, 0, 0.7071069478988647,
        0.30000001192092896, 1, 0.550000011920929,
        2, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
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
SELECT type.id, seed.name, 'assets/models/toggleme_room.glb', seed.node, 1, 1, 'ACTIVE',
       UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (SELECT 'mirror' AS code, '기본 거울' AS name, 'stand' AS node
      UNION ALL
      SELECT 'fridge', '기본 냉장고', 'body') seed
         JOIN furniture_type type ON type.code = seed.code
WHERE NOT EXISTS (SELECT 1 FROM furniture_variant variant WHERE variant.furniture_type_id = type.id);

-- 방 생성에는 활성 벽·바닥 스타일이 하나씩 있어야 한다. 프론트는 방 GLB 하나에 벽·바닥이 함께 있다
INSERT INTO room_style (type, name, asset_object_key, status, created_at, updated_at)
SELECT seed.type, seed.name, 'assets/models/toggleme_room.glb', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (SELECT 'WALL' AS type, '기본 벽' AS name
      UNION ALL
      SELECT 'FLOOR', '기본 바닥') seed
WHERE NOT EXISTS (SELECT 1 FROM room_style style WHERE style.type = seed.type AND style.status = 'ACTIVE');
