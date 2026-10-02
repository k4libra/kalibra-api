package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedExerciseCommandServiceImplTest {

    @Mock
    GeneratedExerciseRepository generatedExerciseRepository;

    @Mock
    CurricularMaterialRepository curricularMaterialRepository;

    @Mock
    CourseRepository courseRepository;

    @Mock
    ExternalExerciseGenerationService externalExerciseGenerationService;

    @InjectMocks
    GeneratedExerciseCommandServiceImpl service;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));
    private final SubtopicId subtopicId = course.getSubtopics().getFirst().getId();
    private final GenerateExercisesForSubtopicCommand teacherCommand =
            new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 2);
    private final GenerateExerciseForStudentCommand studentCommand =
            new GenerateExerciseForStudentCommand(course.getId(), subtopicId, Optional.of(new MasteryProbability(0.4)));

    private CurricularMaterial readyMaterial(String text, int pages) {
        var material = CurricularMaterial.register(new UploadCurricularMaterialCommand("teacher-1", course.getId(),
                List.of(subtopicId), "unit.pdf", MaterialFormat.PDF, new byte[]{1}), "ref.pdf");
        material.markReady(new CurricularContent(text, pages));
        return material;
    }

    private void givenReadyMaterial() {
        when(curricularMaterialRepository.findAllByCourseIdAndSubtopicIdAndStatus(course.getId(), subtopicId, IngestionStatus.READY))
                .thenReturn(List.of(readyMaterial("Linear equations", 2), readyMaterial("Worked examples", 3)));
    }

    @Test
    void shouldGenerateForTheTeacherAndPersistEveryAttemptApprovedOrDiscarded() {
        // Arrange
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        var context = ArgumentCaptor.forClass(CurricularContent.class);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(teacherCommand), any(CurricularContent.class)))
                .thenReturn(List.of(discarded, approved));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(teacherCommand);

        // Assert
        assertThat(result).containsExactly(discarded, approved);
        verify(generatedExerciseRepository).saveAll(List.of(discarded, approved));
        verify(externalExerciseGenerationService).generate(eq(teacherCommand), context.capture());
        assertThat(context.getValue().normalizedText()).isEqualTo("Linear equations\n\nWorked examples");
        assertThat(context.getValue().pageCount()).isEqualTo(5);
    }

    @Test
    void shouldRejectTheTeacherRequestWhenTheSubtopicHasNoIngestedMaterial() {
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.findAllByCourseIdAndSubtopicIdAndStatus(course.getId(), subtopicId, IngestionStatus.READY))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.handle(teacherCommand)).isInstanceOf(SubtopicWithoutIngestedMaterialException.class);
        verifyNoInteractions(externalExerciseGenerationService);
        verify(generatedExerciseRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectACourseOfAnotherTeacher() {
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(teacherCommand)).isInstanceOf(CourseNotOwnedByTeacherException.class);
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @Test
    void shouldRejectASubtopicOutsideTheCourse() {
        var foreign = new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), new SubtopicId(UUID.randomUUID()), 1);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.handle(foreign)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @Test
    void shouldDeliverToTheStudentOnlyTheApprovedExerciseAndStillPersistTheDiscardedOnes() {
        // Arrange
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(studentCommand), any(CurricularContent.class)))
                .thenReturn(List.of(discarded, approved));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var delivered = service.handle(studentCommand);

        // Assert
        assertThat(delivered).containsSame(approved);
        verify(generatedExerciseRepository).saveAll(List.of(discarded, approved));
    }

    @Test
    void shouldDeliverNothingWhenEveryAttemptWasDiscarded() {
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(studentCommand), any(CurricularContent.class)))
                .thenReturn(List.of(discarded, discarded));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.handle(studentCommand)).isEmpty();
        verify(generatedExerciseRepository).saveAll(List.of(discarded, discarded));
    }

    @Test
    void shouldRejectTheStudentRequestWhenTheSubtopicHasNoIngestedMaterial() {
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.findAllByCourseIdAndSubtopicIdAndStatus(course.getId(), subtopicId, IngestionStatus.READY))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.handle(studentCommand)).isInstanceOf(SubtopicWithoutIngestedMaterialException.class);
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @Test
    void shouldDeliverNothingForAnUnknownCourseOrSubtopic() {
        var unknownCourse = new GenerateExerciseForStudentCommand(new CourseId(UUID.randomUUID()), subtopicId, Optional.empty());
        var foreignSubtopic = new GenerateExerciseForStudentCommand(course.getId(), new SubtopicId(UUID.randomUUID()), Optional.empty());
        when(courseRepository.findById(unknownCourse.courseId())).thenReturn(Optional.empty());
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

        assertThat(service.handle(unknownCourse)).isEmpty();
        assertThat(service.handle(foreignSubtopic)).isEmpty();
        verifyNoInteractions(externalExerciseGenerationService);
    }
}
