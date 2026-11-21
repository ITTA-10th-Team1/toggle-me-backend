-- 메모 없음은 null 하나로 맞춘다(Item.record가 이제 빈 메모를 null로 저장한다).
-- 이전에 빈 문자열·공백만 저장된 메모를 null로 바꾼다. 사용자가 고친 기록이 아니라서 updated_at은 그대로 둔다.
-- TRIM은 공백만 지우고 탭·줄바꿈은 지우지 않아서 이들도 함께 비교한다.
UPDATE item
SET memo = NULL
WHERE memo IS NOT NULL
  AND TRIM(REPLACE(REPLACE(REPLACE(memo, CHAR(9), ' '), CHAR(10), ' '), CHAR(13), ' ')) = '';
