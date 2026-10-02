package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.exceptions.ExerciseNotFoundException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.queries.GetAttemptHistoryByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryLine;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryView;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptResultFilter;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import com.kalibra.api.progress.domain.services.ExerciseAttemptQueryService;
import com.kalibra.api.progress.interfaces.rest.transform.ExerciseAttemptAssemblerImpl;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseAttemptsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ExerciseAttemptAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ExerciseAttemptsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExerciseAttemptCommandService commandService;

    @MockitoBean
    ExerciseAttemptQueryService queryService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());

    private String body(String option) {
        return "{\"courseId\":\"" + courseId.value() + "\",\"exerciseId\":\"" + exerciseId.value()
                + "\",\"selectedOptionKey\":\"" + option + "\"}";
    }

    @Test
    void shouldRecordTheAnswerAndShowHowManyPointsTheMasteryWentUp() throws Exception {
        var attempt = ProgressFixtures.attempt("student-1", courseId, subtopicId, "B", 0.30, 0.52);
        when(commandService.handle(new SubmitAnswerCommand("student-1", courseId, exerciseId, "B"))).thenReturn(attempt);

        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("B"))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/exercise-attempts/" + attempt.getId().value()))
                .andExpect(jsonPath("$.id").value(attempt.getId().value().toString()))
                .andExpect(jsonPath("$.result").value("CORRECT"))
                .andExpect(jsonPath("$.statement").value("Solve 2x + 3 = 7"))
                .andExpect(jsonPath("$.explanation").value(ProgressFixtures.EXPLANATION))
                .andExpect(jsonPath("$.previousMastery").value(30.0))
                .andExpect(jsonPath("$.currentMastery").value(52.0))
                .andExpect(jsonPath("$.masteryDeltaPoints").value(22))
                .andExpect(jsonPath("$.masteryChanged").value(true))
                .andExpect(jsonPath("$.points").doesNotExist())
                .andExpect(jsonPath("$.streak").doesNotExist());
    }

    @Test
    void shouldTellThatTheMasteryDidNotChange() throws Exception {
        var attempt = ProgressFixtures.attempt("student-1", courseId, subtopicId, "A", 0.951, 0.954);
        when(commandService.handle(any(SubmitAnswerCommand.class))).thenReturn(attempt);

        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("A"))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("INCORRECT"))
                .andExpect(jsonPath("$.masteryDeltaPoints").value(0))
                .andExpect(jsonPath("$.masteryChanged").value(false));
    }

    @Test
    void shouldReturnForbiddenWhenTheStudentIsNotEnrolled() throws Exception {
        when(commandService.handle(any(SubmitAnswerCommand.class))).thenThrow(new NotEnrolledInCourseException());

        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("B"))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownOrDiscardedExercise() throws Exception {
        when(commandService.handle(any(SubmitAnswerCommand.class))).thenThrow(new ExerciseNotFoundException());

        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("B"))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnBadRequestForAnOptionOutsideTheFourAlternativesOrMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("E"))
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionKey\":\"A\"}").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        verify(commandService, never()).handle(any(SubmitAnswerCommand.class));
    }

    @Test
    void shouldListTheHistoryWithResultDateAndCounts() throws Exception {
        var attempt = ProgressFixtures.attempt("student-1", courseId, subtopicId, "A", 0.52, 0.44);
        var line = new AttemptHistoryLine(attempt.getId().value(), subtopicId.value(), attempt.getExerciseStatement(),
                AnswerResult.INCORRECT, attempt.getFeedback(), attempt.getMasteryChange(), attempt.getAnsweredAt());
        var view = new AttemptHistoryView(3, 2, 1, AttemptResultFilter.ALL, List.of(line), 0, 20, 3, 1);
        when(queryService.handle(new GetAttemptHistoryByCourseQuery("student-1", courseId, AttemptResultFilter.ALL, Pagination.of(0, 20))))
                .thenReturn(view);

        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allCount").value(3))
                .andExpect(jsonPath("$.correctCount").value(2))
                .andExpect(jsonPath("$.incorrectCount").value(1))
                .andExpect(jsonPath("$.activeFilter").value("ALL"))
                .andExpect(jsonPath("$.content[0].id").value(attempt.getId().value().toString()))
                .andExpect(jsonPath("$.content[0].result").value("INCORRECT"))
                .andExpect(jsonPath("$.content[0].answeredAt").exists())
                .andExpect(jsonPath("$.content[0].explanation").value(ProgressFixtures.EXPLANATION))
                .andExpect(jsonPath("$.content[0].masteryDeltaPoints").value(-8))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldFilterTheHistoryByResultAndHighlightTheActiveFilter() throws Exception {
        var query = new GetAttemptHistoryByCourseQuery("student-1", courseId, AttemptResultFilter.INCORRECT, Pagination.of(1, 5));
        when(queryService.handle(query)).thenReturn(new AttemptHistoryView(3, 2, 1, AttemptResultFilter.INCORRECT, List.of(), 1, 5, 1, 1));

        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .param("result", "incorrect").param("page", "1").param("size", "5")
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeFilter").value("INCORRECT"))
                .andExpect(jsonPath("$.incorrectCount").value(1))
                .andExpect(jsonPath("$.content").isEmpty());
        verify(queryService).handle(query);
    }

    @Test
    void shouldAnswerAnEmptyHistoryWithoutError() throws Exception {
        when(queryService.handle(any(GetAttemptHistoryByCourseQuery.class)))
                .thenReturn(new AttemptHistoryView(0, 0, 0, AttemptResultFilter.ALL, List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allCount").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldReturnBadRequestForAnUnsupportedFilterPageSizeOrMissingCourse() throws Exception {
        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .param("result", "MAYBE").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .param("size", "500").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/exercise-attempts").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        verify(queryService, never()).handle(any(GetAttemptHistoryByCourseQuery.class));
    }

    @Test
    void shouldRejectTeachersAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/exercise-attempts").param("courseId", courseId.value().toString())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/exercise-attempts").contentType(MediaType.APPLICATION_JSON).content(body("B"))
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(SubmitAnswerCommand.class));
    }
}
