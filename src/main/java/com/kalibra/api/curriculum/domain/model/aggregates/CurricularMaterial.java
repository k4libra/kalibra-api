package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialFile;
import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CurricularMaterial {

    private MaterialId id;
    private CourseId courseId;
    private List<SubtopicId> subtopicIds;
    private MaterialFile file;
    private IngestionStatus status;
    private Optional<CurricularContent> content;
    private Optional<String> failureReason;
    private Instant uploadedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public CurricularMaterial() {
    }

    private CurricularMaterial(UploadCurricularMaterialCommand command, MaterialFile file) {
        this.id = new MaterialId(UUID.randomUUID());
        this.courseId = command.courseId();
        this.subtopicIds = List.copyOf(command.subtopicIds());
        this.file = file;
        this.status = IngestionStatus.PENDING_INGESTION;
        this.content = Optional.empty();
        this.failureReason = Optional.empty();
        this.uploadedAt = Instant.now();
    }

    public static CurricularMaterial register(UploadCurricularMaterialCommand command, String storageReference) {
        if (command.format() == null) {
            throw new UnsupportedMaterialFormatException(null);
        }
        if (command.subtopicIds() == null || command.subtopicIds().isEmpty()) {
            throw new IllegalArgumentException("Curricular material must be anchored to at least one subtopic");
        }
        var size = command.content() == null ? 0 : command.content().length;
        var file = new MaterialFile(command.fileName(), command.format(), storageReference, size);
        return new CurricularMaterial(command, file);
    }

    public void markReady(CurricularContent content) {
        this.status = IngestionStatus.READY;
        this.content = Optional.of(content);
        this.failureReason = Optional.empty();
    }

    public void markFailed(String reason) {
        this.status = IngestionStatus.INGESTION_ERROR;
        this.content = Optional.empty();
        this.failureReason = Optional.of(reason);
    }

    public boolean isAnchorFor(SubtopicId subtopicId) {
        return subtopicIds.contains(subtopicId);
    }

    public boolean isPendingIngestion() {
        return status == IngestionStatus.PENDING_INGESTION;
    }

    public MaterialId getId() {
        return id;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public List<SubtopicId> getSubtopicIds() {
        return subtopicIds;
    }

    public MaterialFile getFile() {
        return file;
    }

    public IngestionStatus getStatus() {
        return status;
    }

    public Optional<CurricularContent> getContent() {
        return content;
    }

    public Optional<String> getFailureReason() {
        return failureReason;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setId(MaterialId id) {
        this.id = id;
    }

    public void setCourseId(CourseId courseId) {
        this.courseId = courseId;
    }

    public void setSubtopicIds(List<SubtopicId> subtopicIds) {
        this.subtopicIds = subtopicIds;
    }

    public void setFile(MaterialFile file) {
        this.file = file;
    }

    public void setStatus(IngestionStatus status) {
        this.status = status;
    }

    public void setContent(Optional<CurricularContent> content) {
        this.content = content;
    }

    public void setFailureReason(Optional<String> failureReason) {
        this.failureReason = failureReason;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
