ALTER TABLE progress.exercise_attempts
    ADD COLUMN time_spent_seconds INTEGER CHECK (time_spent_seconds BETWEEN 0 AND 3600),
    ADD COLUMN explanation_steps JSONB,
    ADD COLUMN key_idea TEXT,
    ADD COLUMN correct_option_key VARCHAR(1),
    ADD COLUMN options JSONB;
