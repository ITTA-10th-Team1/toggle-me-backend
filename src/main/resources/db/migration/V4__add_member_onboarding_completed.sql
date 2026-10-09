ALTER TABLE member
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE AFTER status;
