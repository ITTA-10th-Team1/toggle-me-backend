-- 같은 테이블로 가입 인증(SIGNUP)과 비밀번호 재설정(PASSWORD_RESET) 코드를 구분한다. 기존 행은 가입 인증이다
ALTER TABLE email_verification ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'SIGNUP' AFTER email;
ALTER TABLE email_verification ADD INDEX idx_email_verification_email_purpose_id (email, purpose, id);
