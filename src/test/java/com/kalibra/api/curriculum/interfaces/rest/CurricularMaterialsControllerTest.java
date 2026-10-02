package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import com.kalibra.api.curriculum.interfaces.rest.transform.CurricularMaterialAssemblerImpl;
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
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurricularMaterialsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CurricularMaterialAssemblerImpl.class})
@TestPropertySource(properties = "kalibra.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class CurricularMaterialsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CurricularMaterialCommandService commandService;

    @MockitoBean
    CurricularMaterialQueryService queryService;

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations")));

    private final MockMultipartFile pdf = new MockMultipartFile("file", "unit-1.pdf", "application/pdf", new byte[]{1, 2, 3});

    private String subtopicId() {
        return course.getSubtopics().getFirst().getId().value().toString();
    }

    private CurricularMaterial registered() {
        var command = new UploadCurricularMaterialCommand("teacher-1", course.getId(),
                List.of(course.getSubtopics().getFirst().getId()), "unit-1.pdf", MaterialFormat.PDF, new byte[]{1, 2, 3});
        return CurricularMaterial.register(command, "ref.pdf");
    }

    @Test
    void shouldUploadMaterialAndAnswerPendingIngestion() throws Exception {
        // Arrange
        var captor = ArgumentCaptor.forClass(UploadCurricularMaterialCommand.class);
        when(commandService.handle(any(UploadCurricularMaterialCommand.class))).thenReturn(registered());

        // Act
        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("subtopicIds", subtopicId())
                        .param("fileName", "unit-1.pdf")
                        .param("format", "pdf")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_INGESTION"))
                .andExpect(jsonPath("$.format").value("PDF"))
                .andExpect(jsonPath("$.failureReason").doesNotExist());

        // Assert
        verify(commandService).handle(captor.capture());
        assertThat(captor.getValue().holderId()).isEqualTo("teacher-1");
        assertThat(captor.getValue().format()).isEqualTo(MaterialFormat.PDF);
        assertThat(captor.getValue().content()).containsExactly(1, 2, 3);
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForAnUnsupportedFormat() throws Exception {
        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("subtopicIds", subtopicId())
                        .param("fileName", "unit-1.docx")
                        .param("format", "DOCX")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
        verify(commandService, never()).handle(any(UploadCurricularMaterialCommand.class));
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForACorruptFile() throws Exception {
        when(commandService.handle(any(UploadCurricularMaterialCommand.class)))
                .thenThrow(UnsupportedMaterialFormatException.unreadable("PDF"));

        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("subtopicIds", subtopicId())
                        .param("fileName", "unit-1.pdf")
                        .param("format", "PDF")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("The file is corrupt or is not a valid PDF file"));
    }

    @Test
    void shouldAnswerAnEmptyPageForACourseWithoutMaterials() throws Exception {
        when(queryService.handle(new GetCurricularMaterialsByCourseQuery("teacher-1", course.getId(), Pagination.of(0, 20))))
                .thenReturn(new CurricularMaterialPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/courses/{id}/curricular-materials", course.getId().value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldReturnBadRequestWithoutSubtopics() throws Exception {
        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("fileName", "unit-1.pdf")
                        .param("format", "PDF")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForAnEmptyFile() throws Exception {
        var empty = new MockMultipartFile("file", "unit-1.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(empty)
                        .param("subtopicIds", subtopicId())
                        .param("fileName", "unit-1.pdf")
                        .param("format", "PDF")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForASubtopicOutsideTheCourse() throws Exception {
        when(commandService.handle(any(UploadCurricularMaterialCommand.class)))
                .thenThrow(new IllegalArgumentException("Subtopic does not belong to the course"));

        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("subtopicIds", java.util.UUID.randomUUID().toString())
                        .param("fileName", "unit-1.pdf")
                        .param("format", "PDF")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnNotFoundWhenUploadingToAnotherTeachersCourse() throws Exception {
        when(commandService.handle(any(UploadCurricularMaterialCommand.class)))
                .thenThrow(new CourseNotOwnedByTeacherException(course.getId().value()));

        mockMvc.perform(multipart("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .file(pdf)
                        .param("subtopicIds", subtopicId())
                        .param("fileName", "unit-1.pdf")
                        .param("format", "PDF")
                        .with(user("teacher-2").roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListMaterialsWithTheirStatus() throws Exception {
        // Arrange
        var failed = registered();
        failed.markFailed("Unreadable scan");
        var page = new CurricularMaterialPage(List.of(failed), 0, 20, 1, 1);
        when(queryService.handle(new GetCurricularMaterialsByCourseQuery("teacher-1", course.getId(), Pagination.of(0, 20))))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/courses/{id}/curricular-materials", course.getId().value()).with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("INGESTION_ERROR"))
                .andExpect(jsonPath("$.content[0].failureReason").value("Unreadable scan"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldReturnBadRequestForAPageSizeAboveTheLimit() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .param("size", "500")
                        .with(user("teacher-1").roles("TEACHER")))
                .andExpect(status().isBadRequest());
        verify(queryService, never()).handle(any(GetCurricularMaterialsByCourseQuery.class));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/curricular-materials", course.getId().value()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidMaterialsToStudents() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}/curricular-materials", course.getId().value())
                        .with(user("student-1").roles("STUDENT")))
                .andExpect(status().isForbidden());
        verify(queryService, never()).handle(any(GetCurricularMaterialsByCourseQuery.class));
    }
}
