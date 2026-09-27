package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularMaterialPage;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurricularMaterialQueryServiceImplTest {

    @Mock
    CurricularMaterialRepository curricularMaterialRepository;

    @Mock
    CourseRepository courseRepository;

    @InjectMocks
    CurricularMaterialQueryServiceImpl service;

    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));

    @Test
    void shouldReturnThePageOfAnOwnCourse() {
        // Arrange
        var pagination = Pagination.of(0, 20);
        var page = new CurricularMaterialPage(List.of(), 0, 20, 0, 0);
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(curricularMaterialRepository.findAllByCourseId(course.getId(), pagination)).thenReturn(page);

        // Act & Assert
        assertThat(service.handle(new GetCurricularMaterialsByCourseQuery("teacher-1", course.getId(), pagination)))
                .isEqualTo(page);
    }

    @Test
    void shouldRejectListingTheMaterialOfAnotherTeachersCourse() {
        // Arrange
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-2")).thenReturn(Optional.empty());
        var query = new GetCurricularMaterialsByCourseQuery("teacher-2", course.getId(), Pagination.of(0, 20));

        // Act & Assert
        assertThatThrownBy(() -> service.handle(query)).isInstanceOf(CourseNotOwnedByTeacherException.class);
        verify(curricularMaterialRepository, never()).findAllByCourseId(any(), any());
    }

    @Test
    void shouldReturnMaterialsPendingIngestion() {
        when(curricularMaterialRepository.findAllByStatus(IngestionStatus.PENDING_INGESTION)).thenReturn(List.of());

        assertThat(service.handle(new GetPendingIngestionMaterialsQuery())).isEmpty();
    }
}
