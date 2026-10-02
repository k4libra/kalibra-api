package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExercisesByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.GeneratedExerciseAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeneratedExercisesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GeneratedExerciseAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class GeneratedExercisesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GeneratedExerciseCommandService commandService;

    @MockitoBean
    GeneratedExerciseQueryService queryService;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations")));
    private final SubtopicId subtopicId = course.getSubtopics().getFirst().getId();

    private String body(int quantity) {
        return "{\"subtopicId\":\"" + subtopicId.value() + "\",\"quantity\":" + quantity + "}";
    }

    @Test
    void shouldGenerateAndAnswerEveryAttemptWithItsVerdict() throws Exception {
        var command = new GenerateExercisesForSubtopicCommand("teacher-1", course.getId(), subtopicId, 2);
        when(commandService.handle(command)).thenReturn(List.of(
                GeneratedExerciseFixtures.approved(course, subtopicId), GeneratedExerciseFixtures.discarded(course, subtopicId)));

        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(2))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].verdict").value("APPROVED"))
                .andExpect(jsonPath("$[0].origin").value("TEACHER_REQUEST"))
                .andExpect(jsonPath("$[0].options.length()").value(4))
                .andExpect(jsonPath("$[0].options[1].correct").value(true))
                .andExpect(jsonPath("$[0].rejectionReason").doesNotExist())
                .andExpect(jsonPath("$[1].verdict").value("DISCARDED"))
                .andExpect(jsonPath("$[1].rejectionReason").value(GeneratedExerciseFixtures.REJECTION_REASON));
    }

    @Test
    void shouldReturnConflictWhenTheSubtopicHasNoIngestedMaterial() throws Exception {
        when(commandService.handle(any(GenerateExercisesForSubtopicCommand.class)))
                .thenThrow(new SubtopicWithoutIngestedMaterialException(subtopicId.value()));

        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(1))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnNotFoundForAnotherTeachersCourse() throws Exception {
        when(commandService.handle(any(GenerateExercisesForSubtopicCommand.class)))
                .thenThrow(new CourseNotOwnedByTeacherException(course.getId().value()));

        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(1))
                        .with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForAQuantityOutsideTheLimitOrASubtopicOutsideTheCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(11))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(0))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
        verify(commandService, never()).handle(any(GenerateExercisesForSubtopicCommand.class));

        when(commandService.handle(any(GenerateExercisesForSubtopicCommand.class)))
                .thenThrow(new IllegalArgumentException("Subtopic does not belong to the course"));
        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(1))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldListTheExercisesWithContentAndVerificationResult() throws Exception {
        var page = new GeneratedExercisePage(List.of(GeneratedExerciseFixtures.discarded(course, subtopicId)), 0, 20, 1, 1);
        when(queryService.handle(new GetGeneratedExercisesByCourseQuery("teacher-1", course.getId(), Optional.empty(), Pagination.of(0, 20))))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].statement").value("Solve x = x"))
                .andExpect(jsonPath("$.content[0].verdict").value("DISCARDED"))
                .andExpect(jsonPath("$.content[0].rejectionReason").value(GeneratedExerciseFixtures.REJECTION_REASON))
                .andExpect(jsonPath("$.content[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterTheListBySubtopic() throws Exception {
        var query = new GetGeneratedExercisesByCourseQuery("teacher-1", course.getId(), Optional.of(subtopicId), Pagination.of(1, 5));
        when(queryService.handle(query)).thenReturn(new GeneratedExercisePage(List.of(), 1, 5, 0, 0));

        mockMvc.perform(get("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .param("subtopicId", subtopicId.value().toString()).param("page", "1").param("size", "5")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(1));
        verify(queryService).handle(query);
    }

    @Test
    void shouldReturnBadRequestForAPageSizeAboveTheLimit() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .param("size", "500").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectStudentsAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/generated-exercises", course.getId().value()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/courses/{id}/generated-exercises", course.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(body(1))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetGeneratedExercisesByCourseQuery.class));
        verify(commandService, never()).handle(any(GenerateExercisesForSubtopicCommand.class));
    }
}
