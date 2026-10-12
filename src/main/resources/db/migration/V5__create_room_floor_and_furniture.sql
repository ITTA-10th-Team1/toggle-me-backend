CREATE TABLE room
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id   BIGINT      NOT NULL,
    name       VARCHAR(50) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- MVP: 사용자당 방 1개. 여러 방을 허용할 때 별도 마이그레이션으로 제거한다.
    CONSTRAINT uk_room_owner UNIQUE (owner_id),
    CONSTRAINT fk_room_owner FOREIGN KEY (owner_id) REFERENCES member (id)
);

CREATE TABLE room_style
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    type             VARCHAR(20)  NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    asset_object_key VARCHAR(500) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE room_floor
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    room_id        BIGINT      NOT NULL,
    wall_style_id  BIGINT      NOT NULL,
    floor_style_id BIGINT      NOT NULL,
    floor_number   INT         NOT NULL,
    name           VARCHAR(50) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_room_floor_room_number UNIQUE (room_id, floor_number),
    CONSTRAINT fk_room_floor_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_room_floor_wall_style FOREIGN KEY (wall_style_id) REFERENCES room_style (id),
    CONSTRAINT fk_room_floor_floor_style FOREIGN KEY (floor_style_id) REFERENCES room_style (id)
);

CREATE TABLE furniture_type
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50) NOT NULL,
    name          VARCHAR(50) NOT NULL,
    display_order INT         NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_furniture_type_code UNIQUE (code)
);

CREATE TABLE furniture_variant
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    furniture_type_id BIGINT       NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    asset_object_key  VARCHAR(500) NOT NULL,
    width_units       INT          NOT NULL,
    height_units      INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_furniture_variant_type (furniture_type_id),
    CONSTRAINT fk_furniture_variant_type FOREIGN KEY (furniture_type_id) REFERENCES furniture_type (id)
);

CREATE TABLE room_furniture
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    floor_id     BIGINT         NOT NULL,
    variant_id   BIGINT         NOT NULL,
    position_x   DECIMAL(10, 4) NOT NULL,
    position_y   DECIMAL(10, 4) NOT NULL,
    rotation     DECIMAL(10, 4) NOT NULL,
    render_order INT            NOT NULL,
    active       BOOLEAN        NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_room_furniture_floor (floor_id),
    CONSTRAINT fk_room_furniture_floor FOREIGN KEY (floor_id) REFERENCES room_floor (id),
    CONSTRAINT fk_room_furniture_variant FOREIGN KEY (variant_id) REFERENCES furniture_variant (id)
);
