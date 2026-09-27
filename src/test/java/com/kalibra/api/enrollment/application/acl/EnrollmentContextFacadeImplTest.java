package com.kalibra.api.enrollment.application.acl;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetAcceptedInvitationByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentsByCourseQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.shared.contracts.enrollment.EnrollmentLookupRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentContextFacadeImplTest {

    @Mock
    EnrollmentQueryService enrollmentQueryService;

    @Mock
    EnrollmentCommandService enrollmentCommandService;

    @Mock
    InvitationQueryService invitationQueryService;

    @InjectMocks
    EnrollmentContextFacadeImpl facade;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final EnrollmentLookupRequest lookup = new EnrollmentLookupRequest(studentId.value(), courseId.value());
    private final Enrollment enrollment = Enrollment.fromAcceptedInvitation(new EnrollStudentCommand(
            new InvitationId(UUID.randomUUID()), courseId, studentId, new Email("ana@kalibra.pe")));

    @Test
    void shouldFetchTheRosterOfTheCourse() {
        when(enrollmentQueryService.handle(new GetEnrollmentsByCourseQuery(courseId))).thenReturn(List.of(enrollment));

        assertThat(facade.fetchCourseRoster(courseId.value())).singleElement().satisfies(entry -> {
            assertThat(entry.studentId()).isEqualTo(studentId.value());
            assertThat(entry.email()).isEqualTo("ana@kalibra.pe");
        });
    }

    @Test
    void shouldAnswerEnrolledWhenTheEnrollmentExists() {
        when(enrollmentQueryService.handle(new GetEnrollmentByStudentAndCourseQuery(studentId, courseId)))
                .thenReturn(Optional.of(enrollment));

        assertThat(facade.isStudentEnrolled(lookup)).isTrue();
        verify(enrollmentCommandService, never()).handle(any(EnrollStudentCommand.class));
    }

    @Test
    void shouldHealAMissingEnrollmentFromTheAcceptedInvitation() {
        // Arrange
        var invitation = Invitation.send(new SendInvitationCommand("teacher-1", courseId, new Email("ana@kalibra.pe")), studentId);
        invitation.accept(new AcceptInvitationCommand(studentId.value().toString(), invitation.getId()));
        when(enrollmentQueryService.handle(new GetEnrollmentByStudentAndCourseQuery(studentId, courseId))).thenReturn(Optional.empty());
        when(invitationQueryService.handle(new GetAcceptedInvitationByStudentAndCourseQuery(studentId, courseId)))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThat(facade.isStudentEnrolled(lookup)).isTrue();
        verify(enrollmentCommandService).handle(new EnrollStudentCommand(
                invitation.getId(), courseId, studentId, new Email("ana@kalibra.pe")));
    }

    @Test
    void shouldAnswerNotEnrolledWithoutAnAcceptedInvitation() {
        when(enrollmentQueryService.handle(new GetEnrollmentByStudentAndCourseQuery(studentId, courseId))).thenReturn(Optional.empty());
        when(invitationQueryService.handle(new GetAcceptedInvitationByStudentAndCourseQuery(studentId, courseId)))
                .thenReturn(Optional.empty());

        assertThat(facade.isStudentEnrolled(lookup)).isFalse();
    }
}
