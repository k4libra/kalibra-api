package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.exceptions.NotEnrolledInCourseException;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressForTeacherQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.Feedback;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicProgressLine;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentProgressController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SubtopicMasteryAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class StudentProgressControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubtopicMasteryQueryService queryService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());

    private StudentProgressReport withActivity() {
        return new StudentProgressReport(studentId.value(), courseId.value(), true, List.of(
                new SubtopicProgressLine(UUID.randomUUID(), "Equations", Optional.of(new MasteryProbability(0.55)), MasteryLevel.MEDIUM, 4),
                new SubtopicProgressLine(UUID.randomUUID(), "Inequalities", Optional.empty(), MasteryLevel.NO_DATA, 0)),
                List.of(new Feedback("Subtract 3, divide by 2")));
    }

    private StudentProgressReport withoutActivity() {
        return new StudentProgressReport(studentId.value(), courseId.value(), false, List.of(
                new SubtopicProgressLine(UUID.randomUUID(), "Equations", Optional.empty(), MasteryLevel.NO_DATA, 0)), List.of());
    }

    @Test
    void shouldShowTheStudentTheirMasteryPerSubtopicAndRecentFeedback() throws Exception {
        when(queryService.handle(new GetStudentProgressByCourseQuery("student-1", courseId))).thenReturn(withActivity());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value()).with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActivity").value(true))
                .andExpect(jsonPath("$.subtopics[0].subtopicName").value("Equations"))
                .andExpect(jsonPath("$.subtopics[0].mastery").value(55.0))
                .andExpect(jsonPath("$.subtopics[0].level").value("MEDIUM"))
                .andExpect(jsonPath("$.subtopics[0].solvedCount").value(4))
                .andExpect(jsonPath("$.subtopics[1].mastery").doesNotExist())
                .andExpect(jsonPath("$.subtopics[1].level").value("NO_DATA"))
                .andExpect(jsonPath("$.recentFeedback[0]").value("Subtract 3, divide by 2"));
    }

    @Test
    void shouldShowAnInitialStateToAStudentWithoutSolvedExercises() throws Exception {
        when(queryService.handle(new GetStudentProgressByCourseQuery("student-1", courseId))).thenReturn(withoutActivity());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value()).with(user("student-1").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActivity").value(false))
                .andExpect(jsonPath("$.subtopics[0].mastery").doesNotExist())
                .andExpect(jsonPath("$.recentFeedback").isEmpty());
    }

    @Test
    void shouldReturnForbiddenToAStudentWhoIsNotEnrolled() throws Exception {
        when(queryService.handle(any(GetStudentProgressByCourseQuery.class))).thenThrow(new NotEnrolledInCourseException());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value()).with(user("student-2").roles("STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldShowTheTeacherTheProgressOfTheSelectedStudent() throws Exception {
        when(queryService.handle(new GetStudentProgressForTeacherQuery("teacher-1", courseId, studentId))).thenReturn(withActivity());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", studentId.value().toString()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentId.value().toString()))
                .andExpect(jsonPath("$.subtopics[0].mastery").value(55.0));
        verify(queryService, never()).handle(any(GetStudentProgressByCourseQuery.class));
    }

    @Test
    void shouldShowTheTeacherAnInformativeStateForAStudentWithoutActivity() throws Exception {
        when(queryService.handle(new GetStudentProgressForTeacherQuery("teacher-1", courseId, studentId))).thenReturn(withoutActivity());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", studentId.value().toString()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActivity").value(false));
    }

    @Test
    void shouldReturnNotFoundForACourseOfAnotherTeacherAndForbiddenForAStudentOutsideTheCourse() throws Exception {
        when(queryService.handle(new GetStudentProgressForTeacherQuery("teacher-2", courseId, studentId)))
                .thenThrow(new CourseNotOwnedByTeacherException(courseId.value()));
        when(queryService.handle(new GetStudentProgressForTeacherQuery("teacher-1", courseId, studentId)))
                .thenThrow(new NotEnrolledInCourseException());

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", studentId.value().toString()).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", studentId.value().toString()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotLetAStudentReadTheProgressOfAnotherStudent() throws Exception {
        when(queryService.handle(new GetStudentProgressForTeacherQuery("student-2", courseId, studentId)))
                .thenThrow(new CourseNotOwnedByTeacherException(courseId.value()));

        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", studentId.value().toString()).with(user("student-2").roles("STUDENT")))
                .andExpect(status().isNotFound());
        verify(queryService, never()).handle(any(GetStudentProgressByCourseQuery.class));
    }

    @Test
    void shouldReturnBadRequestForAnEmptyOrMalformedStudentId() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", "").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())
                        .param("studentId", "not-a-uuid").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
        verify(queryService, never()).handle(any(GetStudentProgressForTeacherQuery.class));
    }

    @Test
    void shouldRejectAnonymousCallersAndUsersWithoutARole() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses/{id}/student-progress", courseId.value()).with(user("someone").roles("REGISTERED_USER")))
                .andExpect(status().isForbidden());
    }
}
