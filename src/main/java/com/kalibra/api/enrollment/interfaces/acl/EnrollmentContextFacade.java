package com.kalibra.api.enrollment.interfaces.acl;

import com.kalibra.api.shared.contracts.enrollment.EnrollmentLookupRequest;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;

import java.util.List;
import java.util.UUID;

public interface EnrollmentContextFacade {

    List<RosterEntry> fetchCourseRoster(UUID courseId);

    boolean isStudentEnrolled(EnrollmentLookupRequest request);
}