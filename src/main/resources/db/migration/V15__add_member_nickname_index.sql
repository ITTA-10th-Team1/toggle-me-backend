-- 회원 검색에서 닉네임 앞부분 일치(LIKE 'abc%')로 찾을 때 쓴다
CREATE INDEX idx_member_nickname ON member (nickname);
