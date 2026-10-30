CREATE TABLE item
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    member_id         BIGINT       NOT NULL,
    furniture_type_id BIGINT       NOT NULL,
    recorded_on       DATE         NOT NULL,
    image_key         VARCHAR(500) NOT NULL,
    memo              VARCHAR(200) NULL,
    visibility        VARCHAR(20)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_item_image_key UNIQUE (image_key),
    INDEX idx_item_member_type_date (member_id, furniture_type_id, recorded_on),
    CONSTRAINT fk_item_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_item_furniture_type FOREIGN KEY (furniture_type_id) REFERENCES furniture_type (id)
);
