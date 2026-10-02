package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PracticeExercisesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ExerciseAttemptAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class PracticeExercisesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExerciseAttemptCommandService commandService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final RequestPracticeExerciseCommand command = new RequestPracticeExerciseCommand("student-1", courseId, subtopicId);

    private String body() {
        return "{\"courseId\":\"" + courseId.value() + "\",\"subtopicId\":\"" + subtopicId.value() + "\"}";
    }

    @Test
    void shouldDeliverAVerifiedExerciseWithoutRevealingTheAnswer() throws Exception {
        var exerciseId = UUID.randomUUID();
        when(commandService.handle(command)).thenReturn(Optional.of(new PracticeExerciseView(exerciseId, subtopicId.value(),
                "Solve 2x + 3 = 7", List.of("x = 1", "x = 2", "x = 3", "x = 5"))));

        var response = mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content(body())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseId").value(exerciseId.toString()))
                .andExpect(jsonPath("$.subtopicId").value(subtopicId.value().toString()))
                .andExpect(jsonPath("$.statement").value("Solve 2x + 3 = 7"))
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.options[1]").value("x = 2"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("correct", "explanation", "verdict");
    }

    @Test
    void shouldReturnNotFoundWhenNoVerifiedExerciseIsAvailable() throws Exception {
        when(commandService.handle(command)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content(body())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No verified exercise is available for the subtopic yet"));
    }

    @Test
    void shouldReturnForbiddenWhenTheStudentIsNotEnrolled() throws Exception {
        when(commandService.handle(command)).thenThrow(new NotEnrolledInCourseException());

        mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content(body())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnBadRequestWithoutCourseOrSubtopic() throws Exception {
        mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        verify(commandService, never()).handle(any(RequestPracticeExerciseCommand.class));
    }

    @Test
    void shouldRejectTeachersAndAnonymousCallers() throws Exception {
        mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/practice-exercises").contentType(MediaType.APPLICATION_JSON).content(body())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/practice-exercises").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        verify(commandService, never()).handle(any(RequestPracticeExerciseCommand.class));
    }
}
