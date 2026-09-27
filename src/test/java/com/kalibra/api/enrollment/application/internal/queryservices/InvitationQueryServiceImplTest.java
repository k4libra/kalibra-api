package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationValidity;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.InvitationRepository;
import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationQueryServiceImplTest {

    @Mock
    InvitationRepository invitationRepository;

    @Mock
    ExternalIamService externalIamService;

    @Mock
    ExternalCurriculumService externalCurriculumService;

    @InjectMocks
    InvitationQueryServiceImpl service;

    private final UUID teacherId = UUID.randomUUID();
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final CourseSummary algebra = new CourseSummary(UUID.randomUUID(), "Algebra", "MAT101", teacherId.toString());
    private final CourseSummary physics = new CourseSummary(UUID.randomUUID(), "Physics", "FIS101", teacherId.toString());

    private Invitation invitationTo(CourseSummary course) {
        return Invitation.send(new SendInvitationCommand(teacherId.toString(), new CourseId(course.courseId()),
                new Email("ana@kalibra.pe")), studentId);
    }

    @Test
    void shouldListThePendingInvitationsOfTheStudentWithCourseAndTeacher() {
        // Arrange
        var invitation = invitationTo(algebra);
        when(invitationRepository.findAllByStudentIdAndStatus(studentId, InvitationStatus.PENDING)).thenReturn(List.of(invitation));
        when(externalCurriculumService.fetchCourseSummary(invitation.getCourseId())).thenReturn(Optional.of(algebra));
        when(externalIamService.fetchUserEmail(teacherId.toString())).thenReturn(Optional.of("teacher@kalibra.pe"));

        // Act
        var views = service.handle(new GetPendingInvitationsByHolderIdQuery(studentId.value().toString()));

        // Assert
        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.invitationId()).isEqualTo(invitation.getId().value());
            assertThat(view.courseName()).isEqualTo("Algebra");
            assertThat(view.teacherEmail()).isEqualTo("teacher@kalibra.pe");
            assertThat(view.sentAt()).isEqualTo(invitation.getValidity().sentAt());
        });
    }

    @Test
    void shouldNotOfferAPendingInvitationPastItsValidity() {
        var overdue = invitationTo(algebra);
        overdue.setValidity(InvitationValidity.threeDaysFrom(Instant.now().minus(Duration.ofDays(4))));
        when(invitationRepository.findAllByStudentIdAndStatus(studentId, InvitationStatus.PENDING)).thenReturn(List.of(overdue));

        assertThat(service.handle(new GetPendingInvitationsByHolderIdQuery(studentId.value().toString()))).isEmpty();
    }

    @Test
    void shouldGroupTheSentInvitationsByCourseIncludingCoursesWithoutInvitations() {
        // Arrange
        var invitation = invitationTo(algebra);
        when(invitationRepository.findAllByHolderId(teacherId.toString())).thenReturn(List.of(invitation));
        when(externalCurriculumService.fetchCoursesByHolderId(teacherId.toString())).thenReturn(List.of(algebra, physics));

        // Act
        var groups = service.handle(new GetSentInvitationsByHolderIdQuery(teacherId.toString()));

        // Assert
        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).courseCode()).isEqualTo("MAT101");
        assertThat(groups.get(0).invitations()).singleElement().satisfies(line -> {
            assertThat(line.invitedEmail()).isEqualTo("ana@kalibra.pe");
            assertThat(line.status()).isEqualTo(InvitationStatus.PENDING);
            assertThat(line.sentAt()).isEqualTo(invitation.getValidity().sentAt());
            assertThat(line.expiresAt()).isEqualTo(invitation.getValidity().expiresAt());
        });
        assertThat(groups.get(1).invitations()).isEmpty();
    }
}
