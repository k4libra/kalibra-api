package com.kalibra.api.progress.domain.model.queries;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;

public record GetStudentProgressForTeacherQuery(
        String holderId,
        CourseId courseId,
        StudentId studentId
) {
}
