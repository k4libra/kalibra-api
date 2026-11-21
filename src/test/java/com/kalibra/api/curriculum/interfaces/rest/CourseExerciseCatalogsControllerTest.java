package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetExerciseCatalogByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseExerciseCatalog;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicExerciseCount;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.GeneratedExerciseAssemblerImpl;
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

@WebMvcTest(CourseExerciseCatalogsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GeneratedExerciseAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CourseExerciseCatalogsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GeneratedExerciseQueryService queryService;

    @Test
    void shouldListTheCountsGroupedByCourseAndSubtopicIncludingCoursesWithoutExercises() throws Exception {
        var withExercises = new CourseExerciseCatalog(UUID.randomUUID(), "Algebra",
                List.of(new SubtopicExerciseCount(UUID.randomUUID(), "Equations", 5, 4, 1)));
        var withoutExercises = new CourseExerciseCatalog(UUID.randomUUID(), "Algorithms",
                List.of(new SubtopicExerciseCount(UUID.randomUUID(), "Sorting", 0, 0, 0)));
        when(queryService.handle(new GetExerciseCatalogByHolderIdQuery("teacher-1")))
                .thenReturn(List.of(withExercises, withoutExercises));

        mockMvc.perform(get("/api/v1/course-exercise-catalogs").with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Algebra"))
                .andExpect(jsonPath("$[0].subtopics[0].subtopicName").value("Equations"))
                .andExpect(jsonPath("$[0].subtopics[0].generated").value(5))
                .andExpect(jsonPath("$[0].subtopics[0].approved").value(4))
                .andExpect(jsonPath("$[0].subtopics[0].discarded").value(1))
                .andExpect(jsonPath("$[1].courseName").value("Algorithms"))
                .andExpect(jsonPath("$[1].subtopics[0].generated").value(0));
    }

    @Test
    void shouldRejectStudentsAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/course-exercise-catalogs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/course-exercise-catalogs").with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetExerciseCatalogByHolderIdQuery.class));
    }
}
