CREATE TABLE IF NOT EXISTS progress.exercise_attempts (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    course_id UUID NOT NULL,
    subtopic_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    exercise_statement TEXT NOT NULL,
    selected_option_key VARCHAR(1) NOT NULL,
    result VARCHAR(10) NOT NULL,
    feedback_explanation TEXT,
    previous_probability DOUBLE PRECISION,
    current_probability DOUBLE PRECISION NOT NULL,
    answered_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exercise_attempts_holder_course ON progress.exercise_attempts (holder_id, course_id, answered_at);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_holder_subtopic ON progress.exercise_attempts (holder_id, subtopic_id, answered_at);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_course ON progress.exercise_attempts (course_id);

CREATE TABLE IF NOT EXISTS progress.subtopic_masteries (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    course_id UUID NOT NULL,
    subtopic_id UUID NOT NULL,
    initial_estimate DOUBLE PRECISION NOT NULL,
    current_estimate DOUBLE PRECISION NOT NULL,
    level VARCHAR(10) NOT NULL,
    estimates_count INT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_subtopic_masteries_holder_subtopic UNIQUE (holder_id, subtopic_id)
);

CREATE INDEX IF NOT EXISTS idx_subtopic_masteries_holder_course ON progress.subtopic_masteries (holder_id, course_id);
CREATE INDEX IF NOT EXISTS idx_subtopic_masteries_course ON progress.subtopic_masteries (course_id);
