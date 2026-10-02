package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.enrollment.interfaces.acl.EnrollmentContextFacade;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.shared.contracts.enrollment.EnrollmentLookupRequest;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalEnrollmentService {

    private final EnrollmentContextFacade enrollmentContextFacade;

    public ExternalEnrollmentService(
            EnrollmentContextFacade enrollmentContextFacade) {
        this.enrollmentContextFacade = enrollmentContextFacade;
    }

    public boolean isEnrolled(
            StudentId studentId,
            CourseId courseId) {
        var request = new EnrollmentLookupRequest(
                studentId.value(),
                courseId.value()
        );
        return enrollmentContextFacade.isStudentEnrolled(request);
    }

    public List<StudentId> fetchRoster(CourseId courseId) {
        return fetchRosterEntries(courseId)
                .stream()
                .map(entry -> new StudentId(entry.studentId()))
                .toList();
    }

    public List<RosterEntry> fetchRosterEntries(CourseId courseId) {
        return enrollmentContextFacade.fetchCourseRoster(courseId.value());
    }
}
