package com.kalibra.api.enrollment.infrastructure.persistence.repositories;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.infrastructure.persistence.entities.InvitationJpaEntity;
import com.kalibra.api.enrollment.infrastructure.persistence.transform.InvitationJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationRepositoryImplTest {

    @Mock
    InvitationJpaRepository jpaRepository;

    @Mock
    InvitationJpaMapper mapper;

    @InjectMocks
    InvitationRepositoryImpl repository;

    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final Invitation invitation = Invitation.send(new SendInvitationCommand(
            "teacher-1", new CourseId(UUID.randomUUID()), new Email("ana@kalibra.pe")), studentId);
    private final InvitationJpaEntity entity = new InvitationJpaEntity();

    @Test
    void shouldSaveAndFlushSoUniqueIndexesFailInsideTheTransaction() {
        when(mapper.toEntity(invitation)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(invitation);

        assertThat(repository.save(invitation)).isSameAs(invitation);
    }

    @Test
    void shouldFindByIdScopedToTheTeacher() {
        when(jpaRepository.findByIdAndHolderId(invitation.getId().value(), "teacher-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(invitation);

        assertThat(repository.findByIdAndHolderId(invitation.getId(), "teacher-1")).contains(invitation);
    }

    @Test
    void shouldFindByIdScopedToTheStudent() {
        when(jpaRepository.findByIdAndStudentId(invitation.getId().value(), studentId.value())).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndStudentId(invitation.getId(), studentId)).isEmpty();
    }

    @Test
    void shouldQueryTheStatusAsItsName() {
        var now = Instant.now();
        when(jpaRepository.findAllByStatusAndExpiresAtBefore("PENDING", now)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(invitation);

        assertThat(repository.findAllByStatusAndExpiresAtBefore(InvitationStatus.PENDING, now)).containsExactly(invitation);
    }

    @Test
    void shouldFindThePendingInvitationsOfTheStudent() {
        when(jpaRepository.findAllByStudentIdAndStatus(studentId.value(), "PENDING")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(invitation);

        assertThat(repository.findAllByStudentIdAndStatus(studentId, InvitationStatus.PENDING)).containsExactly(invitation);
    }
}
