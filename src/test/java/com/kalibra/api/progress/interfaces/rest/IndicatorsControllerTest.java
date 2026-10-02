package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.progress.domain.exceptions.NoIndicatorsAvailableException;
import com.kalibra.api.progress.domain.model.queries.ExportCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetIndicatorGuideQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AccuracyIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.AnonymousStudentCode;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorsReport;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorExportRow;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorGuide;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorsCsvExport;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEvolutionIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.StudentAccuracyLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicApprovalLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicEvolutionLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicPracticeLine;
import com.kalibra.api.progress.domain.model.valueobjects.VerificationIndicator;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.transform.SubtopicMasteryAssemblerImpl;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndicatorsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SubtopicMasteryAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class IndicatorsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubtopicMasteryQueryService queryService;

    private final CourseId courseId = new CourseId(UUID.randomUUID());

    private CourseIndicatorsReport report() {
        var equations = UUID.randomUUID();
        var inequalities = UUID.randomUUID();
        return new CourseIndicatorsReport(courseId.value(), true,
                new AccuracyIndicator(66.7, List.of(
                        new StudentAccuracyLine(UUID.randomUUID(), "ana@kalibra.pe", 2, 3, Optional.of(66.7), true),
                        new StudentAccuracyLine(UUID.randomUUID(), "luis@kalibra.pe", 0, 0, Optional.empty(), false))),
                new PracticeIndicator(3, 3.0, 1, 2, List.of(
                        new SubtopicPracticeLine(equations, "Equations", 3), new SubtopicPracticeLine(inequalities, "Inequalities", 0))),
                new MasteryEvolutionIndicator(52.0, 60.0, 8, List.of(
                        new SubtopicEvolutionLine(equations, "Equations", Optional.of(52.0), Optional.of(60.0), Optional.of(8), MasteryLevel.MEDIUM, true),
                        new SubtopicEvolutionLine(inequalities, "Inequalities", Optional.empty(), Optional.empty(), Optional.empty(), MasteryLevel.NO_DATA, false))),
                new VerificationIndicator(75.0, 6, 2, List.of(new SubtopicApprovalLine(equations, "Equations", 75.0))));
    }

    @Test
    void shouldShowTheFourIndicatorsOfTheCourse() throws Exception {
        when(queryService.handle(new GetCourseIndicatorsQuery("teacher-1", courseId))).thenReturn(report());

        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasActivity").value(true))
                .andExpect(jsonPath("$.accuracy.groupAccuracy").value(66.7))
                .andExpect(jsonPath("$.accuracy.perStudent[0].correct").value(2))
                .andExpect(jsonPath("$.accuracy.perStudent[0].submitted").value(3))
                .andExpect(jsonPath("$.accuracy.perStudent[0].accuracy").value(66.7))
                .andExpect(jsonPath("$.accuracy.perStudent[1].hasActivity").value(false))
                .andExpect(jsonPath("$.accuracy.perStudent[1].accuracy").doesNotExist())
                .andExpect(jsonPath("$.practice.totalSolved").value(3))
                .andExpect(jsonPath("$.practice.averagePerActiveStudent").value(3.0))
                .andExpect(jsonPath("$.practice.activeStudents").value(1))
                .andExpect(jsonPath("$.practice.enrolledStudents").value(2))
                .andExpect(jsonPath("$.practice.perSubtopic[0].solved").value(3))
                .andExpect(jsonPath("$.evolution.groupInitial").value(52.0))
                .andExpect(jsonPath("$.evolution.groupCurrent").value(60.0))
                .andExpect(jsonPath("$.evolution.groupDeltaPoints").value(8))
                .andExpect(jsonPath("$.evolution.perSubtopic[0].deltaPoints").value(8))
                .andExpect(jsonPath("$.evolution.perSubtopic[0].level").value("MEDIUM"))
                .andExpect(jsonPath("$.evolution.perSubtopic[1].practiced").value(false))
                .andExpect(jsonPath("$.evolution.perSubtopic[1].deltaPoints").doesNotExist())
                .andExpect(jsonPath("$.verification.approvalRate").value(75.0))
                .andExpect(jsonPath("$.verification.approved").value(6))
                .andExpect(jsonPath("$.verification.discarded").value(2))
                .andExpect(jsonPath("$.verification.perSubtopic.length()").value(1))
                .andExpect(jsonPath("$.verification.perSubtopic[0].approvalRate").value(75.0));
    }

    @Test
    void shouldAnswerJsonWhenTheClientAsksForJsonExplicitly() throws Exception {
        when(queryService.handle(new GetCourseIndicatorsQuery("teacher-1", courseId))).thenReturn(report());

        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).accept(MediaType.APPLICATION_JSON)
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(queryService, never()).handle(any(ExportCourseIndicatorsQuery.class));
    }

    @Test
    void shouldExportTheIndicatorsAsCsvWithAnonymousCodesOnly() throws Exception {
        var export = new IndicatorsCsvExport("course-indicators.csv", List.of(
                new IndicatorExportRow(new AnonymousStudentCode("STU-0A1B2C3D4E"), "Equations", 3, 66.7, 52.0, 60.0, 75.0),
                new IndicatorExportRow(new AnonymousStudentCode("STU-0A1B2C3D4E"), "=Sums, \"hard\"", 1, 0.0, 30.0, 21.0, 0.0)));
        when(queryService.handle(new ExportCourseIndicatorsQuery("teacher-1", courseId))).thenReturn(export);

        var csv = mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).accept("text/csv")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"course-indicators.csv\""))
                .andReturn().getResponse().getContentAsString();

        assertThat(csv.lines().toList()).containsExactly(
                "student_code,subtopic,exercises_solved,accuracy_percentage,initial_mastery_percentage,"
                        + "current_mastery_percentage,verification_approval_percentage",
                "STU-0A1B2C3D4E,\"Equations\",3,66.7,52.0,60.0,75.0",
                "STU-0A1B2C3D4E,\"'=Sums, \"\"hard\"\"\",1,0.0,30.0,21.0,0.0");
        assertThat(csv).doesNotContain("@", "email", "name");
        verify(queryService, never()).handle(any(GetCourseIndicatorsQuery.class));
    }

    @Test
    void shouldNotOfferTheExportForACourseWithoutIndicators() throws Exception {
        when(queryService.handle(new ExportCourseIndicatorsQuery("teacher-1", courseId)))
                .thenThrow(new NoIndicatorsAvailableException(courseId.value()));

        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).accept("text/csv")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForAnotherTeachersCourse() throws Exception {
        when(queryService.handle(any(GetCourseIndicatorsQuery.class))).thenThrow(new CourseNotOwnedByTeacherException(courseId.value()));

        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldExplainWhatEachIndicatorMeasuresAndWhatAGoodSignalIs() throws Exception {
        when(queryService.handle(new GetIndicatorGuideQuery())).thenReturn(IndicatorGuide.standard());

        mockMvc.perform(get("/api/v1/courses/{id}/indicators/guide", courseId.value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(4))
                .andExpect(jsonPath("$.entries[0].indicator").value("ACCURACY"))
                .andExpect(jsonPath("$.entries[0].whatItMeasures").isNotEmpty())
                .andExpect(jsonPath("$.entries[0].goodSignal").isNotEmpty())
                .andExpect(jsonPath("$.entries[3].indicator").value("VERIFICATION_APPROVAL"));
    }

    @Test
    void shouldRejectStudentsAndAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/courses/{id}/indicators", courseId.value()).accept("text/csv")
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/courses/{id}/indicators/guide", courseId.value()).with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetCourseIndicatorsQuery.class));
    }
}
