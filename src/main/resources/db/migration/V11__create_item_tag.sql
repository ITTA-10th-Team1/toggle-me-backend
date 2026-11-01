CREATE TABLE item_tag
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    item_id    BIGINT      NOT NULL,
    -- 서버가 소문자로 정리해 저장한다. 기본 collation(대소문자·악센트 무시)이면 서버가 다르다고 본 태그를 DB가 중복으로 막아 500이 나므로 바이트 그대로 비교한다
    name       VARCHAR(20) COLLATE utf8mb4_bin NOT NULL,
    position   INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_item_tag_item_name UNIQUE (item_id, name),
    INDEX idx_item_tag_name (name),
    -- 아이템을 지우면 태그도 함께 지운다
    CONSTRAINT fk_item_tag_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
);
