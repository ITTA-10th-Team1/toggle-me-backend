ALTER TABLE refresh_token
    ADD COLUMN session_id VARCHAR(36) NULL AFTER member_id;

UPDATE refresh_token
SET session_id = UUID()
WHERE session_id IS NULL;

ALTER TABLE refresh_token
    MODIFY COLUMN session_id VARCHAR(36) NOT NULL;

CREATE INDEX idx_refresh_token_session_id ON refresh_token (session_id);
