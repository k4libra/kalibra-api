package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentQueryServiceImplTest {

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    ExternalCurriculumService externalCurriculumService;

    @InjectMocks
    EnrollmentQueryServiceImpl service;

    @Test
    void shouldGroupTheStudentsByCourseIncludingCoursesWithoutStudents() {
        // Arrange
        var algebra = new CourseSummary(UUID.randomUUID(), "Algebra", "MAT101", "teacher-1");
        var physics = new CourseSummary(UUID.randomUUID(), "Physics", "FIS101", "teacher-1");
        var enrollment = Enrollment.fromAcceptedInvitation(new EnrollStudentCommand(new InvitationId(UUID.randomUUID()),
                new CourseId(algebra.courseId()), new StudentId(UUID.randomUUID()), new Email("ana@kalibra.pe")));
        when(externalCurriculumService.fetchCoursesByHolderId("teacher-1")).thenReturn(List.of(algebra, physics));
        when(enrollmentRepository.findAllByCourseIdIn(List.of(new CourseId(algebra.courseId()), new CourseId(physics.courseId()))))
                .thenReturn(List.of(enrollment));

        // Act
        var rosters = service.handle(new GetEnrollmentRostersByHolderIdQuery("teacher-1"));

        // Assert
        assertThat(rosters).hasSize(2);
        assertThat(rosters.get(0).courseName()).isEqualTo("Algebra");
        assertThat(rosters.get(0).enrolledCount()).isEqualTo(1);
        assertThat(rosters.get(0).students()).singleElement()
                .satisfies(line -> assertThat(line.email()).isEqualTo("ana@kalibra.pe"));
        assertThat(rosters.get(1).enrolledCount()).isZero();
        assertThat(rosters.get(1).students()).isEmpty();
    }
}
