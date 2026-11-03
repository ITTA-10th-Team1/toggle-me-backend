-- 단방향 팔로우. 승인 없이 바로 맺는다. 방을 OFF로 바꿔도 지우지 않는다(다시 ON이면 그대로 복원)
CREATE TABLE follow
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    follower_id BIGINT      NOT NULL,
    followee_id BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_follower_followee UNIQUE (follower_id, followee_id),
    -- 팔로워 목록·수 조회용. 팔로잉 쪽은 UK의 앞 컬럼이 맡는다
    INDEX idx_follow_followee (followee_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES member (id),
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES member (id)
);
