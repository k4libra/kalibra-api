package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.GeneratedExerciseJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.GeneratedExerciseJpaMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedExerciseRepositoryImplTest {

    @Mock
    GeneratedExerciseJpaRepository jpaRepository;

    @Mock
    GeneratedExerciseJpaMapper mapper;

    @InjectMocks
    GeneratedExerciseRepositoryImpl repository;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations")));
    private final SubtopicId subtopicId = course.getSubtopics().getFirst().getId();
    private final GeneratedExercise exercise = GeneratedExerciseFixtures.approved(course, subtopicId);
    private final GeneratedExerciseJpaEntity entity = new GeneratedExerciseJpaEntity();

    @Test
    void shouldSaveOneAndSeveralExercises() {
        when(mapper.toEntity(exercise)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(exercise);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(jpaRepository.saveAll(List.of(entity))).thenReturn(List.of(entity));

        assertThat(repository.save(exercise)).isSameAs(exercise);
        assertThat(repository.saveAll(List.of(exercise))).containsExactly(exercise);
    }

    @Test
    void shouldFindByIdAndByCourse() {
        when(mapper.toDomain(entity)).thenReturn(exercise);
        when(jpaRepository.findById(exercise.getId().value())).thenReturn(Optional.of(entity));
        when(jpaRepository.findAllByCourseId(course.getId().value())).thenReturn(List.of(entity));
        when(jpaRepository.findAllByCourseIdIn(List.of(course.getId().value()))).thenReturn(List.of(entity));

        assertThat(repository.findById(exercise.getId())).containsSame(exercise);
        assertThat(repository.findAllByCourseId(course.getId())).containsExactly(exercise);
        assertThat(repository.findAllByCourseIdIn(List.of(course.getId()))).containsExactly(exercise);
    }

    @Test
    void shouldNotQueryWhenThereAreNoCourses() {
        assertThat(repository.findAllByCourseIdIn(List.of())).isEmpty();
        verifyNoInteractions(jpaRepository);
    }

    @Test
    void shouldTranslateTheSpringPageIntoTheDomainPageNewestFirst() {
        // Arrange
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        when(mapper.toDomain(entity)).thenReturn(exercise);
        when(jpaRepository.findAllByCourseId(eq(course.getId().value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 10), 11));
        when(jpaRepository.findAllByCourseIdAndSubtopicId(eq(course.getId().value()), eq(subtopicId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // Act
        var page = repository.findAllByCourseId(course.getId(), Pagination.of(1, 10));
        var filtered = repository.findAllByCourseIdAndSubtopicId(course.getId(), subtopicId, Pagination.of(0, 10));

        // Assert
        assertThat(page.items()).containsExactly(exercise);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(filtered.items()).isEmpty();
        verify(jpaRepository).findAllByCourseId(eq(course.getId().value()), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("generatedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
