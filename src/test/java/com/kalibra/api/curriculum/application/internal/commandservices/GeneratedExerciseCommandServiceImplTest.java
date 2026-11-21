package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicArchivedException;
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
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseOrigin;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.shared.engine.EngineUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

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

    @Mock
    GeneratedExercisePoolRefillService refillService;

    @InjectMocks
    GeneratedExerciseCommandServiceImpl service;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations", "Inequalities")));
    private final SubtopicId subtopicId = course.getSubtopics().getFirst().getId();
    private final GenerateExercisesForSubtopicCommand teacherCommand =
            new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 2);
    private final GenerateExerciseForStudentCommand studentCommand =
            new GenerateExerciseForStudentCommand(course.getId(), subtopicId, Optional.of(new MasteryProbability(0.4)), Set.of(), Optional.empty());
    private final GenerateExerciseForStudentCommand targetedCommand =
            new GenerateExerciseForStudentCommand(course.getId(), subtopicId, studentCommand.mastery(), Set.of(), Optional.of(DifficultyLevel.MEDIUM));

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
        when(externalExerciseGenerationService.generate(eq(targetedCommand), any(CurricularContent.class)))
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
        when(externalExerciseGenerationService.generate(eq(targetedCommand), any(CurricularContent.class)))
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
        var unknownCourse = new GenerateExerciseForStudentCommand(new CourseId(UUID.randomUUID()), subtopicId, Optional.empty(), Set.of(), Optional.empty());
        var foreignSubtopic = new GenerateExerciseForStudentCommand(course.getId(), new SubtopicId(UUID.randomUUID()), Optional.empty(), Set.of(), Optional.empty());
        when(courseRepository.findById(unknownCourse.courseId())).thenReturn(Optional.empty());
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

        assertThat(service.handle(unknownCourse)).isEmpty();
        assertThat(service.handle(foreignSubtopic)).isEmpty();
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @Test
    void shouldRejectTeacherGenerationForArchivedSubtopicBeforeCallingEngine() {
        course.getSubtopics().getFirst().setStatus(SubtopicStatus.ARCHIVED);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        assertThatThrownBy(() -> service.handle(teacherCommand)).isInstanceOf(SubtopicArchivedException.class);
        verifyNoInteractions(curricularMaterialRepository, externalExerciseGenerationService, generatedExerciseRepository);
    }

    @Test
    void shouldProvideNoStudentExerciseForArchivedSubtopic() {
        course.getSubtopics().getFirst().setStatus(SubtopicStatus.ARCHIVED);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        assertThat(service.handle(studentCommand)).isEmpty();
        verifyNoInteractions(curricularMaterialRepository, externalExerciseGenerationService, generatedExerciseRepository);
    }

    @ParameterizedTest
    @EnumSource(ExerciseOrigin.class)
    void shouldReuseAnApprovedExerciseWhenThePoolHasOne(ExerciseOrigin origin) {
        var answeredId = new ExerciseId(UUID.randomUUID());
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, studentCommand.mastery(),
                Set.of(answeredId), Optional.empty());
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        approved.setOrigin(origin);
        approved.setDifficulty(DifficultyLevel.MEDIUM);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of(answeredId)))
                .thenReturn(Optional.of(approved));
        when(generatedExerciseRepository.countAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM,
                Set.of(answeredId, approved.getId()))).thenReturn(3L);

        assertThat(service.handle(command)).containsSame(approved);
        verify(generatedExerciseRepository).findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of(answeredId));
        verifyNoInteractions(externalExerciseGenerationService, curricularMaterialRepository, refillService);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2})
    void shouldTriggerOneRefillWhenRemainingUnusedStockIsBelowThree(long stock) {
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        approved.setDifficulty(DifficultyLevel.MEDIUM);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of()))
                .thenReturn(Optional.of(approved));
        when(generatedExerciseRepository.countAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of(approved.getId())))
                .thenReturn(stock);
        givenReadyMaterial();

        assertThat(service.handle(studentCommand)).containsSame(approved);
        verify(refillService).request(eq(targetedCommand), any(CurricularContent.class));
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @ParameterizedTest
    @ValueSource(longs = {3, 4, 100})
    void shouldNotRefillWhenAtLeastThreeUnusedExercisesRemain(long stock) {
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of()))
                .thenReturn(Optional.of(approved));
        when(generatedExerciseRepository.countAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of(approved.getId())))
                .thenReturn(stock);

        assertThat(service.handle(studentCommand)).containsSame(approved);
        verifyNoInteractions(refillService, externalExerciseGenerationService, curricularMaterialRepository);
    }

    @Test
    void shouldReturnThePoolHitEvenWhenTheRefillCannotBeScheduled() {
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of()))
                .thenReturn(Optional.of(approved));
        givenReadyMaterial();
        doThrow(new IllegalStateException("Executor unavailable")).when(refillService).request(any(), any());

        assertThat(service.handle(studentCommand)).containsSame(approved);
        verifyNoInteractions(externalExerciseGenerationService);
    }

    @Test
    void shouldReturnThePoolHitEvenWhenTheStockCheckFails() {
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of()))
                .thenReturn(Optional.of(approved));
        when(generatedExerciseRepository.countAvailableForStudent(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Stock query unavailable"));

        assertThat(service.handle(studentCommand)).containsSame(approved);
        verifyNoInteractions(refillService, externalExerciseGenerationService);
    }

    @Test
    void shouldGenerateAtEasyWhenThePoolMissesWithoutMastery() {
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, Optional.empty(), Set.of(), Optional.empty());
        var targeted = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, Optional.empty(), Set.of(), Optional.of(DifficultyLevel.EASY));
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(targeted), any())).thenReturn(List.of(approved));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(command)).containsSame(approved);
        verify(externalExerciseGenerationService).generate(eq(targeted), any());
        verifyNoInteractions(refillService);
    }

    @Test
    void shouldUseTheMediumPoolWhenHardYieldsNothing() {
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId,
                Optional.of(new MasteryProbability(0.9)), Set.of(), Optional.empty());
        var hard = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, command.mastery(), Set.of(), Optional.of(DifficultyLevel.HARD));
        var discarded = GeneratedExerciseFixtures.discarded(course, subtopicId);
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        approved.setDifficulty(DifficultyLevel.MEDIUM);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(hard), any())).thenReturn(List.of(discarded));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.HARD, Set.of()))
                .thenReturn(Optional.empty());
        when(generatedExerciseRepository.findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of()))
                .thenReturn(Optional.of(approved));
        when(generatedExerciseRepository.countAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of(approved.getId())))
                .thenReturn(3L);

        assertThat(service.handle(command)).containsSame(approved);
        verify(externalExerciseGenerationService, times(1)).generate(any(GenerateExerciseForStudentCommand.class), any());
        verify(generatedExerciseRepository).saveAll(List.of(discarded));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldGenerateMediumOnceWhenHardHasNoApprovedResult(boolean mediumApproved) {
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId,
                Optional.of(new MasteryProbability(0.9)), Set.of(), Optional.empty());
        var hard = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, command.mastery(), Set.of(), Optional.of(DifficultyLevel.HARD));
        var medium = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, command.mastery(), Set.of(), Optional.of(DifficultyLevel.MEDIUM));
        var result = mediumApproved ? GeneratedExerciseFixtures.approved(course, subtopicId)
                : GeneratedExerciseFixtures.discarded(course, subtopicId);
        result.setDifficulty(DifficultyLevel.MEDIUM);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(hard), any())).thenReturn(List.of());
        when(externalExerciseGenerationService.generate(eq(medium), any())).thenReturn(List.of(result));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(command)).isEqualTo(mediumApproved ? Optional.of(result) : Optional.empty());
        verify(externalExerciseGenerationService).generate(eq(hard), any());
        verify(externalExerciseGenerationService).generate(eq(medium), any());
        verify(generatedExerciseRepository).findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of());
        verifyNoInteractions(refillService);
    }

    @Test
    void shouldPropagateTheOutageWhenHardGenerationFails() {
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId,
                Optional.of(new MasteryProbability(0.9)), Set.of(), Optional.empty());
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(any(GenerateExerciseForStudentCommand.class), any()))
                .thenThrow(new EngineUnavailableException("timeout"));

        assertThatThrownBy(() -> service.handle(command)).isInstanceOf(EngineUnavailableException.class);
        verify(generatedExerciseRepository, never()).findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of());
    }

    @Test
    void shouldNotRetryMediumWhenHardIsApproved() {
        var command = new GenerateExerciseForStudentCommand(course.getId(), subtopicId,
                Optional.of(new MasteryProbability(0.9)), Set.of(), Optional.empty());
        var hard = new GenerateExerciseForStudentCommand(course.getId(), subtopicId, command.mastery(), Set.of(), Optional.of(DifficultyLevel.HARD));
        var approved = GeneratedExerciseFixtures.approved(course, subtopicId);
        approved.setDifficulty(DifficultyLevel.HARD);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        givenReadyMaterial();
        when(externalExerciseGenerationService.generate(eq(hard), any())).thenReturn(List.of(approved));
        when(generatedExerciseRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(command)).containsSame(approved);
        verify(generatedExerciseRepository, never()).findAvailableForStudent(course.getId(), subtopicId, DifficultyLevel.MEDIUM, Set.of());
        verifyNoInteractions(refillService);
    }
}
