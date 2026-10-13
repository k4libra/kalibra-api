package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetLockedSubtopicIdsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseQueryServiceImplTest {

    @Mock
    CourseRepository courseRepository;

    @InjectMocks
    CourseQueryServiceImpl service;

    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));

    @Test
    void shouldReturnOnlyTheCoursesOfTheTeacher() {
        when(courseRepository.findAllByHolderId("teacher-1")).thenReturn(List.of(course));

        assertThat(service.handle(new GetCoursesByHolderIdQuery("teacher-1"))).containsExactly(course);
    }

    @Test
    void shouldReturnCourseById() {
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

        assertThat(service.handle(new GetCourseByIdQuery(course.getId()))).contains(course);
    }

    @Test
    void shouldReturnEmptyWhenCourseDoesNotExist() {
        var id = new CourseId(UUID.randomUUID());
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.handle(new GetCourseByIdQuery(id))).isEmpty();
    }

    @Mock
    CurricularMaterialRepository curricularMaterialRepository;

    @Test
    void shouldResolveAllLockedIdsWithOneMaterialQueryPerCourse() {
        var locked = Set.of(course.getSubtopics().getFirst().getId());
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.findMaterialSubtopicIdsByCourseId(course.getId())).thenReturn(locked);
        assertThat(service.handle(new GetLockedSubtopicIdsByCourseQuery(course.getId(), "teacher-1"))).isEqualTo(locked);
        verify(curricularMaterialRepository).findMaterialSubtopicIdsByCourseId(course.getId());
    }

    @Test
    void shouldNotResolveLockedIdsForAnotherTeachersCourse() {
        assertThatThrownBy(() -> service.handle(new GetLockedSubtopicIdsByCourseQuery(course.getId(), "teacher-2")))
                .isInstanceOf(CourseNotFoundException.class);
        verifyNoInteractions(curricularMaterialRepository);
    }
}
