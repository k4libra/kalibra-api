CREATE TABLE IF NOT EXISTS curriculum.generated_exercises (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES curriculum.courses (id),
    subtopic_id UUID NOT NULL,
    origin VARCHAR(20) NOT NULL,
    statement TEXT NOT NULL,
    explanation TEXT NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    verdict VARCHAR(10) NOT NULL,
    correctness_passed BOOLEAN NOT NULL,
    difficulty_passed BOOLEAN NOT NULL,
    rejection_reason VARCHAR(1000),
    used_fallback BOOLEAN NOT NULL,
    generated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_generated_exercises_course ON curriculum.generated_exercises (course_id, generated_at);
CREATE INDEX IF NOT EXISTS idx_generated_exercises_course_subtopic ON curriculum.generated_exercises (course_id, subtopic_id, generated_at);

CREATE TABLE IF NOT EXISTS curriculum.generated_exercise_options (
    exercise_id UUID NOT NULL REFERENCES curriculum.generated_exercises (id),
    option_key VARCHAR(1) NOT NULL,
    text TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    PRIMARY KEY (exercise_id, option_key)
);
