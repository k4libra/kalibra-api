package com.kalibra.api.curriculum.application.internal.jobs;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.queries.GetPendingIngestionMaterialsQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingIngestionReconciliationJobTest {

    @Mock
    CurricularMaterialQueryService curricularMaterialQueryService;

    @Mock
    CurricularMaterialCommandService curricularMaterialCommandService;

    @InjectMocks
    PendingIngestionReconciliationJob job;

    private CurricularMaterial pending() {
        var command = new UploadCurricularMaterialCommand("teacher-1", new CourseId(UUID.randomUUID()),
                List.of(new SubtopicId(UUID.randomUUID())), "unit.pdf", MaterialFormat.PDF, new byte[]{1});
        return CurricularMaterial.register(command, "ref.pdf");
    }

    @Test
    void shouldRetryEveryPendingMaterialEvenWhenOneFails() {
        // Arrange
        var first = pending();
        var second = pending();
        when(curricularMaterialQueryService.handle(any(GetPendingIngestionMaterialsQuery.class)))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("database hiccup"))
                .when(curricularMaterialCommandService).handle(new IngestCurricularMaterialCommand(first.getId()));

        // Act
        job.run();

        // Assert
        verify(curricularMaterialCommandService).handle(new IngestCurricularMaterialCommand(second.getId()));
    }

    @Test
    void shouldDoNothingWhenNothingIsPending() {
        when(curricularMaterialQueryService.handle(any(GetPendingIngestionMaterialsQuery.class))).thenReturn(List.of());

        job.run();

        verify(curricularMaterialCommandService, never()).handle(any(IngestCurricularMaterialCommand.class));
    }
}
