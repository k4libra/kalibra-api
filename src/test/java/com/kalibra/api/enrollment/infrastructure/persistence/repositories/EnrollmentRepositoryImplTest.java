package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.EnrollmentJpaEntity;
import com.kalibra.api.enrollment.infrastructure.persistence.transform.EnrollmentJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentRepositoryImplTest {

    @Mock
    EnrollmentJpaRepository jpaRepository;

    @Mock
    EnrollmentJpaMapper mapper;

    @InjectMocks
    EnrollmentRepositoryImpl repository;

    private final Enrollment enrollment = Enrollment.fromAcceptedInvitation(new EnrollStudentCommand(
            new InvitationId(UUID.randomUUID()), new CourseId(UUID.randomUUID()),
            new StudentId(UUID.randomUUID()), new Email("ana@kalibra.pe")));
    private final EnrollmentJpaEntity entity = new EnrollmentJpaEntity();

    @Test
    void shouldSaveAndFlush() {
        when(mapper.toEntity(enrollment)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(enrollment);

        assertThat(repository.save(enrollment)).isSameAs(enrollment);
    }

    @Test
    void shouldFindTheEnrollmentsOfSeveralCourses() {
        var courseId = enrollment.getCourseId();
        when(jpaRepository.findAllByCourseIdIn(List.of(courseId.value()))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(enrollment);

        assertThat(repository.findAllByCourseIdIn(List.of(courseId))).containsExactly(enrollment);
    }

    @Test
    void shouldFindTheEnrollmentOfTheStudentInTheCourse() {
        when(jpaRepository.findByStudentIdAndCourseId(enrollment.getStudentId().value(), enrollment.getCourseId().value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(enrollment);

        assertThat(repository.findByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId())).contains(enrollment);
    }
}
