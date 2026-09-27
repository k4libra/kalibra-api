package com.kalibra.api.enrollment.application.acl;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetAcceptedInvitationByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentsByCourseQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.acl.EnrollmentContextFacade;
import com.kalibra.api.shared.contracts.enrollment.EnrollmentLookupRequest;
import com.kalibra.api.shared.contracts.enrollment.RosterEntry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentContextFacadeImpl implements EnrollmentContextFacade {

    private final EnrollmentQueryService enrollmentQueryService;
    private final EnrollmentCommandService enrollmentCommandService;
    private final InvitationQueryService invitationQueryService;

    public EnrollmentContextFacadeImpl(
            EnrollmentQueryService enrollmentQueryService,
            EnrollmentCommandService enrollmentCommandService,
            InvitationQueryService invitationQueryService
    ) {
        this.enrollmentQueryService = enrollmentQueryService;
        this.enrollmentCommandService = enrollmentCommandService;
        this.invitationQueryService = invitationQueryService;
    }

    @Override
    public List<RosterEntry> fetchCourseRoster(UUID courseId) {
        return enrollmentQueryService
                .handle(new GetEnrollmentsByCourseQuery(new CourseId(courseId)))
                .stream()
                .map(enrollment -> new RosterEntry(
                        enrollment.getStudentId().value(),
                        enrollment.getStudentEmail().value(),
                        enrollment.getEnrolledAt()
                ))
                .toList();
    }

    @Override
    public boolean isStudentEnrolled(EnrollmentLookupRequest request) {
        var studentId = new StudentId(request.studentId());
        var courseId = new CourseId(request.courseId());

        var enrollment = enrollmentQueryService.handle(
                new GetEnrollmentByStudentAndCourseQuery(studentId, courseId)
        );

        if (enrollment.isPresent()) {
            return true;
        }

        var invitation = invitationQueryService.handle(
                new GetAcceptedInvitationByStudentAndCourseQuery(
                        studentId,
                        courseId
                )
        );

        if (invitation.isEmpty()) {
            return false;
        }

        var accepted = invitation.get();

        enrollmentCommandService.handle(
                new EnrollStudentCommand(
                        accepted.getId(),
                        accepted.getCourseId(),
                        accepted.getStudentId(),
                        accepted.getInvitedEmail()
                )
        );

        return true;
    }
}