CREATE TABLE member
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    handle             VARCHAR(30)  NOT NULL,
    nickname           VARCHAR(30)  NOT NULL,
    profile_object_key VARCHAR(500) NULL,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_handle UNIQUE (handle)
);

CREATE TABLE social_account
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_account_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_account_member FOREIGN KEY (member_id) REFERENCES member (id)
);
