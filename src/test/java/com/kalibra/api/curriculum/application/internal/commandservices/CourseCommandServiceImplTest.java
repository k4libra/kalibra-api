package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.events.CourseCreated;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceImplTest {

    @Mock
    CourseRepository courseRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CourseCommandServiceImpl service;

    @Test
    void shouldSaveCourseAndPublishCourseCreated() {
        // Arrange
        var command = new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations"));
        var eventCaptor = ArgumentCaptor.forClass(CourseCreated.class);
        when(courseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var course = service.handle(command);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().courseId()).isEqualTo(course.getId().value());
        assertThat(eventCaptor.getValue().holderId()).isEqualTo("teacher-1");
    }

    @Test
    void shouldNotSaveACourseWithoutSubtopics() {
        var command = new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of());

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(CourseWithoutSubtopicsException.class);
        verify(courseRepository, never()).save(any());
    }
}
