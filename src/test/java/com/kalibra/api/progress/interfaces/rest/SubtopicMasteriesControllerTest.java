package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.transform.SubtopicMasteryAssemblerImpl;
import com.kalibra.api.shared.config.JwtAuthenticationFilter;
import com.kalibra.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubtopicMasteriesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SubtopicMasteryAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class SubtopicMasteriesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubtopicMasteryQueryService queryService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final GetPracticeSubtopicsByCourseQuery query = new GetPracticeSubtopicsByCourseQuery("student-1", courseId);

    @Test
    void shouldListTheSubtopicsOfTheCourseTheStudentIsEnrolledIn() throws Exception {
        when(queryService.handle(query)).thenReturn(List.of(
                new PracticeSubtopicView(UUID.randomUUID(), "Equations", MasteryLevel.HIGH),
                new PracticeSubtopicView(UUID.randomUUID(), "Inequalities", MasteryLevel.NO_DATA)));

        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", courseId.value().toString())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Equations"))
                .andExpect(jsonPath("$[0].level").value("HIGH"))
                .andExpect(jsonPath("$[1].level").value("NO_DATA"));
    }

    @Test
    void shouldAnswerAnEmptyListForACourseWithoutSubtopics() throws Exception {
        when(queryService.handle(query)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", courseId.value().toString())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnForbiddenWhenTheStudentIsNotEnrolled() throws Exception {
        when(queryService.handle(query)).thenThrow(new NotEnrolledInCourseException());

        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", courseId.value().toString())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnBadRequestWithoutCourse() throws Exception {
        mockMvc.perform(get("/api/v1/subtopic-masteries").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", "not-a-uuid").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTeachersAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", courseId.value().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/subtopic-masteries").param("courseId", courseId.value().toString())
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetPracticeSubtopicsByCourseQuery.class));
    }
}
