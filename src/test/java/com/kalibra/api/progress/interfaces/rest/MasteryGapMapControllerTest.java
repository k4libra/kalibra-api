package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.model.queries.GetMasteryGapMapByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
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

@WebMvcTest(MasteryGapMapController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SubtopicMasteryAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class MasteryGapMapControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubtopicMasteryQueryService queryService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final GetMasteryGapMapByCourseQuery query = new GetMasteryGapMapByCourseQuery("teacher-1", courseId);

    @Test
    void shouldShowTheGroupMasteryLevelCountsAndEachStudentOrderedByPriority() throws Exception {
        var inequalities = UUID.randomUUID();
        var student = UUID.randomUUID();
        when(queryService.handle(query)).thenReturn(new MasteryGapMap(courseId.value(), true,
                List.of(new SubtopicGapLine(inequalities, "Inequalities", 25.0, 2, 0, 0, 1, 1),
                        new SubtopicGapLine(UUID.randomUUID(), "Equations", 70.0, 0, 1, 1, 1, 2)),
                List.of(new StudentMasteryCell(student, "ana@kalibra.pe", inequalities, Optional.of(new MasteryProbability(0.2)), MasteryLevel.LOW),
                        new StudentMasteryCell(UUID.randomUUID(), "rosa@kalibra.pe", inequalities, Optional.empty(), MasteryLevel.NO_DATA))));

        mockMvc.perform(get("/api/v1/courses/{id}/mastery-gap-map", courseId.value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSufficientData").value(true))
                .andExpect(jsonPath("$.subtopics[0].subtopicName").value("Inequalities"))
                .andExpect(jsonPath("$.subtopics[0].reinforcementPriority").value(1))
                .andExpect(jsonPath("$.subtopics[0].groupMastery").value(25.0))
                .andExpect(jsonPath("$.subtopics[0].lowCount").value(2))
                .andExpect(jsonPath("$.subtopics[0].noDataCount").value(1))
                .andExpect(jsonPath("$.subtopics[1].highCount").value(1))
                .andExpect(jsonPath("$.subtopics[1].mediumCount").value(1))
                .andExpect(jsonPath("$.students[0].email").value("ana@kalibra.pe"))
                .andExpect(jsonPath("$.students[0].mastery").value(20.0))
                .andExpect(jsonPath("$.students[0].level").value("LOW"))
                .andExpect(jsonPath("$.students[1].mastery").doesNotExist())
                .andExpect(jsonPath("$.students[1].level").value("NO_DATA"));
    }

    @Test
    void shouldTellThatThereIsNotEnoughDataInsteadOfShowingWrongInformation() throws Exception {
        when(queryService.handle(query)).thenReturn(new MasteryGapMap(courseId.value(), false, List.of(), List.of()));

        mockMvc.perform(get("/api/v1/courses/{id}/mastery-gap-map", courseId.value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSufficientData").value(false))
                .andExpect(jsonPath("$.subtopics").isEmpty());
    }

    @Test
    void shouldReturnNotFoundForAnotherTeachersCourse() throws Exception {
        when(queryService.handle(any(GetMasteryGapMapByCourseQuery.class)))
                .thenThrow(new CourseNotOwnedByTeacherException(courseId.value()));

        mockMvc.perform(get("/api/v1/courses/{id}/mastery-gap-map", courseId.value()).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldRejectStudentsAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/mastery-gap-map", courseId.value())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses/{id}/mastery-gap-map", courseId.value()).with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetMasteryGapMapByCourseQuery.class));
    }
}
