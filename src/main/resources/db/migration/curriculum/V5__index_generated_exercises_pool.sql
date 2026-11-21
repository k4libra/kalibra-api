CREATE INDEX idx_generated_exercises_pool
    ON curriculum.generated_exercises (course_id, subtopic_id, difficulty, verdict);
