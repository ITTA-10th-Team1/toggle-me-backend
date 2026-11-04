-- 프론트 GLB 장면 좌표(3D, +Y가 위쪽, 바닥은 X·Z)에 맞춘다. 위치·회전(쿼터니언 XYZW)·크기를 원래 값 그대로 저장한다
-- 기존 2D 값은 바닥 위치(x, y)였으므로 3D 바닥 평면(x, z)으로 옮기고, 회전 없음(0,0,0,1)·크기 1로 채운다.
-- 기존 rotation(각도 하나)은 단위가 정해지지 않았고 운영 기준 데이터가 없어 변환하지 않고 지운다

ALTER TABLE furniture_type
    MODIFY COLUMN position_x DOUBLE NULL,
    MODIFY COLUMN position_y DOUBLE NULL,
    ADD COLUMN position_z DOUBLE NULL AFTER position_y,
    ADD COLUMN rotation_x DOUBLE NULL AFTER position_z,
    ADD COLUMN rotation_y DOUBLE NULL AFTER rotation_x,
    ADD COLUMN rotation_z DOUBLE NULL AFTER rotation_y,
    ADD COLUMN rotation_w DOUBLE NULL AFTER rotation_z,
    ADD COLUMN scale_x DOUBLE NULL AFTER rotation_w,
    ADD COLUMN scale_y DOUBLE NULL AFTER scale_x,
    ADD COLUMN scale_z DOUBLE NULL AFTER scale_y;

UPDATE furniture_type
SET position_z = position_y,
    position_y = 0,
    rotation_x = 0,
    rotation_y = 0,
    rotation_z = 0,
    rotation_w = 1,
    scale_x    = 1,
    scale_y    = 1,
    scale_z    = 1
WHERE position_x IS NOT NULL;

ALTER TABLE furniture_type DROP COLUMN rotation;

-- 프론트 categoryId(fridge, mirror)와 같은 소문자 코드를 쓴다
UPDATE furniture_type SET code = LOWER(code);

ALTER TABLE room_furniture
    MODIFY COLUMN position_x DOUBLE NOT NULL,
    MODIFY COLUMN position_y DOUBLE NOT NULL,
    ADD COLUMN position_z DOUBLE NOT NULL DEFAULT 0 AFTER position_y,
    ADD COLUMN rotation_x DOUBLE NOT NULL DEFAULT 0 AFTER position_z,
    ADD COLUMN rotation_y DOUBLE NOT NULL DEFAULT 0 AFTER rotation_x,
    ADD COLUMN rotation_z DOUBLE NOT NULL DEFAULT 0 AFTER rotation_y,
    ADD COLUMN rotation_w DOUBLE NOT NULL DEFAULT 1 AFTER rotation_z,
    ADD COLUMN scale_x DOUBLE NOT NULL DEFAULT 1 AFTER rotation_w,
    ADD COLUMN scale_y DOUBLE NOT NULL DEFAULT 1 AFTER scale_x,
    ADD COLUMN scale_z DOUBLE NOT NULL DEFAULT 1 AFTER scale_y;

UPDATE room_furniture
SET position_z = position_y,
    position_y = 0;

ALTER TABLE room_furniture
    DROP COLUMN rotation,
    ALTER COLUMN position_z DROP DEFAULT,
    ALTER COLUMN rotation_x DROP DEFAULT,
    ALTER COLUMN rotation_y DROP DEFAULT,
    ALTER COLUMN rotation_z DROP DEFAULT,
    ALTER COLUMN rotation_w DROP DEFAULT,
    ALTER COLUMN scale_x DROP DEFAULT,
    ALTER COLUMN scale_y DROP DEFAULT,
    ALTER COLUMN scale_z DROP DEFAULT;

-- GLB 안에서 이 디자인을 찾는 그룹 노드 이름 (예: 거울 stand, 냉장고 body)
ALTER TABLE furniture_variant
    ADD COLUMN object_node_name VARCHAR(50) NULL AFTER asset_object_key;
