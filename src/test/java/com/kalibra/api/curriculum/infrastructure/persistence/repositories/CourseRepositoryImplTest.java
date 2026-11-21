package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.CourseJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.CourseJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRepositoryImplTest {

    @Mock
    CourseJpaRepository jpaRepository;

    @Mock
    CourseJpaMapper mapper;

    @InjectMocks
    CourseRepositoryImpl repository;

    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));

    @Test
    void shouldFindByIdScopedToTheTeacher() {
        var entity = new CourseJpaEntity();
        when(jpaRepository.findByIdAndHolderId(course.getId().value(), "teacher-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(course);

        assertThat(repository.findByIdAndHolderId(course.getId(), "teacher-1")).contains(course);
    }

    @Test
    void shouldReturnEmptyWhenTheCourseBelongsToAnotherTeacher() {
        when(jpaRepository.findByIdAndHolderId(course.getId().value(), "teacher-2")).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndHolderId(course.getId(), "teacher-2")).isEmpty();
    }

    @Test
    void shouldListTheCoursesOfTheTeacher() {
        var entity = new CourseJpaEntity();
        when(jpaRepository.findAllByHolderId("teacher-1")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(course);

        assertThat(repository.findAllByHolderId("teacher-1")).containsExactly(course);
    }
}
