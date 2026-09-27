CREATE TABLE IF NOT EXISTS enrollment.invitations (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    holder_id VARCHAR(64) NOT NULL,
    student_id UUID NOT NULL,
    invited_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invitations_holder_id ON enrollment.invitations (holder_id);
CREATE INDEX IF NOT EXISTS idx_invitations_student_status ON enrollment.invitations (student_id, status);
CREATE INDEX IF NOT EXISTS idx_invitations_status_expires_at ON enrollment.invitations (status, expires_at);

-- a student has at most one pending invitation per course
CREATE UNIQUE INDEX IF NOT EXISTS uq_invitations_pending_student_course
    ON enrollment.invitations (course_id, student_id)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS enrollment.enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    student_id UUID NOT NULL,
    student_email VARCHAR(320) NOT NULL,
    invitation_id UUID NOT NULL UNIQUE REFERENCES enrollment.invitations (id),
    enrolled_at TIMESTAMP NOT NULL,
    UNIQUE (course_id, student_id)
);
