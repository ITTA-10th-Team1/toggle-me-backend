-- 최신 방 GLB에서 거울·냉장고 노드의 배율은 (1, 1, 1)이다(프론트 확인 2026-11-21). V17의 예시 배율을 바꾼다.
UPDATE furniture_type
SET scale_x    = 1,
    scale_y    = 1,
    scale_z    = 1,
    updated_at = UTC_TIMESTAMP(6)
WHERE code IN ('mirror', 'fridge');

-- 방을 만들 때 배치 값이 방마다 복사된다. V17 기본 배율이 그대로 남은 가구만 바꾸고, 사용자가 바꾼 배치는 두지 않는다.
UPDATE room_furniture furniture
    JOIN furniture_variant variant ON variant.id = furniture.variant_id
    JOIN furniture_type type ON type.id = variant.furniture_type_id
SET furniture.scale_x = 1,
    furniture.scale_y = 1,
    furniture.scale_z = 1
WHERE ABS(furniture.scale_y - 1) < 0.000001
  AND ABS(furniture.scale_z - 0.55) < 0.000001
  AND ((type.code = 'mirror' AND ABS(furniture.scale_x - 0.25) < 0.000001)
    OR (type.code = 'fridge' AND ABS(furniture.scale_x - 0.3) < 0.000001));
