package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetExerciseCatalogByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExerciseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExercisesByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetVerificationApprovalByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedExerciseQueryServiceImplTest {

    @Mock
    GeneratedExerciseRepository generatedExerciseRepository;

    @Mock
    CourseRepository courseRepository;

    @InjectMocks
    GeneratedExerciseQueryServiceImpl service;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));
    private final Course emptyCourse = Course.create(new CreateCourseCommand("teacher-1", "Algorithms",
            new CourseCode("ALG201"), List.of("Sorting")));
    private final SubtopicId equations = course.getSubtopics().getFirst().getId();
    private final Pagination pagination = Pagination.of(0, 20);

    @Test
    void shouldListTheExercisesOfMyCourseWithAndWithoutSubtopicFilter() {
        // Arrange
        var all = new GeneratedExercisePage(List.of(GeneratedExerciseFixtures.approved(course, equations),
                GeneratedExerciseFixtures.discarded(course, equations)), 0, 20, 2, 1);
        var filtered = new GeneratedExercisePage(List.of(), 0, 20, 0, 0);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAllByCourseId(course.getId(), pagination)).thenReturn(all);
        when(generatedExerciseRepository.findAllByCourseIdAndSubtopicId(course.getId(), equations, pagination)).thenReturn(filtered);

        // Act & Assert
        assertThat(service.handle(new GetGeneratedExercisesByCourseQuery("teacher-1", course.getId(), Optional.empty(), pagination)))
                .isSameAs(all);
        assertThat(service.handle(new GetGeneratedExercisesByCourseQuery("teacher-1", course.getId(), Optional.of(equations), pagination)))
                .isSameAs(filtered);
    }

    @Test
    void shouldHideTheExercisesOfAnotherTeachersCourse() {
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(
                new GetGeneratedExercisesByCourseQuery("teacher-2", course.getId(), Optional.empty(), pagination)))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        verifyNoInteractions(generatedExerciseRepository);
    }

    @Test
    void shouldGroupTheCountsByCourseAndSubtopicKeepingCoursesWithoutExercises() {
        // Arrange
        when(courseRepository.findAllByHolderId("teacher-1")).thenReturn(List.of(course, emptyCourse));
        when(generatedExerciseRepository.findAllByCourseIdIn(List.of(course.getId(), emptyCourse.getId())))
                .thenReturn(List.of(GeneratedExerciseFixtures.approved(course, equations),
                        GeneratedExerciseFixtures.approved(course, equations),
                        GeneratedExerciseFixtures.discarded(course, equations)));

        // Act
        var catalogs = service.handle(new GetExerciseCatalogByHolderIdQuery("teacher-1"));

        // Assert
        assertThat(catalogs).hasSize(2);
        assertThat(catalogs.getFirst().courseName()).isEqualTo("Algebra");
        var equationsCount = catalogs.getFirst().subtopics().getFirst();
        assertThat(equationsCount.subtopicName()).isEqualTo("Equations");
        assertThat(equationsCount.generated()).isEqualTo(3);
        assertThat(equationsCount.approved()).isEqualTo(2);
        assertThat(equationsCount.discarded()).isEqualTo(1);
        assertThat(catalogs.getFirst().subtopics().get(1).generated()).isZero();
        assertThat(catalogs.get(1).courseName()).isEqualTo("Algorithms");
        assertThat(catalogs.get(1).subtopics()).singleElement()
                .satisfies(count -> assertThat(count.generated()).isZero());
    }

    @Test
    void shouldReportTheApprovalRateOmittingSubtopicsWithoutExercises() {
        // Arrange
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAllByCourseId(course.getId()))
                .thenReturn(List.of(GeneratedExerciseFixtures.approved(course, equations),
                        GeneratedExerciseFixtures.approved(course, equations),
                        GeneratedExerciseFixtures.approved(course, equations),
                        GeneratedExerciseFixtures.discarded(course, equations)));

        // Act
        var report = service.handle(new GetVerificationApprovalByCourseQuery(course.getId()));

        // Assert
        assertThat(report.generated()).isEqualTo(4);
        assertThat(report.approved()).isEqualTo(3);
        assertThat(report.discarded()).isEqualTo(1);
        assertThat(report.approvalRate()).isEqualTo(75.0);
        assertThat(report.bySubtopic()).singleElement()
                .satisfies(count -> {
                    assertThat(count.subtopicName()).isEqualTo("Equations");
                    assertThat(count.approvalRate()).isEqualTo(75.0);
                });
    }

    @Test
    void shouldReportNoApprovalForACourseWithoutExercises() {
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAllByCourseId(course.getId())).thenReturn(List.of());

        var report = service.handle(new GetVerificationApprovalByCourseQuery(course.getId()));

        assertThat(report.generated()).isZero();
        assertThat(report.approvalRate()).isZero();
        assertThat(report.bySubtopic()).isEmpty();
    }

    @Test
    void shouldFindAnExerciseById() {
        var exercise = GeneratedExerciseFixtures.approved(course, equations);
        when(generatedExerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));

        assertThat(service.handle(new GetGeneratedExerciseByIdQuery(exercise.getId()))).containsSame(exercise);
    }
}
