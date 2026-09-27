package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFormat;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurricularMaterialTest {

    private static final SubtopicId SUBTOPIC = new SubtopicId(UUID.randomUUID());

    private static UploadCurricularMaterialCommand command(MaterialFormat format, List<SubtopicId> subtopicIds) {
        return new UploadCurricularMaterialCommand("teacher-1", new CourseId(UUID.randomUUID()), subtopicIds,
                "unit-1.pdf", format, new byte[]{1, 2, 3});
    }

    @Test
    void shouldRegisterMaterialPendingIngestion() {
        // Act
        var material = CurricularMaterial.register(command(MaterialFormat.PDF, List.of(SUBTOPIC)), "stored-ref.pdf");

        // Assert
        assertThat(material.getId()).isNotNull();
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.PENDING_INGESTION);
        assertThat(material.isPendingIngestion()).isTrue();
        assertThat(material.getFile().fileName()).isEqualTo("unit-1.pdf");
        assertThat(material.getFile().storageReference()).isEqualTo("stored-ref.pdf");
        assertThat(material.getFile().sizeBytes()).isEqualTo(3);
        assertThat(material.getContent()).isEmpty();
        assertThat(material.getFailureReason()).isEmpty();
        assertThat(material.isAnchorFor(SUBTOPIC)).isTrue();
    }

    @Test
    void shouldRejectMaterialWithoutFormat() {
        assertThatThrownBy(() -> CurricularMaterial.register(command(null, List.of(SUBTOPIC)), "ref"))
                .isInstanceOf(UnsupportedMaterialFormatException.class);
    }

    @Test
    void shouldRejectMaterialWithoutSubtopics() {
        assertThatThrownBy(() -> CurricularMaterial.register(command(MaterialFormat.PDF, List.of()), "ref"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBecomeReadyWithItsContent() {
        // Arrange
        var material = CurricularMaterial.register(command(MaterialFormat.PNG, List.of(SUBTOPIC)), "ref.png");
        var content = new CurricularContent("Linear equations...", 3);

        // Act
        material.markReady(content);

        // Assert
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.READY);
        assertThat(material.getContent()).contains(content);
        assertThat(material.isPendingIngestion()).isFalse();
    }

    @Test
    void shouldKeepTheReasonWhenIngestionFails() {
        // Arrange
        var material = CurricularMaterial.register(command(MaterialFormat.JPEG, List.of(SUBTOPIC)), "ref.jpeg");

        // Act
        material.markFailed("Unreadable scan");

        // Assert
        assertThat(material.getStatus()).isEqualTo(IngestionStatus.INGESTION_ERROR);
        assertThat(material.getFailureReason()).contains("Unreadable scan");
    }
}
