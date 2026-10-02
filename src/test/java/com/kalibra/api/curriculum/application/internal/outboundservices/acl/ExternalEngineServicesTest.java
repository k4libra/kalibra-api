package com.kalibra.api.curriculum.application.internal.outboundservices.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseOrigin;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.shared.engine.EngineTaskClient;
import com.kalibra.api.shared.engine.EngineTaskFailedException;
import com.kalibra.api.shared.engine.EngineUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalEngineServicesTest {

    private static final String RUNS = """
            [
              {
                "approvedExercise": {"statement": "Resuelve 2x + 3 = 7"},
                "exhausted": false,
                "attempts": [
                  {
                    "number": 1,
                    "exercise": {"statement": "Resuelve x = ?", "options": ["1", "2", "3", "4"],
                                 "correctOptionKey": "A", "explanation": "", "difficulty": "HARD"},
                    "approved": false, "correctnessPassed": true, "difficultyPassed": false,
                    "rejectionReason": "La dificultad no corresponde al nivel del estudiante.", "usedFallback": false
                  },
                  {
                    "number": 2,
                    "exercise": {"statement": "Resuelve 2x + 3 = 7", "options": ["1", "2", "3", "4"],
                                 "correctOptionKey": "B", "explanation": "Resta 3 y divide entre 2.", "difficulty": "MEDIUM"},
                    "approved": true, "correctnessPassed": true, "difficultyPassed": true,
                    "rejectionReason": null, "usedFallback": true
                  }
                ]
              }
            ]
            """;

    @Mock
    EngineTaskClient engineTaskClient;

    @Mock
    CourseRepository courseRepository;

    @Mock
    MaterialStorageService materialStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));
    private final CurricularContent context = new CurricularContent("Linear equations", 2);

    private JsonNode json(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    private CurricularMaterial material() {
        return CurricularMaterial.register(new UploadCurricularMaterialCommand("teacher-1", course.getId(),
                List.of(course.getSubtopics().getFirst().getId()), "syllabus.pdf", MaterialFormat.PDF, new byte[]{1}), "ref.pdf");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAskTheEngineForOneExerciseAtTheLevelOfTheStudentAndKeepEveryAttempt() throws Exception {
        var subtopicId = course.getSubtopics().getFirst().getId();
        var payload = ArgumentCaptor.forClass(Object.class);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(engineTaskClient.submit(eq("exercise-generations"), payload.capture())).thenReturn(json(RUNS));
        var service = new ExternalExerciseGenerationService(engineTaskClient, courseRepository);

        var attempts = service.generate(
                new GenerateExerciseForStudentCommand(course.getId(), subtopicId, Optional.of(new MasteryProbability(0.52))), context);

        assertThat((Map<String, Object>) payload.getValue())
                .containsEntry("courseId", course.getId().value().toString())
                .containsEntry("subtopicId", subtopicId.value().toString())
                .containsEntry("subtopicName", "Equations")
                .containsEntry("normalizedContent", "Linear equations")
                .containsEntry("masteryProbability", 0.52)
                .containsEntry("quantity", 1);
        assertThat(attempts).hasSize(2);
        var discarded = attempts.getFirst();
        assertThat(discarded.isAvailableToStudents()).isFalse();
        assertThat(discarded.getVerification().rejectionReason()).contains("La dificultad no corresponde al nivel del estudiante.");
        assertThat(discarded.getVerification().difficultyPassed()).isFalse();
        var approved = attempts.getLast();
        assertThat(approved.isAvailableToStudents()).isTrue();
        assertThat(approved.getOrigin()).isEqualTo(ExerciseOrigin.STUDENT_PRACTICE);
        assertThat(approved.getDifficulty()).isEqualTo(DifficultyLevel.MEDIUM);
        assertThat(approved.correctOption().getKey()).isEqualTo("B");
        assertThat(approved.correctOption().getText()).isEqualTo("2");
        assertThat(approved.getVerification().usedFallback()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAskTheEngineForTheQuantityTheTeacherRequested() throws Exception {
        var subtopicId = course.getSubtopics().getFirst().getId();
        var payload = ArgumentCaptor.forClass(Object.class);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(engineTaskClient.submit(eq("exercise-generations"), payload.capture())).thenReturn(json(RUNS));
        var service = new ExternalExerciseGenerationService(engineTaskClient, courseRepository);

        var attempts = service.generate(new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 3), context);

        assertThat((Map<String, Object>) payload.getValue())
                .containsEntry("quantity", 3)
                .containsEntry("masteryProbability", null);
        assertThat(attempts).extracting(GeneratedExercise::getOrigin).containsOnly(ExerciseOrigin.TEACHER_REQUEST);
    }

    @Test
    void shouldSkipAnAttemptTheDomainCannotHold() throws Exception {
        var malformed = RUNS.replaceFirst("\\[\"1\", \"2\", \"3\", \"4\"]", "[\"1\", \"2\"]");
        var subtopicId = course.getSubtopics().getFirst().getId();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(engineTaskClient.submit(eq("exercise-generations"), any())).thenReturn(json(malformed));
        var service = new ExternalExerciseGenerationService(engineTaskClient, courseRepository);

        var attempts = service.generate(new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 1), context);

        assertThat(attempts).hasSize(1);
        assertThat(attempts.getFirst().isAvailableToStudents()).isTrue();
    }

    @Test
    void shouldLetAnEngineFailureReachTheCaller() {
        var subtopicId = course.getSubtopics().getFirst().getId();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(engineTaskClient.submit(eq("exercise-generations"), any()))
                .thenThrow(new EngineTaskFailedException(502, "Un proveedor externo de IA no respondió correctamente."));
        var service = new ExternalExerciseGenerationService(engineTaskClient, courseRepository);

        assertThatThrownBy(() -> service.generate(
                new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 1), context))
                .isInstanceOf(EngineTaskFailedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendThePresignedUrlOfTheMaterialAndReturnItsContent() throws Exception {
        var material = material();
        var payload = ArgumentCaptor.forClass(Object.class);
        when(materialStorageService.temporaryUrl("ref.pdf")).thenReturn("https://r2.example/ref.pdf?sig=1");
        when(engineTaskClient.submit(eq("curricular-extractions"), payload.capture()))
                .thenReturn(json("{\"normalizedText\":\"Ecuaciones lineales\",\"pageCount\":3}"));
        var service = new ExternalCurricularExtractionService(engineTaskClient, materialStorageService);

        var content = service.extract(material);

        assertThat(content).contains(new CurricularContent("Ecuaciones lineales", 3));
        assertThat((Map<String, Object>) payload.getValue())
                .containsEntry("materialId", material.getId().value().toString())
                .containsEntry("storageReference", "https://r2.example/ref.pdf?sig=1")
                .containsEntry("format", "pdf");
        verify(materialStorageService).temporaryUrl("ref.pdf");
    }

    @Test
    void shouldTreatAMaterialTheEngineRejectsAsUnreadable() {
        when(materialStorageService.temporaryUrl("ref.pdf")).thenReturn("https://r2.example/ref.pdf?sig=1");
        when(engineTaskClient.submit(eq("curricular-extractions"), any()))
                .thenThrow(new EngineTaskFailedException(422, "No se pudo extraer el contenido del material."));
        var service = new ExternalCurricularExtractionService(engineTaskClient, materialStorageService);

        assertThat(service.extract(material())).isEmpty();
    }

    @Test
    void shouldKeepTheMaterialPendingWhenTheEngineOrItsProviderIsDown() {
        when(materialStorageService.temporaryUrl("ref.pdf")).thenReturn("https://r2.example/ref.pdf?sig=1");
        var service = new ExternalCurricularExtractionService(engineTaskClient, materialStorageService);

        when(engineTaskClient.submit(eq("curricular-extractions"), any()))
                .thenThrow(new EngineTaskFailedException(502, "Un proveedor externo de IA no respondió correctamente."));
        assertThatThrownBy(() -> service.extract(material())).isInstanceOf(EngineTaskFailedException.class);

        when(engineTaskClient.submit(eq("curricular-extractions"), any()))
                .thenThrow(new EngineUnavailableException("timeout"));
        assertThatThrownBy(() -> service.extract(material())).isInstanceOf(EngineUnavailableException.class);
    }
}
