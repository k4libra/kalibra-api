package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalCurricularExtractionService;
import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.events.MaterialIngested;
import com.kalibra.api.curriculum.domain.model.events.MaterialIngestionFailed;
import com.kalibra.api.curriculum.domain.model.events.MaterialUploaded;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurricularMaterialCommandServiceImplTest {

    @Mock
    CurricularMaterialRepository curricularMaterialRepository;

    @Mock
    CourseRepository courseRepository;

    @Mock
    MaterialStorageService materialStorageService;

    @Mock
    ExternalCurricularExtractionService externalCurricularExtractionService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CurricularMaterialCommandServiceImpl service;

    private final Course course = Course.create(
            new CreateCourseCommand("teacher-1", "Algebra", new CourseCode("MAT101"), List.of("Equations")));

    private UploadCurricularMaterialCommand upload(SubtopicId subtopicId) {
        return new UploadCurricularMaterialCommand("teacher-1", course.getId(), List.of(subtopicId),
                "unit-1.pdf", MaterialFormat.PDF, new byte[]{1, 2, 3});
    }

    private CurricularMaterial pendingMaterial() {
        return CurricularMaterial.register(upload(course.getSubtopics().getFirst().getId()), "ref.pdf");
    }

    @Test
    void shouldStoreRegisterAndPublishWhenUploadingToAnOwnCourse() {
        // Arrange
        var command = upload(course.getSubtopics().getFirst().getId());
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));
        when(materialStorageService.store("unit-1.pdf", command.content())).thenReturn("ref.pdf");
        when(curricularMaterialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var material = service.handle(command);

        // Assert
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.PENDING_INGESTION);
        assertThat(material.getFile().storageReference()).isEqualTo("ref.pdf");
        verify(eventPublisher).publishEvent(new MaterialUploaded(material.getId().value()));
    }

    @Test
    void shouldRejectUploadToACourseOfAnotherTeacher() {
        // Arrange
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.handle(upload(course.getSubtopics().getFirst().getId())))
                .isInstanceOf(CourseNotOwnedByTeacherException.class);
        verifyNoInteractions(materialStorageService);
    }

    @Test
    void shouldRejectUploadForASubtopicOutsideTheCourse() {
        // Arrange
        when(courseRepository.findByIdAndHolderId(course.getId(), "teacher-1")).thenReturn(Optional.of(course));

        // Act & Assert
        assertThatThrownBy(() -> service.handle(upload(new SubtopicId(UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(materialStorageService);
        verify(curricularMaterialRepository, never()).save(any());
    }

    @Test
    void shouldMarkReadyAndPublishWhenExtractionReturnsContent() {
        // Arrange
        var material = pendingMaterial();
        var content = new CurricularContent("Linear equations", 2);
        when(curricularMaterialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(externalCurricularExtractionService.extract(material)).thenReturn(Optional.of(content));

        // Act
        service.handle(new IngestCurricularMaterialCommand(material.getId()));

        // Assert
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.READY);
        verify(curricularMaterialRepository).save(material);
        verify(eventPublisher).publishEvent(new MaterialIngested(material.getId().value(), material.getCourseId().value()));
    }

    @Test
    void shouldMarkFailedAndPublishWhenExtractionRejectsTheContent() {
        // Arrange
        var material = pendingMaterial();
        when(curricularMaterialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(externalCurricularExtractionService.extract(material)).thenReturn(Optional.empty());

        // Act
        service.handle(new IngestCurricularMaterialCommand(material.getId()));

        // Assert
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.INGESTION_ERROR);
        assertThat(material.getFailureReason()).isPresent();
        verify(eventPublisher).publishEvent(any(MaterialIngestionFailed.class));
    }

    @Test
    void shouldKeepMaterialPendingWhenTheEngineIsUnavailable() {
        // Arrange
        var material = pendingMaterial();
        when(curricularMaterialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(externalCurricularExtractionService.extract(material))
                .thenThrow(new UnsupportedOperationException("Adaptive Engine extraction is not available yet"));

        // Act
        service.handle(new IngestCurricularMaterialCommand(material.getId()));

        // Assert
        assertThat(material.isPendingIngestion()).isTrue();
        verify(curricularMaterialRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldIgnoreMaterialThatIsNoLongerPending() {
        // Arrange
        var material = pendingMaterial();
        material.markReady(new CurricularContent("done", 1));
        when(curricularMaterialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        // Act
        service.handle(new IngestCurricularMaterialCommand(material.getId()));

        // Assert
        verifyNoInteractions(externalCurricularExtractionService);
    }
}
