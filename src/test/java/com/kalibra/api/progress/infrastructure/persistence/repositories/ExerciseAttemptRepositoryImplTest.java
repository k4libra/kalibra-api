package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.infrastructure.persistence.entities.ExerciseAttemptJpaEntity;
import com.kalibra.api.progress.infrastructure.persistence.transform.ExerciseAttemptJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseAttemptRepositoryImplTest {

    @Mock
    ExerciseAttemptJpaRepository jpaRepository;

    @Mock
    ExerciseAttemptJpaMapper mapper;

    @InjectMocks
    ExerciseAttemptRepositoryImpl repository;

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final ExerciseAttempt attempt = ProgressFixtures.attempt(holderId, courseId, subtopicId, "B", 0.30, 0.52);
    private final ExerciseAttemptJpaEntity entity = new ExerciseAttemptJpaEntity();

    @Test
    void shouldSaveAndFlush() {
        when(mapper.toEntity(attempt)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(attempt);

        assertThat(repository.save(attempt)).isSameAs(attempt);
    }

    @Test
    void shouldFindTheAttemptsOfTheStudentAndOfTheCourse() {
        lenient().when(mapper.toDomain(entity)).thenReturn(attempt);
        when(jpaRepository.findAllByHolderIdAndCourseIdOrderByAnsweredAtDesc(holderId, courseId.value())).thenReturn(List.of(entity));
        when(jpaRepository.findAllByCourseId(courseId.value())).thenReturn(List.of(entity));
        when(jpaRepository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId.value()))
                .thenReturn(Optional.of(entity));
        when(jpaRepository.countByHolderIdAndCourseIdAndResult(holderId, courseId.value(), "CORRECT")).thenReturn(4L);

        assertThat(repository.findAllByHolderIdAndCourseId(holderId, courseId)).containsExactly(attempt);
        assertThat(repository.findAllByCourseId(courseId)).containsExactly(attempt);
        assertThat(repository.findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(holderId, subtopicId)).containsSame(attempt);
        assertThat(repository.countByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.CORRECT)).isEqualTo(4);
    }

    @Test
    void shouldTranslateTheSpringPageIntoTheDomainPageNewestFirst() {
        // Arrange
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        when(mapper.toDomain(entity)).thenReturn(attempt);
        when(jpaRepository.findAllByHolderIdAndCourseId(eq(holderId), eq(courseId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(2, 5), 11));
        when(jpaRepository.findAllByHolderIdAndCourseIdAndResult(eq(holderId), eq(courseId.value()), eq("INCORRECT"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

        // Act
        var page = repository.findAllByHolderIdAndCourseId(holderId, courseId, Pagination.of(2, 5));
        var filtered = repository.findAllByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.INCORRECT, Pagination.of(0, 5));

        // Assert
        assertThat(page.items()).containsExactly(attempt);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(filtered.items()).isEmpty();
        verify(jpaRepository).findAllByHolderIdAndCourseId(eq(holderId), eq(courseId.value()), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("answeredAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
