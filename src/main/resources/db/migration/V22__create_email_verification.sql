CREATE TABLE email_verification
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NOT NULL,
    code_hash         VARCHAR(64)  NOT NULL,
    expires_at        DATETIME(6)  NOT NULL,
    failed_attempts   INT          NOT NULL DEFAULT 0,
    verified_at       DATETIME(6)  NULL,
    token_hash        VARCHAR(64)  NULL,
    token_expires_at  DATETIME(6)  NULL,
    consumed_at       DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_token_hash UNIQUE (token_hash),
    INDEX idx_email_verification_email_id (email, id)
);

-- 기존 계정은 인증하지 않았으므로 NULL(미인증)로 둔다
ALTER TABLE local_account ADD COLUMN email_verified_at DATETIME(6) NULL AFTER email;
