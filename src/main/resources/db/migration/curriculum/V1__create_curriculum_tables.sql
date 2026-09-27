CREATE TABLE IF NOT EXISTS curriculum.courses (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_courses_holder_id ON curriculum.courses (holder_id);

CREATE TABLE IF NOT EXISTS curriculum.course_subtopics (
    course_id UUID NOT NULL REFERENCES curriculum.courses (id),
    subtopic_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (course_id, subtopic_id)
);

CREATE TABLE IF NOT EXISTS curriculum.curricular_materials (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES curriculum.courses (id),
    file_name VARCHAR(255) NOT NULL,
    format VARCHAR(10) NOT NULL,
    storage_reference VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    normalized_text TEXT,
    page_count INT,
    failure_reason VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_curricular_materials_course ON curriculum.curricular_materials (course_id, uploaded_at);
CREATE INDEX IF NOT EXISTS idx_curricular_materials_status ON curriculum.curricular_materials (status);

CREATE TABLE IF NOT EXISTS curriculum.curricular_material_subtopics (
    material_id UUID NOT NULL REFERENCES curriculum.curricular_materials (id),
    subtopic_id UUID NOT NULL,
    PRIMARY KEY (material_id, subtopic_id)
);

CREATE TABLE IF NOT EXISTS curriculum.teacher_workspaces (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL UNIQUE,
    active_course_id UUID REFERENCES curriculum.courses (id)
);
