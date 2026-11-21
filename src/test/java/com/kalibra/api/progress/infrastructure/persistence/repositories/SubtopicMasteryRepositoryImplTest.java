package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.infrastructure.persistence.entities.SubtopicMasteryJpaEntity;
import com.kalibra.api.progress.infrastructure.persistence.transform.SubtopicMasteryJpaMapper;
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
class SubtopicMasteryRepositoryImplTest {

    @Mock
    SubtopicMasteryJpaRepository jpaRepository;

    @Mock
    SubtopicMasteryJpaMapper mapper;

    @InjectMocks
    SubtopicMasteryRepositoryImpl repository;

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final SubtopicMastery mastery = ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.52, MasteryLevel.MEDIUM);
    private final SubtopicMasteryJpaEntity entity = new SubtopicMasteryJpaEntity();

    @Test
    void shouldSaveAndFlushSoTheUniqueConstraintFailsInsideTheTransaction() {
        when(mapper.toEntity(mastery)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(mastery);

        assertThat(repository.save(mastery)).isSameAs(mastery);
    }

    @Test
    void shouldFindTheMasteriesOfTheStudentAndOfTheCourse() {
        when(mapper.toDomain(entity)).thenReturn(mastery);
        when(jpaRepository.findByHolderIdAndSubtopicId(holderId, subtopicId.value())).thenReturn(Optional.of(entity));
        when(jpaRepository.findAllByHolderIdAndCourseId(holderId, courseId.value())).thenReturn(List.of(entity));
        when(jpaRepository.findAllByCourseId(courseId.value())).thenReturn(List.of(entity));

        assertThat(repository.findByHolderIdAndSubtopicId(holderId, subtopicId)).containsSame(mastery);
        assertThat(repository.findAllByHolderIdAndCourseId(holderId, courseId)).containsExactly(mastery);
        assertThat(repository.findAllByCourseId(courseId)).containsExactly(mastery);
    }
}
