package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRemovalException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRenameException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.events.CourseCreated;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    CurricularMaterialRepository curricularMaterialRepository;

    private Course editableCourse() {
        return Course.create(new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations", "Functions")));
    }

    @Test
    void shouldResolveLockedRenameFromAllMaterialsBeforeSaving() {
        var course = editableCourse();
        var id = course.getSubtopics().getFirst().getId();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.existsByCourseIdAndSubtopicId(course.getId(), id)).thenReturn(true);
        var command = new UpdateSubtopicCommand(course.getId(), "teacher-1", id, Optional.of("Renamed"), Optional.empty(), Optional.empty());
        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(SubtopicLockedForRenameException.class);
        verify(curricularMaterialRepository).existsByCourseIdAndSubtopicId(course.getId(), id);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void shouldRejectRemovalWhenMaterialExists() {
        var course = editableCourse();
        var id = course.getSubtopics().getFirst().getId();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.existsByCourseIdAndSubtopicId(course.getId(), id)).thenReturn(true);
        assertThatThrownBy(() -> service.handle(new RemoveSubtopicCommand(course.getId(), "teacher-1", id)))
                .isInstanceOf(SubtopicLockedForRemovalException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void shouldAllowArchivingLockedSubtopic() {
        var course = editableCourse();
        var id = course.getSubtopics().getFirst().getId();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.existsByCourseIdAndSubtopicId(course.getId(), id)).thenReturn(true);
        when(courseRepository.save(course)).thenReturn(course);
        var updated = service.handle(new UpdateSubtopicCommand(course.getId(), "teacher-1", id, Optional.empty(), Optional.empty(), Optional.of(SubtopicStatus.ARCHIVED)));
        assertThat(updated.getSubtopics().getFirst().isActive()).isFalse();
        verify(courseRepository).save(course);
    }

    @Test
    void shouldSaveCourseRenameAndSubtopicAdditionWithoutMaterialLookup() {
        var course = editableCourse();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        service.handle(new UpdateCourseCommand(course.getId(), "teacher-1", Optional.of("Geometry"), Optional.empty()));
        service.handle(new AddSubtopicCommand(course.getId(), "teacher-1", "Limits"));
        assertThat(course.getName()).isEqualTo("Geometry");
        assertThat(course.getSubtopics()).hasSize(3);
        verifyNoInteractions(curricularMaterialRepository);
    }

    @Test
    void shouldRemoveUnlockedSubtopicAndSaveContiguousOrder() {
        var course = editableCourse();
        var id = course.getSubtopics().getFirst().getId();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        service.handle(new RemoveSubtopicCommand(course.getId(), "teacher-1", id));
        assertThat(course.getSubtopics()).hasSize(1);
        assertThat(course.getSubtopics().getFirst().getDisplayOrder()).isEqualTo(1);
        verify(curricularMaterialRepository).existsByCourseIdAndSubtopicId(course.getId(), id);
        verify(courseRepository).save(course);
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "add", "update", "remove"})
    void shouldHideOtherTeachersCoursesBeforeLookingUpMaterials(String operation) {
        var course = editableCourse();
        var id = course.getSubtopics().getFirst().getId();
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-2")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> {
            switch (operation) {
                case "course" -> service.handle(new UpdateCourseCommand(course.getId(), "teacher-2", Optional.of("Name"), Optional.empty()));
                case "add" -> service.handle(new AddSubtopicCommand(course.getId(), "teacher-2", "Name"));
                case "update" -> service.handle(new UpdateSubtopicCommand(course.getId(), "teacher-2", id, Optional.empty(), Optional.empty(), Optional.of(SubtopicStatus.ARCHIVED)));
                case "remove" -> service.handle(new RemoveSubtopicCommand(course.getId(), "teacher-2", id));
                default -> throw new AssertionError(operation);
            }
        }).isInstanceOf(CourseNotFoundException.class);
        verifyNoInteractions(curricularMaterialRepository);
        verify(courseRepository, never()).save(any());
    }
}
