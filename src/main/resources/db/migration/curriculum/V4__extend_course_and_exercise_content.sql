ALTER TABLE curriculum.courses
    ADD COLUMN term TEXT,
    ADD COLUMN faculty TEXT,
    ADD COLUMN semester TEXT;

ALTER TABLE curriculum.course_subtopics ADD COLUMN description VARCHAR(280);

ALTER TABLE curriculum.generated_exercises
    ADD COLUMN explanation_steps JSONB,
    ADD COLUMN key_idea TEXT;
