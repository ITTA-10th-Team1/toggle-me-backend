CREATE TABLE image_analysis
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    image_key  VARCHAR(500) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    result     JSON         NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_image_analysis_member FOREIGN KEY (member_id) REFERENCES member (id)
);
