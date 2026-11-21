package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.events.StudentEnrolled;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentCommandServiceImplTest {

    @Mock
    EnrollmentRepository enrollmentRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PlatformTransactionManager transactionManager;

    EnrollmentCommandServiceImpl service;

    private final EnrollStudentCommand command = new EnrollStudentCommand(new InvitationId(UUID.randomUUID()),
            new CourseId(UUID.randomUUID()), new StudentId(UUID.randomUUID()), new Email("ana@kalibra.pe"));

    @BeforeEach
    void setUp() {
        service = new EnrollmentCommandServiceImpl(enrollmentRepository, eventPublisher, transactionManager);
    }

    @Test
    void shouldEnrollTheStudentAndPublishIt() {
        when(enrollmentRepository.findByStudentIdAndCourseId(command.studentId(), command.courseId())).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var enrollment = service.handle(command);

        assertThat(enrollment.getStudentId()).isEqualTo(command.studentId());
        verify(eventPublisher).publishEvent(any(StudentEnrolled.class));
    }

    @Test
    void shouldReturnTheExistingEnrollmentWithoutEnrollingTwice() {
        var existing = Enrollment.fromAcceptedInvitation(command);
        when(enrollmentRepository.findByStudentIdAndCourseId(command.studentId(), command.courseId())).thenReturn(Optional.of(existing));

        assertThat(service.handle(command)).isSameAs(existing);
        verify(enrollmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturnTheEnrollmentSavedByAConcurrentEnrollment() {
        var concurrent = Enrollment.fromAcceptedInvitation(command);
        when(enrollmentRepository.findByStudentIdAndCourseId(command.studentId(), command.courseId()))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(enrollmentRepository.save(any())).thenThrow(new DataIntegrityViolationException("enrollments_course_id_student_id_key"));

        assertThat(service.handle(command)).isSameAs(concurrent);
    }
}
