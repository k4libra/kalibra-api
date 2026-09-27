CREATE TABLE IF NOT EXISTS iam.student_preferences (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL UNIQUE,
    daily_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_reminder_time TIME,
    dark_mode BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_student_preferences_reminder_time
    ON iam.student_preferences (daily_reminder_time)
    WHERE daily_reminder_enabled;
