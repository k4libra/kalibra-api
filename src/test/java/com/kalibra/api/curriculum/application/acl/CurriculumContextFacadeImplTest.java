package com.kalibra.api.curriculum.application.acl;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumContextFacadeImplTest {

    @Mock
    CourseQueryService courseQueryService;

    @InjectMocks
    CurriculumContextFacadeImpl facade;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));

    @Test
    void shouldTellWhetherTheCourseBelongsToTheTeacher() {
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.of(course));

        assertThat(facade.isCourseOwnedBy(course.getId().value(), "teacher-1")).isTrue();
        assertThat(facade.isCourseOwnedBy(course.getId().value(), "teacher-2")).isFalse();
    }

    @Test
    void shouldAnswerFalseAndEmptyForAnUnknownCourse() {
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.empty());
        var unknown = UUID.randomUUID();

        assertThat(facade.isCourseOwnedBy(unknown, "teacher-1")).isFalse();
        assertThat(facade.fetchCourseSummary(unknown)).isEmpty();
        assertThat(facade.fetchSubtopics(unknown)).isEmpty();
    }

    @Test
    void shouldExposeSummariesWithNeutralTypes() {
        // Arrange
        when(courseQueryService.handle(any(GetCourseByIdQuery.class))).thenReturn(Optional.of(course));
        when(courseQueryService.handle(any(GetCoursesByHolderIdQuery.class))).thenReturn(List.of(course));

        // Act
        var summary = facade.fetchCourseSummary(course.getId().value());
        var subtopics = facade.fetchSubtopics(course.getId().value());

        // Assert
        assertThat(summary).isPresent();
        assertThat(summary.get().code()).isEqualTo("MAT101");
        assertThat(summary.get().holderId()).isEqualTo("teacher-1");
        assertThat(facade.fetchCoursesByHolderId("teacher-1")).containsExactly(summary.get());
        assertThat(subtopics).extracting("name").containsExactly("Equations", "Inequalities");
        assertThat(subtopics).extracting("displayOrder").containsExactly(1, 2);
    }
}
