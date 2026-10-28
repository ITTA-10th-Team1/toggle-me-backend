ALTER TABLE member
    ADD COLUMN profile_completed BOOLEAN NOT NULL DEFAULT FALSE AFTER onboarding_completed;
