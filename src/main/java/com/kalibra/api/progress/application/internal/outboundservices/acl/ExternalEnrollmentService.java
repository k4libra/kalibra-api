package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalEnrollmentService {

    public boolean isEnrolled(
            StudentId studentId,
            CourseId courseId) {
        return false;
    }

    public List<StudentId> fetchRoster(CourseId courseId) {
        return List.of();
    }
}