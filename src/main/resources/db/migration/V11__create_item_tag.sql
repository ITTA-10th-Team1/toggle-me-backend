CREATE TABLE item_tag
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    item_id    BIGINT      NOT NULL,
    name       VARCHAR(20) NOT NULL,
    position   INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_item_tag_item_name UNIQUE (item_id, name),
    INDEX idx_item_tag_name (name),
    -- 아이템을 지우면 태그도 함께 지운다
    CONSTRAINT fk_item_tag_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
);
