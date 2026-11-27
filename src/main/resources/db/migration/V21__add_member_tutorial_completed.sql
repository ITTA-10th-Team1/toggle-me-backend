-- 튜토리얼을 회원당 한 번만 보여 주기 위한 값. INSTANT로 테이블을 다시 만들지 않고, 안 되면 바로 실패한다
ALTER TABLE member
    ADD COLUMN tutorial_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ALGORITHM = INSTANT;
