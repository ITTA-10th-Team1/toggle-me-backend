CREATE TABLE local_account
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_local_account_email UNIQUE (email),
    CONSTRAINT uk_local_account_member UNIQUE (member_id),
    CONSTRAINT fk_local_account_member FOREIGN KEY (member_id) REFERENCES member (id)
);
