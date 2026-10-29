ALTER TABLE local_account RENAME COLUMN email TO login_id;
ALTER TABLE local_account RENAME INDEX uk_local_account_email TO uk_local_account_login_id;

ALTER TABLE local_account ADD COLUMN email VARCHAR(255) NULL AFTER login_id;
UPDATE local_account SET email = login_id;
ALTER TABLE local_account MODIFY COLUMN email VARCHAR(255) NOT NULL;
ALTER TABLE local_account ADD CONSTRAINT uk_local_account_email UNIQUE (email);
